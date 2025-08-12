package project3

class SemanticAnalyzer(parser: Parser) extends Reporter with BugReporter {
  import Language._

  /*
   * Primitive functions that do not need to be defined or declared.
   */
  val primitives = Map[String,(Boolean,Type)](
      "getchar" -> (false, FunType(List(), IntType)),
      "putchar" -> (false, FunType(List(("", IntType)), UnitType))
    )

  /*
   * Define an empty state for the Semantic Analyzer.
   *
   * NOTE:
   *   val env = new Env
   *
   *   env("hello") is equivalent to env.apply("hello")
   */
  class Env {
    def apply(name: String): Option[Type] = None
    def isVar(name: String) = false
  }

  /*
   * Env that keeps track of variables defined.
   * The map stores true if the variable is mutable,
   * false otherwise and its type.
   */
  case class TypeEnv(
    vars: Map[String,(Boolean, Type)] = primitives,
    outer: Env = new Env) extends Env {

    /*
     * Return true if the variable is already defined
     * in this scope
     */
    def isDefined(name: String) = vars.contains(name)

    /*
     * Make a copy of this object and add a mutable variable 'name'
     */
    def withVar(name: String, tp: Type): TypeEnv = {
      copy(vars = vars + (name -> (true, tp)))
    }

    /*
     * Make a copy of this object and add an immutable variable 'name'
     */
    def withVal(name: String, tp: Type): TypeEnv = {
      copy(vars = vars + (name -> (false, tp)))
    }

    /*
     * Make a copy of this object and add in the list of immutable variables.
     */
    def withVals(list: List[(String,Type)]): TypeEnv = {
      copy(vars = vars ++ (list map { t => (t._1, (false, t._2)) }).toMap)
    }

    /*
     * Return true if 'name' is a mutable variable defined in this scope
     * or in the outer scope.
     */
    override def isVar(name: String) = vars.get(name) match {
      case None => outer.isVar(name)
      case Some((mut, _)) => mut
    }

    /*
     * Return the Type if the variable 'name' is an option.
     * i.e. Some(tp) if the variable exists or None if it doesn't
     */
    override def apply(name: String): Option[Type] = vars.get(name) match {
      case Some((_, tp)) => Some(tp)
      case None => outer(name)
    }
  }

  // Error reporting
  var numError = 0
  def error(msg: String, pos: Position): Unit = {
    numError += 1
    parser.error(msg, pos)
  }

  // Warning reporting
  var numWarning = 0
  def warn(msg: String, pos: Position): Unit = {
    numWarning += 1
    parser.warn(msg, pos)
  }

  /*
   * Return a fresh name if a new variable needs to be defined
   */
  var next = 0
  def freshName(pref: String = "x") = {
    next += 1
    s"${pref}_$next"
  }

  /*
   * Auxiliary functions. May be useful.
   */
  def getName(arg: Any): String = arg match {
    case Arg(name, _, _) => name
    case FunDef(name, _, _, _) =>  name
    case _ => BUG(s"Don't know how to extract name from $arg")
  }

  def getPos(arg: Any): Position = arg match {
    case Arg(_, _, pos) => pos
    case fd@FunDef(_, _, _, _) => fd.pos
    case _ => BUG(s"Don't know how to extract position from $arg")
  }

  def checkDuplicateNames(args: List[Any]): Boolean = args match {
    case h::t =>
      val name = getName(h)
      val (dup, other) = t partition { arg => name == getName(arg) }
      dup foreach { arg =>
        error(s"$name is already defined", getPos(arg))
      }
      checkDuplicateNames(other) || dup.length > 0
    case Nil => false
  }

  def funType(args: List[Arg], rtp: Type): FunType = {
    FunType(args map { arg => (arg.name, arg.tp) }, rtp)
  }

  def listArgType(size: Int, tp: Type) = List.fill(size)(("", tp))

  /**
   * Run the Semantic Analyzer on the given AST.
   *
   * Print out the number of warnings and errors found, if any.
   * Return the AST with types resolved and the number of warnings
   * and errors.
   *
   * NOTE: we want our main program to return an Int!
   */
  def run(exp: Exp) = {
    numError = 0
    val nexp = typeCheck(exp, IntType)(TypeEnv())
    if (numWarning > 0)
      System.err.println(s"""$numWarning warning${if (numWarning != 1) "s" else ""} found""")
    if (numError > 0)
      System.err.println(s"""$numError error${if (numError != 1) "s" else ""} found""")

    (nexp, numWarning, numError)
  }

  // List of valid infix operators
  val isBOperator   = Set("==","!=","<=",">=","<",">")
  val isIntOperator   = Set("+","-","*","/")

  /*
   * Returns the type of the binary operator 'op'. See case "+" for an example
   */
  def typeBinOperator(op: String)(pos: Position) = op match {
    // Arithmetic: (Int, Int) => Int
    case "+" | "-" | "*" | "/" => 
      FunType(List(("", IntType), ("", IntType)), IntType)

    // Comparison: (Int, Int) => Boolean
    case "==" | "!=" | "<=" | ">=" | "<" | ">" => 
      FunType(List(("", IntType), ("", IntType)), BooleanType)

    // If the operator is not defined, throw an error message and return UnknownType
    case _ =>
      error("undefined binary operator", pos)
      UnknownType
  }

  // List of valid unary operators
  val isIntUnOperator   = Set("+","-")

  /*
   * Returns the type of the unary operator 'op'
   */
  def typeUnOperator(op: String)(pos: Position) = op match {
    // Unary operators: (Int) => Int
    case "+" | "-" => 
      FunType(List(("", IntType)), IntType)

    // If the operator is not defined, throw an error message and return UnknownType
    case _ =>
      error(s"undefined unary operator", pos)
      UnknownType
  }

  /*
   * Returns the type of the ternary operator 'op'
   * operators: block-set
   */
  def typeTerOperator(op: String)(pos: Position) = op match {
    // Ternary operators: (Array, Int, Int) => Unit
    case "block-set" =>
      FunType(List(("", ArrayType(UnknownType)), ("", IntType), ("", IntType)), UnitType)

    // If the operator is not defined, throw an error message and return UnknownType
    case _ =>
      error(s"undefined ternary operator", pos)
      UnknownType
  }
  /*
   * Return the type of the operator 'op' with arity 'arity'
   */
  def typeOperator(op: String, arity: Int)(pos: Position): Type = arity match {
    case 3 => typeTerOperator(op)(pos)
    case 2 => typeBinOperator(op)(pos)
    case 1 => typeUnOperator(op)(pos)
    case _ =>
      error(s"undefined operator", pos)
      UnknownType
  }

  /*
   * Check if 'tp' conforms to 'pt' and return the more precise type.
   * The result needs to be well formed.
   */
  def typeConforms(tp: Type, pt: Type)(env: TypeEnv, pos: Position): Type = (tp, pt) match {
    case (_, _) if tp == pt => typeWellFormed(tp)(env, pos)
    case (_, UnknownType) => typeWellFormed(tp)(env, pos)  // tp <: Any
    case (UnknownType, _) => typeWellFormed(pt)(env, pos)  // for function arguments
    case (FunType(args1, rtp1), FunType(args2, rtp2)) if args1.length == args2.length => 
      // If both functions have the same number of arguments, we need to check
      // we want to unify or conform the types of the arguments and the return type.
      // Internally, this checks that each pair of (tp1, tp2) conforms.
      val nargs = typeConform(args1, args2)(env, pos)
      // We then unify the return types of the functions.
      val nrtp = typeConforms(rtp1, rtp2)(env, pos)
      FunType(nargs, nrtp)
    case (ArrayType(tp), ArrayType(pt)) => ArrayType(typeConforms(tp, pt)(env, pos))
    case _ => error(s"type mismatch;\nfound   : $tp\nexpected: $pt", pos); pt
  }

  /*
   * Auxiliary function used to check function type argument conformity.
   *
   * The function is verifying that 'tp' elements number n conforms
   * to 'pt' element number n. It returns the list of precise types
   * returned by each invocation to typeConforms
   */
  def typeConform(tp: List[(String, Type)], pt: List[(String,Type)])(env: TypeEnv, pos: Position): List[(String, Type)] = {
    if (tp.length != pt.length) BUG("length of list does not match")

    (tp zip pt) map { case ((arg1, tp1), (arg2, tp2)) =>
      (if (tp1 != UnknownType) arg1 
       else arg2, typeConforms(tp1, tp2)(env, pos))
    }
  }

  /*
   * Verify that the type 'tp' is well formed. i.e there is no
   * UnknownType.
   */
  def typeWellFormed(tp: Type)(env: TypeEnv, pos: Position)(implicit forFunction: Boolean=false): Type = tp match {
    case FunType(args, rte) =>
      FunType(args map { case (n, tp) => 
        (n, typeWellFormed(tp)(env, pos)) 
      }, typeWellFormed(rte)(env, pos)(true))
    case ArrayType(tp) => ArrayType(typeWellFormed(tp)(env, pos))
    case UnknownType =>
        if (forFunction) error("malformed type: function return types must be explicit if function is used recursively or in other functions' bodies", pos) 
        else error("malformed type", pos)
        UnknownType
    case _ => tp
  }


  /*
   * typeCheck takes an expression and an expected type (which may be UnknownType).
   * This is done via calling the typeInfer and typeConforms
   * functions (details below), and finally returning the original
   * expression with all typing information resolved.
   *
   * typeInfer uses the inference rules seen during the lectures
   * to discover the type of an expression. As a reminder, the rules we saw can be
   * found in lectures 5 and 6.
   *
   * The code must follow the inference rules seen during the lectures.
   *
   * The errors/warnings check that you had to implement for project 2
   * should be already implemented. However, there are new variables
   * introduced that need to be check for duplicate (function name,
   * variables names). We defined the rules for function semantic in
   * lecture 5.
   */
  def typeCheck(exp: Exp, pt: Type)(env: TypeEnv): Exp = {
    val nexp = typeInfer(exp, pt)(env)
    val rnexpType = typeConforms(nexp.tp, pt)(env, exp.pos)
    nexp.withType(rnexpType)
  }

  def typeInfer(exp: Exp, pt: Type)(env: TypeEnv): Exp = exp match {
    case Lit(_: Int) => exp.withType(IntType)
    case Lit(_: Boolean) => exp.withType(BooleanType)
    case Lit(_: Unit) => exp.withType(UnitType)
    case Prim("block-set", args) => 
      if (args.length != 3) {
        error("block-set must have exactly 3 arguments", exp.pos)
        exp.withType(UnknownType)
      } else {
        // arg0: array, arg1: index, arg2: newValue
        val arrExp = typeCheck(args(0), ArrayType(UnknownType))(env)
        // val idxExp = typeCheck(args(1), IntType)(env)
        // val valExp = typeCheck(args(2), UnknownType)(env)
        arrExp.tp match {
          case ArrayType(elemTp) =>
            // Type-check the index argument against IntType
            val idxExp = typeCheck(args(1), IntType)(env)
            // Type-check the value argument against the element type of the array
            val valExp = typeCheck(args(2), elemTp)(env)
            // Since this operation is for assgignment, the type of the expression is UnitType
            Prim("block-set", List(arrExp, idxExp, valExp)).withType(UnitType)

          case _ =>
            error("block-set requires an array as the first argument", exp.pos)
            exp.withType(UnknownType)
        }
      }
    case Prim(op, args) =>
      typeOperator(op, args.length)(exp.pos) match {
        case FunType(atps, rtp) =>
          // Ensure that the number of arguments is correct
          if (args.length != atps.length) {
            error(s"wrong number of arguments for $op", exp.pos)
            exp.withType(rtp)
          } else {
            // Type-check each argument against its expected type
            val nargs = (args zip atps).map { case (arg, (_, expectedTp)) =>
              typeCheck(arg, expectedTp)(env)
            }
            // Return the new primitive expression with the refined types
            Prim(op, nargs).withType(rtp)
          }
        case UnknownType => exp.withType(UnknownType)
        case _ => BUG("operator's type needs to be FunType")
      }
    case Let(x, tp, rhs, body) =>
      if (env.isDefined(x))
        warn("reuse of variable name", exp.pos)
      val nrhs = typeCheck(rhs, tp)(env)
      val nbody = typeCheck(body, pt)(env.withVal(x, nrhs.tp))
      Let(x, nrhs.tp, nrhs, nbody).withType(nbody.tp)
    case Ref(x) =>
      env(x) match {
        case Some(tp) =>
          // Check that the type from the environment is well formed
          val ntp = typeWellFormed(tp)(env, exp.pos)
          Ref(x).withType(ntp)
        case _ =>
          error("undefined identifier", exp.pos)
          Ref(x).withType(UnknownType)
      }
    case If(cond, tBranch, eBranch) =>
      // Hint: type check the else branch before the then branch.
      val nelse = typeCheck(eBranch, pt)(env)
      // Then use the type of the else branch as the expected type for the then branch
      val nthen = typeCheck(tBranch, nelse.tp)(env)
      // Type-check the condition expecting a Boolean
      val ncond = typeCheck(cond, BooleanType)(env)
      // By the inference rule, both branches have the same type so the if expression has the same type
      If(ncond, nthen, nelse).withType(nthen.tp)
    case VarDec(x, tp, rhs, body) =>
      if (env.isDefined(x))
        warn("reuse of variable name", exp.pos)
      // Type-check the right-hand side (e1) of the variable declaration (T1)
      val nrhs = typeCheck(rhs, tp)(env)
      // Extend the environment with x: T1 and type-check the body (e2) of the variable declaration (T2)
      val nbody = typeCheck(body, pt)(env.withVar(x, nrhs.tp))
      // The entire expression gets the type of the body (pt)
      VarDec(x, nrhs.tp, nrhs, nbody).withType(nbody.tp)
    case VarAssign(x, rhs) =>
      // Look up the type of the variable x in the environment
      val xtp = if (!env.isDefined(x)) {
        error("undefined identifier", exp.pos)
        UnknownType
      } else {
        if (!env.isVar(x))
          error("reassignment to val", exp.pos)
        env(x).get
      }

      // Type-check of the right-hand side (e1) of the variable assignment (T1)
      // with the expected type of the variable xtp
      val nrhs = typeCheck(rhs, xtp)(env)

      // Build the assignemnt expression with type xtp (same as nrhs.tp)
      val nexp = VarAssign(x, nrhs).withType(xtp)

      /* Because of syntactic sugar, a variable assignment 
       * statement can be accepted as an expression
       * of type Unit. In this case, we will modify
       * the AST and store the assignment value into
       * a "dummy" variable and return the Unit Literal.
       *
       * For example,
       *
       * If(..., VarAssign("x", Lit(1)), Lit(()))
       *
       * requires the two branches of the If to be of the same
       * type, in this case, Unit. Therefore the "then" branch
       * will need to be modified to have the correct type.
       * Without changing the semantics!
       */
      pt match {
        case UnitType =>
          // Create a dummy variable name
          val dummy = freshName("dummy")
          // Construct a Let expression to store the assignment expression and return UnitType
          Let(dummy, xtp, nexp, Lit(()).withType(UnitType))
        case _ =>
          // Otherwise return the VarAssign expression
          nexp
      }
    case While(cond, lbody, body) =>
      // Type-check the final expression (e2) with the expected type pt
      val nbody = typeCheck(body, pt)(env)
      // Type-check the loop body (e1) with the expected type Unit
      val nlbody = typeCheck(lbody, UnitType)(env)
      // Type-check the condition (c1) with the expected type Boolean
      val ncond = typeCheck(cond, BooleanType)(env)
      // The overall while expression gets the type of the final expression (T2)
      While(ncond, nlbody, nbody).withType(nbody.tp)
    case FunDef(fname, args, rtp, fbody) =>
      // Check for duplicate argument names
      if (checkDuplicateNames(args)) {
        error(s"Duplicate argument names in function $fname", fbody.pos)
      }
      // Build the declared function type: (T1, T2, ..., Tn) => rtp
      val declaredFunType = funType(args, rtp)
      // Check that there is no previous declaration of the same function name
      if (env.isDefined(fname)) {
        error(s"Function $fname is already defined (no overloading allowed)", fbody.pos)
      }
      // Extend the environment with the function name and type for recursive calls
      val envWithFun = env.withVal(fname, declaredFunType)
      // Extend the enrironment with the function arguments (as immutable variables)
      val envWithArgs = envWithFun.withVals(args.map(arg => (arg.name, arg.tp)))
      // Type-check the function body with the expected return type rtp
      val nfbody = typeCheck(fbody, rtp)(envWithArgs)
      // Ensure that the infered function body type conforms to the declared function type
      val nfbodyType = typeConforms(nfbody.tp, rtp)(envWithArgs, fbody.pos)
      // Construct and return the function definition with the type of the function
      FunDef(fname, args, nfbodyType, nfbody).withType(funType(args, nfbodyType))
    case LetRec(funs, body) =>
      // Build a list of function name, declared function type pairs
      val funTypes: List[(String, Type)] = funs.map {
        case FunDef(fname, args, rtp, _) =>
          // Construct the declared function type: (T1, T2, ..., Tn) => rtp
          (fname, funType(args, rtp))
        case _ => BUG("LetRec expects only function definitions")
      }
      // Extend the environment with the function names and types for recursive calls
      val envWithFuns = env.withVals(funTypes)
      // Type-check each function definition in the extended environment
      val nfuns = funs.map {
        case f @ FunDef(fname, args, rtp, fbody) =>
          // Extend the environment with the function parameters (as immutable variables)
          val envWithArgs = envWithFuns.withVals(args.map(arg => (arg.name, arg.tp)))
          // Type-check the function body with the expected return type rtp
          val nfbody = typeCheck(fbody, rtp)(envWithArgs)
          // Ensure that the infered function body type conforms to the declared function type
          val nfbodyType = typeConforms(nfbody.tp, rtp)(envWithArgs, fbody.pos)
          // Construct the function definition with the type of the function
          FunDef(fname, args, nfbodyType, nfbody).withType(funType(args, nfbodyType))
      }
      // Type-check the body of the LetRec in the extended environment
      val nbody = typeCheck(body, pt)(envWithFuns)
      // The entire expression gets the type of the body (pt)
      LetRec(nfuns, nbody).withType(nbody.tp)
    case App(fun, args) =>
      // Type-check the function expression with an unknown type
      val nFun: Exp = typeCheck(fun, UnknownType)(env)

      // Handling some errors
      val ftp = nFun.tp match {
        case ftp@FunType(fargs, _) if fargs.length == args.length =>
          ftp
        case ftp@FunType(fargs, rtp) if fargs.length < args.length =>
          error(s"too many arguments for method: ($fargs)$rtp", exp.pos)
          FunType(fargs ++ List.fill(args.length - fargs.length)(("", UnknownType)), rtp)
        case ftp@FunType(fargs, rtp) =>
          error(s"not enough arguments for method: ($fargs)$rtp", exp.pos)
          ftp
        case ArrayType(tp) =>
          FunType(List(("", IntType)), tp)
        case tp =>
          error(s"$tp does not take paramters", exp.pos)
          FunType(List.fill(args.length)(("", UnknownType)), pt)
      }

      // Type-check each argument against the expected type
      val nargs: List[Exp] = (args zip ftp.args).map {
        case (arg, (_, expectedTp)) => typeCheck(arg, expectedTp)(env)
      }

      // Transform some function applications into primitives on arrays.
      nFun.tp match {
        case ArrayType(tp) =>
          // If the function is an array, then we need to transform the function application
          Prim("block-get", List(nFun, nargs.head)).withType(tp)
        case _ => 
          App(nFun, nargs).withType(ftp.rtp)
      }
    case ArrayDec(size: Exp, etp: Type) => 
      // Type-check the size expression with the expected type Int
      val nsize = typeCheck(size, IntType)(env)
      // The type of the array is ArrayType(etp)
      ArrayDec(nsize, etp).withType(ArrayType(etp))
    case _ => BUG(s"malformed expresstion $exp")
  }
}
