package miniscala

import scala.collection.mutable.{ Map => MutableMap }

abstract class CPSOptimizer[T <: CPSTreeModule { type Name = Symbol }]
  (val treeModule: T) {
  import treeModule._

  def apply(tree: Tree): Tree = {
    val simplifiedTree = fixedPoint(tree)(shrink)
    val maxSize = (size(simplifiedTree) * 1.5).toInt
    fixedPoint(simplifiedTree, 8) { t => inline(t, maxSize) }
  }

  /* Counts how many times a symbol is encountered as an applied function,
   * and how many as a value
   */
  private case class Count(applied: Int = 0, asValue: Int = 0)

  /* Local state of the optimization
   * Note: To update the state, use the with* methods
   */
  private case class State(
    /* How many times a symbol is encountered in the Tree. Note: The
     * census for the whole program gets calculated once in the
     * beginning, and passed to the initial state.
     */
    census: Map[Name, Count],
    // Name substitution that needs to be applied to the current tree
    subst: Substitution[Name] = Substitution.empty,
    // Names that have a constant value
    lEnv: Map[Name, Literal] = Map.empty,
    // The inverse of lEnv
    lInvEnv: Map[Literal, Name] = Map.empty,
    // A known block mapped to its tag and length
    bEnv: Map[Name, (Literal, Name)] = Map.empty,
    // ((p, args) -> n2) is included in eInvEnv iff n2 == p(args)
    // Note: useful for common-subexpression elimination
    eInvEnv: Map[(ValuePrimitive, Seq[Name]), Name] = Map.empty,
    // Continuations that will be inlined
    cEnv: Map[Name, CntDef] = Map.empty,
    // Functions that will be inlined
    fEnv: Map[Name, FunDef] = Map.empty) {

    // Checks whether a symbol is dead in the current state
    def dead(s: Name): Boolean =
      census get s map (_ == Count(applied = 0, asValue = 0)) getOrElse true
    // Checks whether a symbols is applied exactly once as a function
    // in the current State, and never used as a value
    def appliedOnce(s: Name): Boolean =
      census get s map (_ == Count(applied = 1, asValue = 0)) getOrElse false

    // Adds a substitution to the state
    def withSubst(from: Name, to: Name): State =
      copy(subst = subst + (from -> to))
    // Adds a Seq of substitutions to the state
    def withSubst(from: Seq[Name], to: Seq[Name]): State =
      copy(subst = subst ++ (from zip to))

    // Adds a constant to the State
    def withLit(name: Name, value: Literal) =
      copy(lEnv = lEnv + (name -> value), lInvEnv = lInvEnv + (value -> name))
    // Adds a block to the state
    def withBlock(name: Name, tag: Literal, size: Name) =
      copy(bEnv = bEnv + (name -> (tag, size)))
    // Adds a primitive assignment to the state
    def withExp(name: Name, prim: ValuePrimitive, args: Seq[Name]) =
      copy(eInvEnv = eInvEnv + ((prim, args) -> name))
    // Adds an inlinable continuation to the state
    def withCnt(cnt: CntDef) =
      copy(cEnv = cEnv + (cnt.name -> cnt))
    // Adds a Seq of inlinable continuations to the state
    def withCnts(cnts: Seq[CntDef]) =
      (this /: cnts) (_.withCnt(_))
    // Adds an inlinable function to the state
    def withFun(fun: FunDef) =
      copy(fEnv = fEnv + (fun.name -> fun))
    // Adds a Seq of inlinable functions to the state
    def withFuns(funs: Seq[FunDef]) =
      (this /: funs) (_.withFun(_))
    /*
     * The same state, with empty inverse environments.
     * Use this when entering a new FunDef, because assigned Name's may
     * come out of scope during hoisting.
     */
    def withEmptyInvEnvs =
      copy(lInvEnv = Map.empty, eInvEnv = Map.empty)
  }

  private def argsAreAllKnown(names: Seq[Name], st: State): Boolean = {
    // Returns true if *all* names in the sequence map to known literals in st.lEnv
    names.forall(st.lEnv.contains)
  }

  private def extractKnownVals(names: Seq[Name], st: State): Seq[Literal] = {
    // Gathers the literal values for each name in 'names' from st.lEnv
    names.map(st.lEnv)
  }

  // Shrinking optimizations

  private def shrink(tree: Tree): Tree = {
    def shrinkT(tree: Tree)(implicit s: State): Tree = tree match {
      // LetL - let binding
      case LetL(name, value, body) =>
        // If the symbol is never used, remove the binding
        if (s.dead(name)) {
          shrinkT(body)(s)
          
        } else {
          // If we've seen this literal before, unify with the old name
          s.lInvEnv.get(value) match {
            case Some(existingName) =>
              // existingName already holds the same literal,
              // so just substitute name -> existingName
              val s2 = s.withSubst(name, existingName)
              shrinkT(body.subst(s2.subst))(s2)

            case None =>
              // Otherwise, record (name -> literal) in lEnv/lInvEnv
              val s2 = s.withLit(name, value)
              // Continue shrinking the body with updated state
              LetL(name, value, shrinkT(body)(s2))
          }
        }

      // LetP - primitive assignment
      case LetP(binder, prim, argList, body) =>
        val isImpure = impure(prim)
        val isUnstable = unstable(prim)
        val argCount = argList.size

        // 1) If the binding is never used and the primitive is pure, skip it
        if (s.dead(binder) && !isImpure) {
          shrinkT(body)(s)

        // 2) Common Subexpression Elimination (CSE) when the operation is stable/pure
        } else if (s.eInvEnv.contains((prim, argList)) && !isImpure && !isUnstable) {
          val updState = s.withSubst(binder, s.eInvEnv((prim, argList)))
          shrinkT(body.subst(updState.subst))(updState)

        // 3) Removing the identity primitive
        } else if (prim == identity) {
          val updState = s.withSubst(binder, argList.head)
          shrinkT(body.subst(updState.subst))(updState)

        // 4) Handling block allocation
        } else if (blockAllocTag.isDefinedAt(prim)) {
          val knownTag = blockAllocTag(prim)
          val sizeSym = argList.head
          val updState = s.withBlock(binder, knownTag, sizeSym)
          LetP(binder, prim, argList, shrinkT(body)(updState))

        // 5) If the primitive is "blockTag" and we already know the block's tag
        } else if (prim == blockTag && s.bEnv.contains(argList.head)) {
          val (storedTag, _) = s.bEnv(argList.head)
          shrinkT(LetL(binder, storedTag, body))

        // 6) If the primitive is "blockLength" and we know the block's size
        } else if (prim == blockLength && s.bEnv.contains(argList.head)) {
          val (_, sizeName) = s.bEnv(argList.head)
          val sizeLit = s.lEnv(sizeName) // retrieve the literal from lEnv
          shrinkT(LetL(binder, sizeLit, body))

        // 7) If the primitive is both pure and stable, try constant folding & algebraic rules
        } else if (!isImpure && !isUnstable) {
          // Only some rules make sense with binary primitives
          if (argCount == 2) {
            val lhs = argList.head
            val rhs = argList(1)

            // (a) Attempt constant folding if both arguments are known in lEnv
            if (argsAreAllKnown(argList, s)) {
              val constVals = extractKnownVals(argList, s)
              val foldedLit = vEvaluator((prim, constVals))
              shrinkT(LetL(binder, foldedLit, body))(s)

            // (b) Neutral / absorbing checks on the left argument
            } else if (s.lEnv.contains(lhs)) {
              val leftLit = s.lEnv(lhs)
              if (leftNeutral((leftLit, prim))) {
                val upd = s.withSubst(binder, rhs)
                shrinkT(body.subst(upd.subst))(upd)
              } else if (leftAbsorbing(leftLit, prim)) {
                val upd = s.withSubst(binder, lhs)
                shrinkT(body.subst(upd.subst))(upd)
              } else {
                LetP(binder, prim, argList, shrinkT(body)(s.withExp(binder, prim, argList)))
              }

            // (c) Neutral / absorbing checks on the right argument
            } else if (s.lEnv.contains(rhs)) {
              val rightLit = s.lEnv(rhs)
              if (rightNeutral((prim, rightLit))) {
                val upd = s.withSubst(binder, lhs)
                shrinkT(body.subst(upd.subst))(upd)
              } else if (rightAbsorbing((prim, rightLit))) {
                val upd = s.withSubst(binder, rhs)
                shrinkT(body.subst(upd.subst))(upd)
              } else {
                LetP(binder, prim, argList, shrinkT(body)(s.withExp(binder, prim, argList)))
              }

            // (d) If the two arguments are identical and we have a known reduction
            } else if (lhs == rhs && sameArgReduce.isDefinedAt(prim)) {
              shrinkT(LetL(binder, sameArgReduce(prim), body))

            // (e) Otherwise, record the expression in eInvEnv and move on
            } else {
              LetP(binder, prim, argList, shrinkT(body)(s.withExp(binder, prim, argList)))
            }

          // If there are not exactly two arguments, do a default shrink
          } else {
            LetP(binder, prim, argList, shrinkT(body)(s.withExp(binder, prim, argList)))
          }

        // 8) By default, just rebuild and recurse
        } else {
          LetP(binder, prim, argList, shrinkT(body)(s.withExp(binder, prim, argList)))
        }

      // LetF - function definition
      case LetF(funs, body) =>
        // We'll accumulate the "live" (not dead) functions in a new list
        val liveFuns = collection.mutable.ListBuffer[FunDef]()

        // We'll carry an updated state s2 through the function definitions
        // to keep track of the substitutions and inlinable functions
        var s2 = s

        // Iterate over the function definitions
        for (fun <- funs) {
          if (!s2.dead(fun.name)) {
            // Potential short-circuiting: if the body is just a call to the function
            // and the function is not used as a value, we can inline it
            fun.body match {
              // Optional "tail-call shortcut"
              case AppF(calledFun, retC, args) =>
                // If the function is applied exactly once we might do direct substitution
                // or record for inlining
                if (s2.appliedOnce(fun.name)) {
                  s2 = s2.withFun(fun)
                } else {
                  // If we do want to keep the function, we still need to shrink the body
                  // and add the function to the list of live functions
                  val shrunkFunBody = shrinkT(fun.body.subst(s2.subst))(s2.withEmptyInvEnvs)
                  liveFuns += FunDef(fun.name, fun.retC, fun.args, shrunkFunBody)
                }

              // Default: normal function body
              case _ =>
                // Shrink the function body with a fresh environment
                val shrunkFunBody = shrinkT(fun.body.subst(s2.subst))(s2.withEmptyInvEnvs)
                // Add the function to the list of live functions
                liveFuns += FunDef(fun.name, fun.retC, fun.args, shrunkFunBody)
                // If applied once, record the function for inlining
                if (s2.appliedOnce(fun.name)) {
                  // println(s"Function '${fun.name}' with args ${fun.args.mkString(", ")} marked for inlining")
                  s2 = s2.withFun(fun)
                }
            }
          }
        }

        // Now we can shrink the outer body with the updated state
        val shrunkBody = shrinkT(body.subst(s2.subst))(s2)

        // If no live functions remain, we can just return the shrunk body
        if (liveFuns.isEmpty) {
          shrunkBody
        } else {
          // Otherwise, we need to rebuild the tree with the live functions
          LetF(liveFuns.toSeq, shrunkBody).subst(s2.subst)
        }

      // LetC - continuation definition
      case LetC(cnts, body) =>
        // We'll accumulate the "live" (not dead) continuations in a new list
        val liveCnts = collection.mutable.ListBuffer[CntDef]()

        // Keep track of the updated state s2
        var s2 = s

        // Iterate over the continuations
        for (cnt <- cnts) {
          // If this continuation is actually used in the body keep it, otherwise remove it
          if (!s2.dead(cnt.name)) {
            cnt.body match {
              // If the body is just a call to the continuation, we can inline it
              case AppC(targetCnt, args) =>
                // If the continuation is applied exactly once we can record it for inlining
                if (s2.appliedOnce(cnt.name)) {
                  s2 = s2.withCnt(cnt)
                } else {
                  // If we do want to keep the continuation, we still need to shrink the body
                  // and add the continuation to the list of live continuations
                  val shrunkCntBody = shrinkT(cnt.body.subst(s2.subst))(s2.withEmptyInvEnvs)
                  liveCnts += CntDef(cnt.name, cnt.args, shrunkCntBody)
                }

              // Default: normal continuation body
              case _ =>
                // Shrink the continuation body with a fresh environment
                val shrunkCntBody = shrinkT(cnt.body.subst(s2.subst))(s2.withEmptyInvEnvs)
                // Add the continuation to the list of live continuations
                liveCnts += CntDef(cnt.name, cnt.args, shrunkCntBody)
                // If applied once, record the continuation for inlining
                if (s2.appliedOnce(cnt.name)) {
                  s2 = s2.withCnt(cnt)
                }
            }
          }
        }

        // Now we can shrink the outer body with the updated state
        val shrunkBody = shrinkT(body.subst(s2.subst))(s2)

        // If no live continuations remain, we can just return the shrunk body
        if (liveCnts.isEmpty) {
          shrunkBody
        } else {
          // Otherwise, we need to rebuild the tree with the live continuations
          LetC(liveCnts.toSeq, shrunkBody).subst(s2.subst)
        }
      
      // If - conditional statement
      case If(cond, args, thenC, elseC) =>
        // Apply any name substitution to the arguments & continuations names
        val newArgs = args.map(s.subst)
        val newThenC = s.subst(thenC)
        val newElseC = s.subst(elseC)

        // If there are exactly two arguments and they are the same name we can
        // apply the same argument reduction to see if that condition is always true or false
        if (newArgs.length == 2 && newArgs(0) == newArgs(1) && sameArgReduceC.isDefinedAt(cond)) {
          // If sameArgReduceC(cond) is true -> always go to thenC
          // If sameArgReduceC(cond) is false -> always go to elseC
          if (sameArgReduceC(cond)) {
            // We can just inline the thenC continuation
            AppC(newThenC, Seq.empty)
          } else {
            // We can just inline the elseC continuation
            AppC(newElseC, Seq.empty)
          }
        }

        // Otherwise, see if all arguments are known values to apply the constant folding
        else {
          // Check if all arguments have known literal values in lEnv
          val knownArgs = 
            if (newArgs.forall(a => s.lEnv.contains(a)))
              Some(newArgs.map(a => s.lEnv(a)))
            else None

          knownArgs match {
            case Some(lits) =>
              // If cEvaluator is defined -> we can fold the condition
              cEvaluator.lift((cond, lits)) match {
                // Condition is known at compile time -> pick the right branch
                case Some(true) =>
                  // We can just inline the thenC continuation
                  AppC(newThenC, Seq.empty)
                case Some(false) =>
                  // We can just inline the elseC continuation
                  AppC(newElseC, Seq.empty)
                // No constant folding available, so we need to keep the If statement
                case None =>
                  // Rebuild the tree with the new state
                  If(cond, newArgs, newThenC, newElseC).subst(s.subst)
              }

            case None =>
              // If some arguments are not known, we need to keep the If statement
              // Rebuild the tree with the new state
              If(cond, newArgs, newThenC, newElseC).subst(s.subst)
          }
        }

      // AppC - continuation application
      case AppC(cnt, args) =>
        // See if continuation is in the continuation evironment (single use)
        s.cEnv.get(cnt) match {
          case Some(cntDef) =>
            // This means we want to inline 'cntDef.body' substituting cntDef.args with 'args'
            val s2 = s.withSubst(cntDef.args, args)
            // Then shrink the inlined body with the new updated state
            shrinkT(cntDef.body.subst(s2.subst))(s2)

          case None =>
            // If the continuation is not in the environment (not inlinable), we need to
            // rebuild the tree with the new state and apply the substitution
            val newCnt = s.subst(cnt)
            val newArgs = args.map(s.subst)
            AppC(newCnt, newArgs).subst(s.subst)
        }

      // AppF - function application
      case AppF(fun, retC, args) =>
        // See if function is in the function environment (function is inlinable)
        s.fEnv.get(fun) match {
          case Some(funDef) =>
            // This means we want to inline 'funDef.body' by substituting:
            // - funDef.args -> actual 'args'
            // - funDef.retC -> 'retC'
            val s2 = s.withSubst(funDef.args, args).withSubst(funDef.retC, retC)
            // Then shrink the inlined body with the new updated state
            shrinkT(funDef.body.subst(s2.subst))(s2)

          case None =>
            // If the function is not in the environment (not inlinable), we need to
            // rebuild the tree with the new state and apply the substitution
            val newFun = s.subst(fun)
            val newRetC = s.subst(retC)
            val newArgs = args.map(s.subst)
            AppF(newFun, newRetC, newArgs).subst(s.subst)
        }
      
      case _ => tree
    }

    shrinkT(tree)(State(census(tree)))
  }

  // (Non-shrinking) inlining

  private def inline(tree: Tree, maxSize: Int): Tree = {

    val fibonacci = Seq(1, 2, 3, 5, 8, 13)

    val trees = Stream.iterate((0, tree), fibonacci.length + 1) { case (i, tree) =>
      val funLimit = fibonacci(i)
      val cntLimit = i

      def inlineT(tree: Tree)(implicit s: State): Tree = tree match {
        // LetF - function definition
        case LetF(funs, body) =>
          // Accumulate the new (potentially inlined) function definitions
          var newFuns = Seq.empty[FunDef]
          // We'll update our state as we process the functions
          var newState = s

          // Process each function definition in the LetF block
          for (fun <- funs) {
            if(!newState.dead(fun.name)) {
              // Only consider functions that are used 
              // Measure the overall tree size (could be the outer body size)
              val currentSize = size(body)
              // If the tree is small enough and we haven't already marked this function,
              // record it for inlining in the state
              if (currentSize <= funLimit && !(newState.fEnv contains fun.name)) {
                newState = newState.withFun(fun)
              }
              // Recursively inline the function's body with a fresh inverse environment
              // so that local renamings don't affect the outer body
              val inlinedBody = inlineT(fun.body)(newState.withEmptyInvEnvs)
              // Rebuild the function definition with the inlined body
              newFuns = newFuns :+ FunDef(fun.name, fun.retC, fun.args, inlinedBody)
              // We cannot do newFuns :+= FunDef(fun.name, fun.retC, fun.args, inlinedBody)
              // because that only works on mutable collections
            }
          }

          // Finally we can inline the outer body with the new state and rebuild LetF
          LetF(newFuns, inlineT(body)(newState)).subst(newState.subst)

        // LetC - continuation definition
        case LetC(cnts, body) =>
          // Start with an empty list of new continuation definitions
          var newCnts = Seq.empty[CntDef]
          // We'll update our state as we process the continuations
          var newState = s

          // Process each continuation definition in the LetC block
          for (cnt <- cnts) {
            if (!newState.dead(cnt.name)) {
              // Only consider continuations that are used
              // Measure the overall tree size (could be the outer body size)
              val currentSize = size(body)
              // If the tree is small enough and we haven't already marked this continuation,
              // record it for inlining in the state
              if (currentSize <= cntLimit && !(newState.cEnv contains cnt.name)) {
                newState = newState.withCnt(cnt)
              }
              // Recursively inline the continuation's body with a fresh inverse environment
              // so that local renamings don't affect the outer body
              val inlinedBody = inlineT(cnt.body)(newState.withEmptyInvEnvs)
              // Rebuild the continuation definition with the inlined body
              newCnts = newCnts :+ CntDef(cnt.name, cnt.args, inlinedBody)
            }
          }

          // Finally we can inline the outer body with the new state and rebuild LetC
          // If no continuations remain after filtering, we can just return the inlined body
          if (newCnts.isEmpty) {
            inlineT(body)(newState)
          } else {
            // Otherwise, we need to rebuild the tree with the new continuations
            LetC(newCnts, inlineT(body)(newState)).subst(newState.subst)
          }

        // AppC - continuation application
        case AppC(cnt, args) =>
          s.cEnv.get(cnt) match {
            case Some(cntDef) =>
              // Generate fresh symbols for each formal parameter of the continuation
              // in order to avoid variable capture
              val freshParams: Seq[Name] = cntDef.args.map(a => Symbol.fresh(a.toString))

              // Replace the original parameters with the fresh ones
              val s1 = s.withSubst(cntDef.args, freshParams)

              // Now substitute these fresh symbols with the actual arguments
              val s2 = s1.withSubst(freshParams, args)

              // Inline the body of the continuation with the new state
              // First, shrink the body to propagate changes inside it and then
              // recursively inline the body with the new state
              inlineT(shrink(cntDef.body.subst(s2.subst)))(s2)

            case None =>
              // If the continuation is not inlinable, we need to rebuild the tree
              // with the new state and apply the current substitution
              val newCnt = s.subst(cnt)
              val newArgs = args.map(s.subst)
              AppC(newCnt, newArgs).subst(s.subst)
          }

        // AppF - function application
        case AppF(fun, retC, args) =>
          s.fEnv.get(fun) match {
            case Some(funDef) =>
              // Generate fresh symbols to avoid variable capture
              val freshRetC = Symbol.fresh("retC")
              val freshParams = funDef.args.map(param => Symbol.fresh(param.toString))

              // First, map the function's return continuation and formal parameters to fresh names
              val s1 = s.withSubst(funDef.retC, freshRetC)
              val s2 = (s1 /: (funDef.args zip freshParams)) { (st, pair) => 
                st.withSubst(pair._1, pair._2)
              }
              // Then, map the fresh parameter names to the actual arguments
              val s3 = (s2 /: (freshParams zip args)) { (st, pair) =>
                st.withSubst(pair._1, pair._2)
              }
              // Crucially, map the fresh return continuation to the actual return continuation.
              val s4 = s3.withSubst(freshRetC, retC)

              // // Substitute funDef.retC -> freshRetC and funDef.args -> freshParams in the body
              // val s1 = s.withSubst(funDef.retC, freshRetC).withSubst(funDef.args, freshParams)

              // // Now substitute these fresh symbols with the actual arguments
              // // freshRetC -> retC and freshParams -> args
              // val s2 = s1.withSubst(freshRetC, retC).withSubst(freshParams, args)

              // // Shrink the final body to propagate changes inside it
              // // and then inline the body with the new state
              // shrink(funDef.body.subst(s2.subst))

              // Recursively inline the substituted body with the updated state
              val inlinedBody = inlineT(funDef.body.subst(s4.subst))(s4)

              // Shrink the inlined body to propagate changes inside it
              shrink(inlinedBody)

            case None =>
              // If the function is not inlinable, we need to rebuild the tree
              // with the new state and apply the current substitution
              val newFun = s.subst(fun)
              val newRetC = s.subst(retC)
              val newArgs = args.map(s.subst)
              AppF(newFun, newRetC, newArgs).subst(s.subst)
          }

        // LetL - let binding
        case LetL(name, value, body) =>
        // If the symbol is never used, remove the binding
        if (s.dead(name)) {
          inlineT(body)(s)
        } else {
          s.lInvEnv.get(value) match {
            case Some(existingName) =>
              // unify repeated constants
              val s2 = s.withSubst(name, existingName)
              inlineT(body.subst(s2.subst))(s2)

            case None =>
              val s2 = s.withLit(name, value)
              // inline the body, and then substitute 
              LetL(name, value, inlineT(body)(s2)).subst(s2.subst)
          }
        }

        // LetP - primitive assignment
        case LetP(name, prim, args, body) =>
          // For a pure primitive, if the name is not used at all, remove the binding
          if(!impure(prim) && s.dead(name)) {
            inlineT(body)(s)
          } else {
            // Otherwise, record the primitive assignment in the state and
            // inline the body with the new state
            val s1 = s.withExp(name, prim, args)
            // Rebuild the tree with the new state
            LetP(name, prim, args, inlineT(body)(s1)).subst(s1.subst)
          }

        // For If and Halt statements even though they are not inlinable, we still
        // need to apply the current substitution to guarantees that my inline
        // pass traverses the whole tree
        case If(cond, args, thenC, elseC) =>
          // Apply any name substitution to the arguments & continuations names
          val newArgs = args.map(s.subst)
          val newThenC = s.subst(thenC)
          val newElseC = s.subst(elseC)
          // Rebuild the tree with the new state
          If(cond, newArgs, newThenC, newElseC).subst(s.subst)

        case Halt(arg) =>
          // Apply any name substitution to the argument
          val newArg = s.subst(arg)
          // Rebuild the tree with the new state
          Halt(newArg).subst(s.subst)
        
        case _ => tree
      }

      (i + 1, fixedPoint(inlineT(tree)(State(census(tree))))(shrink))
    }

    trees.takeWhile{ case (_, tree) => size(tree) <= maxSize }.last._2
  }

  // Census computation
  private def census(tree: Tree): Map[Name, Count] = {
    val census = MutableMap[Name, Count]()
    val rhs = MutableMap[Name, Tree]()

    def incAppUse(symbol: Name): Unit = {
      val currCount = census.getOrElse(symbol, Count())
      census(symbol) = currCount.copy(applied = currCount.applied + 1)
      rhs remove symbol foreach addToCensus
    }

    def incValUse(symbol: Name): Unit = {
      val currCount = census.getOrElse(symbol, Count())
      census(symbol) = currCount.copy(asValue = currCount.asValue + 1)
      rhs remove symbol foreach addToCensus
    }

    def addToCensus(tree: Tree): Unit = (tree: @unchecked) match {
      case LetL(_, _, body) =>
        addToCensus(body)
      case LetP(_, _, args, body) =>
        args foreach incValUse; addToCensus(body)
      case LetC(cnts, body) =>
        rhs ++= (cnts map { c => (c.name, c.body) }); addToCensus(body)
      case LetF(funs, body) =>
        rhs ++= (funs map { f => (f.name, f.body) }); addToCensus(body)
      case AppC(cnt, args) =>
        incAppUse(cnt); args foreach incValUse
      case AppF(fun, retC, args) =>
        incAppUse(fun); incValUse(retC); args foreach incValUse
      case If(_, args, thenC, elseC) =>
        args foreach incValUse; incValUse(thenC); incValUse(elseC)
      case Halt(arg) =>
        incValUse(arg)
    }

    addToCensus(tree)
    census.toMap
  }

  private def sameLen(formalArgs: Seq[Name], actualArgs: Seq[Name]): Boolean =
    formalArgs.length == actualArgs.length

  private def size(tree: Tree): Int = (tree: @unchecked) match {
    case LetL(_, _, body) => size(body) + 1
    case LetP(_, _, _, body) => size(body) + 1
    case LetC(cs, body) => (cs map { c => size(c.body) }).sum + size(body)
    case LetF(fs, body) => (fs map { f => size(f.body) }).sum + size(body)
    case AppC(_, _) | AppF(_, _, _) | If(_, _, _, _) | Halt(_) => 1
  }

  // Returns whether a ValuePrimitive has side-effects
  protected val impure: ValuePrimitive => Boolean
  // Returns whether different applications of a ValuePrimivite on the
  // same arguments may yield different results
  protected val unstable: ValuePrimitive => Boolean
  // Extracts the tag from a block allocation primitive
  protected val blockAllocTag: PartialFunction[ValuePrimitive, Literal]
  // Returns true for the block tag primitive
  protected val blockTag: ValuePrimitive
  // Returns true for the block length primitive
  protected val blockLength: ValuePrimitive
  // Returns true for the identity primitive
  protected val identity: ValuePrimitive

  // ValuePrimitives with their left-neutral elements
  protected val leftNeutral: Set[(Literal, ValuePrimitive)]
  // ValuePrimitives with their right-neutral elements
  protected val rightNeutral: Set[(ValuePrimitive, Literal)]
  // ValuePrimitives with their left-absorbing elements
  protected val leftAbsorbing: Set[(Literal, ValuePrimitive)]
  // ValuePrimitives with their right-absorbing elements
  protected val rightAbsorbing: Set[(ValuePrimitive, Literal)]
  // ValuePrimitives with the value equal arguments reduce to
  protected val sameArgReduce: PartialFunction[ValuePrimitive, Literal]
  // TestPrimitives with the (boolean) value equal arguments reduce to
  protected val sameArgReduceC: PartialFunction[TestPrimitive, Boolean]
  // An evaluator for ValuePrimitives
  protected val vEvaluator: PartialFunction[(ValuePrimitive, Seq[Literal]),
                                            Literal]
  // An evaluator for TestPrimitives
  protected val cEvaluator: PartialFunction[(TestPrimitive, Seq[Literal]),
                                            Boolean]
}

object CPSOptimizerHigh extends CPSOptimizer(SymbolicCPSTreeModule)
    with (SymbolicCPSTreeModule.Tree => SymbolicCPSTreeModule.Tree) {
  import treeModule._

  // Primitives that may have side-effects.
  protected val impure: ValuePrimitive => Boolean =
    Set(MiniScalaBlockSet, MiniScalaByteRead, MiniScalaByteWrite)

  // Unstable primitives: different applications on the same arguments
  // may yield different results.
  protected val unstable: ValuePrimitive => Boolean = {
    case MiniScalaBlockAlloc(_) | MiniScalaBlockGet | MiniScalaByteRead => true
    case _ => false
  }

  // Block allocation primitive tag extractor
  protected val blockAllocTag: PartialFunction[ValuePrimitive, Literal] = {
    case MiniScalaBlockAlloc(tag) => IntLit(tag)
  }
  protected val blockTag: ValuePrimitive = MiniScalaBlockTag
  protected val blockLength: ValuePrimitive = MiniScalaBlockLength

  protected val identity: ValuePrimitive = MiniScalaId

  // ValuePrimitives with their left-neutral elements
  protected val leftNeutral: Set[(Literal, ValuePrimitive)] =
    Set(
      (IntLit(0), MiniScalaIntAdd),         // 0 + x = x
      (IntLit(1), MiniScalaIntMul),         // 1 * x = x
      (IntLit(~0), MiniScalaIntBitwiseAnd), // -1 & x = x (all bits set)
      (IntLit(0), MiniScalaIntBitwiseOr),   // 0 | x = x
      (IntLit(0), MiniScalaIntBitwiseXOr)   // 0 ^ x = x
    )
  // ValuePrimitives with their right-neutral elements  
  protected val rightNeutral: Set[(ValuePrimitive, Literal)] =
    Set(
      (MiniScalaIntAdd, IntLit(0)),         // x + 0 = x
      (MiniScalaIntSub, IntLit(0)),         // x - 0 = x
      (MiniScalaIntMul, IntLit(1)),         // x * 1 = x
      (MiniScalaIntDiv, IntLit(1)),         // x / 1 = x
      (MiniScalaIntArithShiftLeft, IntLit(0)), // x << 0 = x
      (MiniScalaIntArithShiftRight, IntLit(0)), // x >> 0 = x
      (MiniScalaIntBitwiseAnd, IntLit(~0)), // x & -1 = x (all bits set)
      (MiniScalaIntBitwiseOr, IntLit(0)),   // x | 0 = x
      (MiniScalaIntBitwiseXOr, IntLit(0))   // x ^ 0 = x
    )

  protected val leftAbsorbing: Set[(Literal, ValuePrimitive)] =
    Set(
      (IntLit(0), MiniScalaIntMul),         // 0 * x = 0
      (IntLit(0), MiniScalaIntBitwiseAnd),  // 0 & x = 0
      (IntLit(~0), MiniScalaIntBitwiseOr)   // -1 | x = -1 (all bits set)
    )
  protected val rightAbsorbing: Set[(ValuePrimitive, Literal)] =
    Set(
      (MiniScalaIntMul, IntLit(0)),          // x * 0 = 0
      (MiniScalaIntBitwiseAnd, IntLit(0)),   // x & 0 = 0
      (MiniScalaIntBitwiseOr, IntLit(~0))    // x | -1 = -1 (all bits set)
    )

  protected val sameArgReduce: PartialFunction[ValuePrimitive, Literal] =
    Map(
      MiniScalaIntSub -> IntLit(0),         // x - x = 0
      MiniScalaIntDiv -> IntLit(1),         // x / x = 1
      MiniScalaIntMod -> IntLit(0),         // x % x = 0
      MiniScalaIntBitwiseXOr -> IntLit(0)   // x ^ x = 0
    )

  protected val sameArgReduceC: PartialFunction[TestPrimitive, Boolean] = {
    case MiniScalaIntLe | MiniScalaIntGe | MiniScalaEq => true
    case MiniScalaIntLt | MiniScalaIntGt | MiniScalaNe => false
    case _ => false
  }

  // Evaluator for ValuePrimitives
  protected val vEvaluator: PartialFunction[(ValuePrimitive, Seq[Literal]),
                                            Literal] = {
    case (MiniScalaIntAdd, Seq(IntLit(x), IntLit(y))) => IntLit(x + y)
    case (MiniScalaIntSub, Seq(IntLit(x), IntLit(y))) => IntLit(x - y)
    case (MiniScalaIntMul, Seq(IntLit(x), IntLit(y))) => IntLit(x * y)
    case (MiniScalaIntDiv, Seq(IntLit(x), IntLit(y))) if (y != 0) =>
      IntLit(Math.floorDiv(x, y))
    case (MiniScalaIntMod, Seq(IntLit(x), IntLit(y))) if (y != 0) =>
      IntLit(Math.floorMod(x, y))

    case (MiniScalaIntArithShiftLeft, Seq(IntLit(x), IntLit(y))) =>
      IntLit(x << y)
    case (MiniScalaIntArithShiftRight, Seq(IntLit(x), IntLit(y))) =>
      IntLit(x >> y)
    case (MiniScalaIntBitwiseAnd, Seq(IntLit(x), IntLit(y))) =>
      IntLit(x & y)
    case (MiniScalaIntBitwiseOr, Seq(IntLit(x), IntLit(y))) =>
      IntLit(x | y)
    case (MiniScalaIntBitwiseXOr, Seq(IntLit(x), IntLit(y))) =>
      IntLit(x ^ y)
    
    case (MiniScalaCharToInt, Seq(CharLit(c))) => IntLit(c.toInt)
    case (MiniScalaIntToChar, Seq(IntLit(i))) => CharLit(i.toChar)
    case (MiniScalaId, Seq(x)) => x
  }

  // Evaluator for TestPrimitives
  protected val cEvaluator: PartialFunction[(TestPrimitive, Seq[Literal]),
                                            Boolean] = {

    case (MiniScalaIntP, Seq(IntLit(_))) => true
    case (MiniScalaIntP, Seq(_)) => false
    case (MiniScalaCharP, Seq(CharLit(_))) => true
    case (MiniScalaCharP, Seq(_)) => false
    case (MiniScalaBoolP, Seq(BooleanLit(_))) => true
    case (MiniScalaBoolP, Seq(_)) => false
    case (MiniScalaUnitP, Seq(UnitLit)) => true
    case (MiniScalaUnitP, Seq(_)) => false

    case (MiniScalaIntLt, Seq(IntLit(x), IntLit(y))) => x < y
    case (MiniScalaIntLe, Seq(IntLit(x), IntLit(y))) => x <= y
    case (MiniScalaEq, Seq(IntLit(x), IntLit(y))) => x == y
    case (MiniScalaNe, Seq(IntLit(x), IntLit(y))) => x != y
    case (MiniScalaIntGe, Seq(IntLit(x), IntLit(y))) => x >= y
    case (MiniScalaIntGt, Seq(IntLit(x), IntLit(y))) => x > y

    case (MiniScalaEq, Seq(BooleanLit(x), BooleanLit(y))) => x == y
    case (MiniScalaNe, Seq(BooleanLit(x), BooleanLit(y))) => x != y
  }
}

object CPSOptimizerLow extends CPSOptimizer(SymbolicCPSTreeModuleLow)
    with (SymbolicCPSTreeModuleLow.Tree => SymbolicCPSTreeModuleLow.Tree) {
  import treeModule._

  protected val impure: ValuePrimitive => Boolean =
    Set(CPSBlockSet, CPSByteRead, CPSByteWrite)

  protected val unstable: ValuePrimitive => Boolean = {
    case CPSBlockAlloc(_) | CPSBlockGet | CPSByteRead => true
    case _ => false
  }

  protected val blockAllocTag: PartialFunction[ValuePrimitive, Literal] = {
    case CPSBlockAlloc(tag) => tag
  }
  protected val blockTag: ValuePrimitive = CPSBlockTag
  protected val blockLength: ValuePrimitive = CPSBlockLength

  protected val identity: ValuePrimitive = CPSId

  protected val leftNeutral: Set[(Literal, ValuePrimitive)] =
    Set((0, CPSAdd), (1, CPSMul), (~0, CPSAnd), (0, CPSOr), (0, CPSXOr))
  protected val rightNeutral: Set[(ValuePrimitive, Literal)] =
    Set((CPSAdd, 0), (CPSSub, 0), (CPSMul, 1), (CPSDiv, 1),
        (CPSArithShiftL, 0), (CPSArithShiftR, 0),
        (CPSAnd, ~0), (CPSOr, 0), (CPSXOr, 0))

  protected val leftAbsorbing: Set[(Literal, ValuePrimitive)] =
    Set((0, CPSMul), (0, CPSAnd), (~0, CPSOr))
  protected val rightAbsorbing: Set[(ValuePrimitive, Literal)] =
    Set((CPSMul, 0), (CPSAnd, 0), (CPSOr, ~0))

  protected val sameArgReduce: Map[ValuePrimitive, Literal] =
    Map(CPSSub -> 0, CPSDiv -> 1, CPSMod -> 0, CPSXOr -> 0)

  protected val sameArgReduceC: PartialFunction[TestPrimitive, Boolean] = {
    case CPSLe | CPSGe | CPSEq => true
    case CPSLt | CPSGt | CPSNe => false
  }

  protected val vEvaluator: PartialFunction[(ValuePrimitive, Seq[Literal]),
                                            Literal] = {
    case (CPSAdd, Seq(x, y)) => x + y
    case (CPSSub, Seq(x, y)) => x - y
    case (CPSMul, Seq(x, y)) => x * y
    case (CPSDiv, Seq(x, y)) if (y != 0) => Math.floorDiv(x, y)
    case (CPSMod, Seq(x, y)) if (y != 0) => Math.floorMod(x, y)

    case (CPSArithShiftL, Seq(x, y)) => x << y
    case (CPSArithShiftR, Seq(x, y)) => x >> y
    case (CPSAnd, Seq(x, y)) => x & y
    case (CPSOr, Seq(x, y)) => x | y
    case (CPSXOr, Seq(x, y)) => x ^ y
  }

  protected val cEvaluator: PartialFunction[(TestPrimitive, Seq[Literal]),
                                            Boolean] = {

    case (CPSLt, Seq(x, y)) => x < y
    case (CPSLe, Seq(x, y)) => x <= y
    case (CPSEq, Seq(x, y)) => x == y
    case (CPSNe, Seq(x, y)) => x != y
    case (CPSGe, Seq(x, y)) => x >= y
    case (CPSGt, Seq(x, y)) => x > y
  }
}
