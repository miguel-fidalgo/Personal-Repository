  package miniscala

  import miniscala.{ SymbolicCMScalaTreeModule => S }
  import miniscala.{ SymbolicCPSTreeModule => C }

  object CMScalaToCPSTranslator extends (S.Tree => C.Tree) {
    def apply(tree: S.Tree): C.Tree = {
      nonTail(tree){_ =>
        val z = Symbol.fresh("c0")
        C.LetL(z, IntLit(0), C.Halt(z))
      }(Set.empty)
    }

    private def nonTail(tree: S.Tree)(ctx: Symbol=>C.Tree)(implicit mut: Set[Symbol]): C.Tree = {
      // @unchecked to avoid bogus compiler warnings
      (tree: @unchecked) match {
        // Assignment to an immutable variable
        // [val n1 = e1; e] C = 
        //    [e1] (λv(val_p n1 = id(v); [e] C))
        case S.Let(name, _, value, body) =>
          nonTail(value) {v =>
              C.LetP(name, MiniScalaId, Seq(v), nonTail(body)(ctx))
          }

        // Reference of an immutable variable
        // [n] C = C[n]
        case S.Ref(name) if !mut(name) =>
          ctx(name) // Plug n into the context

        // Reference of a mutable variable
        // [n] C where n is mutable
        //    val_l z = 0;
        //    val_p v = block-get(n, z); C[v]
        case S.Ref(name) => // if mut(name) =>
          val z = Symbol.fresh("z")
          val v = Symbol.fresh("v")
          C.LetL(z, IntLit(0),
            C.LetP(v, MiniScalaBlockGet, Seq(name, z), ctx(v)))
        
        // Assignment to a mutable variable
        // [var n1 = e1; e] C =
        //    val_l s = 1;
        //    val_p n1 = block-alloc-242(s);
        //    val_l z = 0;
        //    [e1] (λv (val_p d = block-set(n1, z, v); [e] C))
        case S.VarDec(name, _, value, body) =>
          val s = Symbol.fresh("s")
          val z = Symbol.fresh("z")
          C.LetL(s, IntLit(1),
            C.LetP(name, MiniScalaBlockAlloc(0), Seq(s),
              C.LetL(z, IntLit(0),
                nonTail(value) { v =>
                  val d = Symbol.fresh("d")
                  C.LetP(d, MiniScalaBlockSet, Seq(name, z, v),
                    nonTail(body)(ctx))})))

        // Assignment to a mutable variable (already declared)
        // [n1 = e1] C where n1 is mutable
        //    val_l z = 0;
        //    [e1] (λv (val_p d = block-set(n1, z, v); C[v]))
        case S.VarAssign(name, value) =>
          val z = Symbol.fresh("z")
          C.LetL(z, IntLit(0),
            nonTail(value) { v =>
              val d = Symbol.fresh("d")
              C.LetP(d, MiniScalaBlockSet, Seq(name, z, v),
                ctx(v))})

        // Function definition
        // [def f1(n1,1:_, ...) = e1; def ...; e] C =
        //    def_f f1(c, n1,1, ...) = {
        //      [e1] (λv (c(v)))
        //    }; def_f ...;
        //    [e] C
        case S.LetRec(funs, body) =>
          val cpsFuns: Seq[C.FunDef] = funs.map { 
              case S.FunDef(funName, _, argList, _, funBody) =>
                  // Make a fresh symbol for the function’s continuation param:
                  val c = Symbol.fresh("cFun")

                  // Make fresh symbols for each user arg:
                  val argSyms = argList.map(arg => Symbol.fresh(arg.name.toString))

                  // Substitution: from each old arg name -> the fresh argSym
                  val substPairs = argList.map(_.name).zip(argSyms)
                  val renamedBody = substitute(funBody, substPairs.toMap)

                  val bodyCPS = tail(renamedBody, c)
                  C.FunDef(funName, c, argSyms, bodyCPS)
          }
          // Wrap the function definitions in a LetF
          C.LetF(cpsFuns, nonTail(body)(ctx))


        // Function application
        // [e(e1, e2, ...)] C =
        //    [e] (λv ([e1] (λv1 ([e2] (λv2 (...
        //    def_c c(r) = { C[r] };
        //    v(c, v1, v2, ...)))))))
        case S.App(funExpr, _, argExprs) =>
          // Translate 'funExpr' -> symbol 'fun'
          nonTail(funExpr) { fun =>
            // Translate argExprs -> symbols 'args'
            nonTail_*(argExprs) { args =>
              // Create a new continuation c(r) => C[r]
              val c = Symbol.fresh("c")
              val r = Symbol.fresh("r")
              C.LetC(
                // def_c c(r) = { C[r] };
                Seq(C.CntDef(c, Seq(r), ctx(r))),
                // v(c, v1, v2, ...)
                C.AppF(fun, c, args)
              )
            }
          }

          // Literal
          // [l] C = where l is a literal
          //    val_l n = l; C[n]
          case S.Lit(lit) =>
            val n = Symbol.fresh("n")
            C.LetL(n, lit, ctx(n))

          // If-then-else
          // [if (e1) e2 else e3] C =
          //    def_c c(r) = { C[r] };
          //    def_c ct() = { [e2] (λv2 (c(v2))) };
          //    def_c cf() = { [e3] (λv3 (cS(v3))) };
          //    val_l f = false;
          //    [e1] (λv1 (if (v1 != false) ct() else cf()))
          case S.If(condE, tBranch, eBranch)
              if !condE.isInstanceOf[S.Prim] =>
            
            val c = Symbol.fresh("c")
            val r = Symbol.fresh("r")

            // Final continuation
            val cDef = C.CntDef(c, Seq(r), ctx(r))

            // Local continuation ct() => [tBranch] -> c(v2)
            val ct = Symbol.fresh("ct")
            val ctDef = C.CntDef(ct, Seq(),
              nonTail(tBranch) { v2 =>
              C.AppC(c, Seq(v2))
            })
            
            // Local continuation cf() => [eBranch] -> c(v3)
            val cf = Symbol.fresh("cf")
            val cfDef = C.CntDef(cf, Seq(),
              nonTail(eBranch) { v3 =>
                C.AppC(c, Seq(v3))
            })

            // Translate the condition in nonTail form
            C.LetC(Seq(cDef, ctDef, cfDef), cond(condE, ct, cf))

          // If-then-else with primitive condition
          // [if (p(e1, ...)) e2 else e3] C =
          //    def_c c(r) = { C[r] };
          //    def_c ct() = { [e2] (λv2 (c(v2))) };
          //    def_c cf() = { [e3] (λv3 (c(v3))) };
          //    [e1] (λv1 (if p(v1 ...) ct() else cf()))
          case S.If(S.Prim(p: MiniScalaTestPrimitive, cArgs), tBranch, eBranch) =>
            // p is the test primitive (e.g. <, >, ==, !=, etc.)
            // cArgs are the arguments to the test primitive (e.g. v1, v2, etc.)
            val c = Symbol.fresh("c")
            val r = Symbol.fresh("r")
            val cDef = C.CntDef(c, Seq(r), ctx(r))

            // Local continuation ct() => [tBranch] -> c(v2)
            val ct = Symbol.fresh("ct")
            val ctDef = C.CntDef(ct, Seq(),
              nonTail(tBranch) { v2 =>
              C.AppC(c, Seq(v2))
            })
            
            // Local continuation cf() => [eBranch] -> c(v3)
            val cf = Symbol.fresh("cf")
            val cfDef = C.CntDef(cf, Seq(),
              nonTail(eBranch) { v3 =>
                C.AppC(c, Seq(v3))
            })

            // Evaluate all the subexpressions of the test p(e1, e2, ...) in nonTail_*
            C.LetC(Seq(cDef, ctDef, cfDef),
              nonTail_*(cArgs) { args =>
                C.If(p, args, ct, cf)
              })

          // While loop
          // [while (e1) e2; e3] C =
          //    def_c loop() = {
          //       def_c c() = { [e3] C };
          //       def_c ct() = { [e2] (λv (loop())) };
          //       val_l f = false;
          //       [e1] (λv (if (v != false) ct() else c()))
          //    };
          //    loop()
          case S.While(condE, lBody, body) =>
            val loop = Symbol.fresh("loop")
            val c = Symbol.fresh("c")
            val ct = Symbol.fresh("ct")

            // The "false" continuation: if the condition is false, we return to the outer context
            val cDef = C.CntDef(c, Seq(),
              nonTail(body) { v =>
                ctx(v)
              })
            
            // The "true" continuation: if the condition is true, we loop back to the beginning
            val ctDef = C.CntDef(ct, Seq(),
              nonTail(lBody) { v =>
                C.AppC(loop, Seq())
              })

            // The loop continuation itself
            // val loopDef = {
            //   val f = Symbol.fresh("false")
            //   C.CntDef(loop, Seq(),
            //     C.LetL(f, BooleanLit(false),
            //       nonTail(condE) { v =>
            //         C.If(MiniScalaNe, Seq(v, f), ct, c)}))
            // }
            val loopDef = C.CntDef(loop, Seq(), cond(condE, ct, c))

            // Wrap the loop in a LetC
            C.LetC(Seq(loopDef, cDef, ctDef), 
              C.AppC(loop, Seq()))

            // Primitive operation
            // [p(e1, e2, ...)] C =
            // - if p is a test primitive => already covered by the If case
            // - if p is a value primitive => val_p n = p(e1, e2, ...); C[n]
            case S.Prim(p, args) =>
              p match {
                case testP: MiniScalaTestPrimitive =>
                  nonTail_*(args) { args =>
                    val c = Symbol.fresh("c")
                    val r = Symbol.fresh("r")
                    val cDef = C.CntDef(c, Seq(r), ctx(r))

                    // Local continuation ct() => [tBranch] -> c(v2)
                    val ct = Symbol.fresh("ct")
                    val vTrue = Symbol.fresh("vTrue")
                    val ctDef = C.CntDef(ct, Seq(),
                      C.LetL(vTrue, BooleanLit(true),
                        C.AppC(c, Seq(vTrue))))

                    // Local continuation cf() => [eBranch] -> c(v3)
                    val cf = Symbol.fresh("cf")
                    val vFalse = Symbol.fresh("vFalse")
                    val cfDef = C.CntDef(cf, Seq(),
                      C.LetL(vFalse, BooleanLit(false),
                        C.AppC(c, Seq(vFalse))))

                    // Translate the condition in nonTail form
                    C.LetC(Seq(cDef, ctDef, cfDef),
                      C.If(testP, args, ct, cf))
                    }

                  case valP: MiniScalaValuePrimitive =>
                    // Just a normal value operation => val_p n = p(e1, e2, ...); C[n]
                    nonTail_*(args) { args =>
                      val n = Symbol.fresh("n")
                      C.LetP(n, valP, args, ctx(n))
                    }
              }
      }
    }
    
    // nonTail_* takes a sequence of S.Tree, and a continuation that takes a
    // sequence of symbols.  The sequence of symbols in the continuation
    // represents the transformed result of `trees`.  This is particularly useful
    // for the App case in nonTail.
    private def nonTail_*(trees: Seq[S.Tree])(ctx: Seq[Symbol]=>C.Tree)(implicit mut: Set[Symbol]): C.Tree =
      trees match {
        case Seq() => 
          ctx(Seq())
        case t +: ts =>
          nonTail(t)(tSym => nonTail_*(ts)(tSyms => ctx(tSym +: tSyms)))
      }

    private def tail(tree: S.Tree, c: Symbol)(implicit mut: Set[Symbol]): C.Tree = {
      // @unchecked to avoid bogus compiler warnings
      (tree: @unchecked) match {
        // Assignment to an immutable variable
        case S.Let(name, _, value, body) =>
          nonTail(value)(v =>
            C.LetP(name, MiniScalaId, Seq(v), tail(body, c)))

        // Reference of an immutable variable
        // [n]T c = c(n)
        case S.Ref(name) if !mut(name) =>
          C.AppC(c, Seq(name))

        // Reference of a mutable variable
        // [n]T c = where n is a mutable variable
        //    val_l z = 0;
        //    val_p v = block-get(n, z); c(v)
        case S.Ref(name) =>
          val z = Symbol.fresh("z")
          val v = Symbol.fresh("v")
          C.LetL(z, IntLit(0),
            C.LetP(v, MiniScalaBlockGet, Seq(name, z), 
              C.AppC(c, Seq(v)))) 

        // Assignment to a mutable variable
        // [var n1 = e1; e]T c =
        //    val_l s = 1;
        //    val_p n1 = block-alloc-242(s);
        //    val_l z = 0;
        //    [e1] (λv (val_p d = block-set(n1, z, v); eT c))
        case S.VarDec(name, _, value, body) =>
          val s = Symbol.fresh("s")
          val z = Symbol.fresh("z")
          C.LetL(s, IntLit(1),
            C.LetP(name, MiniScalaBlockAlloc(0), Seq(s),
              C.LetL(z, IntLit(0),
                nonTail(value) { v =>
                  val d = Symbol.fresh("d")
                  C.LetP(d, MiniScalaBlockSet, Seq(name, z, v),
                    tail(body, c))})))

          // Assignment to a mutable variable (already declared)
          // [n1 = e1]T c where n1 is mutable
          //    val_l z = 0;
          //    [e1] (λv (val_p d = block-set(n1, z, v); c(v)))
          case S.VarAssign(name, value) =>
            val z = Symbol.fresh("z")
            C.LetL(z, IntLit(0),
              nonTail(value) { v =>
                val d = Symbol.fresh("d")
                C.LetP(d, MiniScalaBlockSet, Seq(name, z, v),
                  C.AppC(c, Seq(v)))})

          // Function definition
          // [def f1(n1,1:_, ...) = e1; def ...; e]T c =
          //    def_f f1(c, n1,1, ...) = {
          //    [e1]T c
          //    }; def_f ...;
          //    eT c
          case S.LetRec(funs, body) =>
          val cpsFuns: Seq[C.FunDef] = funs.map { 
              case S.FunDef(funName, _, argList, _, funBody) =>
                  // Make a fresh symbol for the function’s continuation param:
                  val c = Symbol.fresh("cFun")

                  // Make fresh symbols for each user arg:
                  val argSyms = argList.map(arg => Symbol.fresh(arg.name.toString))

                  // Substitution: from each old arg name -> the fresh argSym
                  val substPairs = argList.map(_.name).zip(argSyms)
                  val renamedBody = substitute(funBody, substPairs.toMap)

                  val bodyCPS = tail(renamedBody, c)
                  C.FunDef(funName, c, argSyms, bodyCPS)
          }
          // Wrap the function definitions in a LetF
          C.LetF(cpsFuns, tail(body, c))

          // Function application
          // [e(e1, e2, ...)]T c =
          //   [e] (λv ([e1] (λv1 ([e2] (λv2 (... (v(c, v, v1, v2, ...))))))))
          case S.App(funExpr, _, argExprs) =>
            nonTail(funExpr) { fun =>
              nonTail_*(argExprs) { args =>
                // We already have the continuation c, so we just need to apply it to the arguments
                C.AppF(fun, c, args)
              }
            }

          // Literal
          // [l]T c = where l is a literal
          //    val_l n = l; c(n)
          case S.Lit(lit) =>
            val n = Symbol.fresh("n")
            C.LetL(n, lit, C.AppC(c, Seq(n)))

          // If-then-else
          // [if (e1) e2 else e3]T c =
          //    def_c ct() = { [e2]T c };
          //    def_c cf() = { [e3]T c };
          //    [e1] (λv (if (v != false) ct() else cf()))
          case S.If(condE, tBranch, eBranch)
              if !condE.isInstanceOf[S.Prim] =>
            
            // Local continuation ct() => [tBranch]T c
            val ct = Symbol.fresh("ct")
            val ctDef = C.CntDef(ct, Seq(),
              tail(tBranch, c))

            // Local continuation cf() => [eBranch]T c
            val cf = Symbol.fresh("cf")
            val cfDef = C.CntDef(cf, Seq(),
              tail(eBranch, c))

            // Translate the condition in nonTail form
            C.LetC(Seq(ctDef, cfDef), cond(condE, ct, cf))

          // If-then-else with primitive condition
          // [if (p(e1, ...)) e2 else e3]T c =
          //    def_c ct() = { [e2]T c };
          //    def_c cf() = { [e3]T c };
          //    [e1] (λv (if p(v ...) ct() else cf()))
          case S.If(S.Prim(p: MiniScalaTestPrimitive, cArgs), tBranch, eBranch) =>
            // p is the test primitive (e.g. <, >, ==, !=, etc.)
            // cArgs are the arguments to the test primitive (e.g. v1, v2, etc.)
            // Local continuation ct() => [tBranch]T c
            val ct = Symbol.fresh("ct")
            val ctDef = C.CntDef(ct, Seq(),
              tail(tBranch, c))

            // Local continuation cf() => [eBranch]T c
            val cf = Symbol.fresh("cf")
            val cfDef = C.CntDef(cf, Seq(),
              tail(eBranch, c))

            // Evaluate all the subexpressions of the test p(e1, e2, ...) in nonTail_*
            C.LetC(Seq(ctDef, cfDef),
              nonTail_*(cArgs) { args =>
                C.If(p, args, ct, cf)
              })

          // While loop
          // [while (e1) e2; e3]T c =
          //    def_c loop() = {
          //       def_c c() = { [e3]T c };
          //       def_c ct() = { [e2]T (λv (loop())) };
          //       val_l f = false;
          //       [e1] (λv (if (v != false) ct() else c()))
          //    };
          //    loop()
          case S.While(condE, lBody, body) =>
            val loop = Symbol.fresh("loop")
            val c = Symbol.fresh("c")
            val ct = Symbol.fresh("ct")

            // The "false" continuation: if the condition is false, we return to the outer context
            val cDef = C.CntDef(c, Seq(),
              tail(body, c))
            
            // The "true" continuation: if the condition is true, we loop back to the beginning
            val ctDef = C.CntDef(ct, Seq(),
              nonTail(lBody) { _ =>
                C.AppC(loop, Seq())
              })

            // The loop continuation itself
            // val loopDef = {
            //   val f = Symbol.fresh("false")
            //   C.CntDef(loop, Seq(),
            //     C.LetL(f, BooleanLit(false),
            //       nonTail(condE) { v =>
            //         C.If(MiniScalaNe, Seq(v, f), ct, c)}))
            // }
            val loopDef = C.CntDef(loop, Seq(), cond(condE, ct, c))

            // Wrap the loop in a LetC
            C.LetC(Seq(loopDef, cDef, ctDef), 
              C.AppC(loop, Seq()))

          // Primitive operation
          // [p(e1, e2, ...)]T c =
          // - if p is a test primitive => already covered by the If case
          // - if p is a value primitive => val_p n = p(e1, e2, ...); c(n)
          case S.Prim(p, args) =>
            p match {
              case testP: MiniScalaTestPrimitive =>
                nonTail_*(args) { args =>
                  // Local continuation ct() => [tBranch] -> c(v2)
                  val ct = Symbol.fresh("ct")
                  val vTrue = Symbol.fresh("vTrue")
                  val ctDef = C.CntDef(ct, Seq(),
                    C.LetL(vTrue, BooleanLit(true),
                      C.AppC(c, Seq(vTrue))))

                  // Local continuation cf() => [eBranch] -> c(v3)
                  val cf = Symbol.fresh("cf")
                  val vFalse = Symbol.fresh("vFalse")
                  val cfDef = C.CntDef(cf, Seq(),
                    C.LetL(vFalse, BooleanLit(false),
                      C.AppC(c, Seq(vFalse))))

                  // Translate the condition in nonTail form
                  C.LetC(Seq(ctDef, cfDef),
                    C.If(testP, args, ct, cf))
                }

              case valP: MiniScalaValuePrimitive =>
                // Just a normal value operation => val_p n = p(e1, e2, ...); c(n)
                nonTail_*(args) { args =>
                  val n = Symbol.fresh("n")
                  C.LetP(n, valP, args, 
                    C.AppC(c, Seq(n)))
                }
            }
      }
    }

    def substitute(tree: S.Tree, env: Map[Symbol, Symbol]): S.Tree = tree match {

      // ------------------
      //  LITERALS & REFS
      // ------------------

      case S.Lit(lit) =>
          // A literal has no references to rename.
          tree

      case S.Ref(x) =>
          // If x is in the env, rename it; otherwise leave it alone.
          env.get(x) match {
          case Some(xNew) => S.Ref(xNew).withPos(tree.pos).withType(tree.tp)
          case None       => tree
          }

      // ------------------
      //    LET BINDINGS
      // ------------------

      case S.Let(x, xtp, rhs, body) =>
          // 1) Substitute inside rhs
          val rhsSub = substitute(rhs, env)
          // 2) If x itself should be renamed, do it
          val xNew = env.getOrElse(x, x)
          // 3) Substitute inside body. 
          //    If xNew is a brand-new symbol, it won't collide, so we can keep same env.
          //    But be careful about shadowing in a more advanced scenario.
          val bodySub = substitute(body, env)
          S.Let(xNew, xtp, rhsSub, bodySub).withPos(tree.pos).withType(tree.tp)

      // ------------------
      //  MUTABLE VARS
      // ------------------

      case S.VarDec(x, xtp, rhs, body) =>
          val rhsSub  = substitute(rhs, env)
          val xNew    = env.getOrElse(x, x)
          val bodySub = substitute(body, env)
          S.VarDec(xNew, xtp, rhsSub, bodySub).withPos(tree.pos).withType(tree.tp)

      case S.VarAssign(x, rhs) =>
          val xNew   = env.getOrElse(x, x)
          val rhsSub = substitute(rhs, env)
          S.VarAssign(xNew, rhsSub).withPos(tree.pos).withType(tree.tp)

      case S.Prim(p, args) =>
          val argsSub = args.map(e => substitute(e, env))
          S.Prim(p, argsSub).withPos(tree.pos).withType(tree.tp)

      // ------------------
      //   BRANCHES
      // ------------------

      case S.If(cond, thenE, elseE) =>
          val condSub  = substitute(cond, env)
          val thenSub  = substitute(thenE, env)
          val elseSub  = substitute(elseE, env)
          S.If(condSub, thenSub, elseSub).withPos(tree.pos).withType(tree.tp)

      case S.While(cond, loopBody, body) =>
          val condSub = substitute(cond, env)
          val lbSub   = substitute(loopBody, env)
          val bdSub   = substitute(body, env)
          S.While(condSub, lbSub, bdSub).withPos(tree.pos).withType(tree.tp)

      // ------------------
      //  FUNCTION DEFINITIONS
      // ------------------

      case S.LetRec(funs, body) =>
          // For each FunDef, rename inside the body of that function.
          // If you plan to rename the function symbols themselves or the parameters,
          // you can do that here. For a minimal version:
          val newFuns = funs.map {
          case S.FunDef(fname, ptps, args, rtp, fBody) =>
              // (If you want to rename the function name itself, check env.get(fname).)
              val fnameNew = env.getOrElse(fname, fname)
              
              // Potentially rename each Arg(...) name as well:
              val newArgs = args.map { arg =>
              val oldSym = arg.name
              val newSym = env.getOrElse(oldSym, oldSym)
              S.Arg(newSym, arg.tp, arg.pos)
              }

              // Recursively substitute in the function body:
              val fBodySub = substitute(fBody, env)
              
              val newFunDef = S.FunDef(fnameNew, ptps, newArgs, rtp, fBodySub)
              newFunDef.pos = tree.pos
              newFunDef.tp  = tree.tp
              newFunDef
          }
          // Then rename inside the 'body' expression
          val bodySub = substitute(body, env)
          S.LetRec(newFuns, bodySub).withPos(tree.pos).withType(tree.tp)

      // ------------------
      //   FUNCTION APPS
      // ------------------

      case S.App(fun, ptps, args) =>
          val funSub  = substitute(fun, env)
          val argsSub = args.map(a => substitute(a, env))
          S.App(funSub, ptps, argsSub).withPos(tree.pos).withType(tree.tp)

      // ------------------
      //   PAIRS, ALLOC, SELECT, HALT
      //   (Add any other nodes in your AST)
      // ------------------

      case S.PairDec(e1, e2) =>
          val e1Sub = substitute(e1, env)
          val e2Sub = substitute(e2, env)
          S.PairDec(e1Sub, e2Sub).withPos(tree.pos).withType(tree.tp)

      case S.PrimAlloc(tps, es) =>
          val esSub = es.map(e => substitute(e, env))
          S.PrimAlloc(tps, esSub).withPos(tree.pos).withType(tree.tp)

      case S.Select(obj, field) =>
          val objSub = substitute(obj, env)
          S.Select(objSub, field).withPos(tree.pos).withType(tree.tp)

      case S.Halt(exitVal) =>
          val exitValSub = substitute(exitVal, env)
          S.Halt(exitValSub).withPos(tree.pos).withType(tree.tp)
      }


    private def cond(tree: S.Tree, trueC: Symbol, falseC: Symbol)(implicit mut: Set[Symbol]): C.Tree = {
      def litToCont(l: CMScalaLiteral): Symbol =
        if (l != BooleanLit(false)) trueC else falseC

      tree match {
        case S.If(condE, S.Lit(tl), S.Lit(fl)) =>
          cond(condE, litToCont(tl), litToCont(fl))

        // [if (e1) e2 else e3]_N C =
        //    def_c c(r) = { C[r] };
        //    def_c ct() = { [e2]_T c };
        //    def_c cf() = { [e3]_T c };
        //    [e1]_C ct cf;
        case S.If(cond, tBranch, eBranch) 
            if !cond.isInstanceOf[S.Prim] =>

          // Local continuation ct() => [tBranch] -> c(v2)
          val ct = Symbol.fresh("ct")
          val ctDef = C.CntDef(ct, Seq(), tail(tBranch, trueC))

          // Local continuation cf() => [eBranch] -> c(v3)
          val cf = Symbol.fresh("cf")
          val cfDef = C.CntDef(cf, Seq(), tail(eBranch, falseC))

          // Translate the condition in nonTail form
          nonTail(cond) { v =>
            val f = Symbol.fresh("false")
            C.LetC(Seq(ctDef, cfDef),
              C.If(MiniScalaNe, Seq(v, f), ct, cf))}

        // [while (e1) e2; e3]_N C =
        //    def_c loop(d) = {
        //       def_c c() = { [e3]_T c };
        //       def_c ct() = { [e2]_T loop };
        //       [e1]_C ct c
        //    };
        //    val_l d = ();
        //    loop(d)
        case S.While(condE, loopBody, body) =>
          val loop = Symbol.fresh("loop")
          val d    = Symbol.fresh("d")
          val c    = Symbol.fresh("cFalse")
          val ct   = Symbol.fresh("cTrue")

          // Continuation for “false”: do [e3]_T and then go to the outer context
          val cDef = C.CntDef(c, Seq(), tail(body, falseC))
          
          // Continuation for “true”: do [e2]_T and then jump back to loop
          val ctDef = C.CntDef(ct, Seq(), tail(loopBody, loop))

          // The loop(d) continuation: test e1, branching to ct or c
          val loopDef = C.CntDef(loop, Seq(d), cond(condE, ct, c))

          // Finally bind d = () and call loop(d)
          C.LetL(d, UnitLit,
            C.LetC(Seq(loopDef, cDef, ctDef),
              C.AppC(loop, Seq(d)))
          )

        case S.Prim(p: MiniScalaTestPrimitive, args) =>
          nonTail_*(args)(as => C.If(p, as, trueC, falseC))

        case other =>
          nonTail(other)(o =>
            nonTail(S.Lit(BooleanLit(false)))(n =>
              C.If(MiniScalaNe, Seq(o, n), trueC, falseC)))
      }
    }

    // Helper function for defining a continuation.
    // Example:
    // tempLetC("c", Seq(r), ctx(r))(k => App(f, k, as))
    private def tempLetC(cName: String, args: Seq[C.Name], cBody: C.Tree)
                        (body: C.Name=>C.Tree): C.Tree = {
      val cSym = Symbol.fresh(cName)
      C.LetC(Seq(C.CntDef(cSym, args, cBody)), body(cSym))
    }
  }
