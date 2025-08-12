package miniscala

import BitTwiddling.bitsToIntMSBF
import miniscala.{ SymbolicCPSTreeModule => H }
import miniscala.{ SymbolicCPSTreeModuleLow => L }

/**
 * Value-representation phase for the CPS language. Translates a tree
 * with high-level values (blocks, integers, booleans, unit) and
 * corresponding primitives to one with low-level values (blocks
 * and integers only) and corresponding primitives.
 *
 * @author Michel Schinz <Michel.Schinz@epfl.ch>
 */

object CPSValueRepresenter extends (H.Tree => L.Tree) {
  case class EnvMapping(envSym: Symbol, fvsIndex: Map[Symbol, Int])

  def apply(tree: H.Tree): L.Tree =
    transform(tree, None, Map.empty)

  val trueLit = bitsToIntMSBF(1, 1, 0, 1, 0)
  val falseLit = bitsToIntMSBF(0, 1, 0, 1, 0)
  val unitLit = bitsToIntMSBF(0, 0, 1, 0)
  val optimized = false

  private def transform(tree: H.Tree, currentEnv: Option[EnvMapping], locals: Map[Symbol, Boolean]): L.Tree =
  tree match {

    // Literals
    case H.LetL(name, IntLit(value), body) =>
      L.LetL(name, (value << 1) | 1, transform(body, currentEnv, locals))
    
    case H.LetL(name, CharLit(value), body) =>
      L.LetL(name, (value << 3) | bitsToIntMSBF(1, 1, 0), transform(body, currentEnv, locals))
    
    case H.LetL(name, BooleanLit(value), body) =>
      if(value==true)
        L.LetL(name, trueLit, transform(body, currentEnv, locals))
      else
        L.LetL(name, falseLit, transform(body, currentEnv, locals))

    case H.LetL(name, UnitLit, body) =>
      L.LetL(name, unitLit, transform(body, currentEnv, locals))

    // *************** Primitives ***********************
    // Make sure you implement all possible primitives
    // (defined in MiniScalaPrimitives.scala)
    //
    // Integer primitives
    // For adding two integers, the key is to add the two integers and then subtract 1
    case H.LetP(name, MiniScalaIntAdd, args, body) =>
      tempLetP(CPSAdd, args) { r => // r = n1(args(0)) + n2(args(1))
        tempLetL(1) { c1 =>         // c1 = 1
          L.LetP(name, CPSSub, Seq(r, c1), transform(body, currentEnv, locals)) } } // name = r - c1 = (n1 + n2) - 1

    // For substraction, the key is to subtract the two integers and then add 1
    case H.LetP(name, MiniScalaIntSub, args, body) =>
      tempLetP(CPSSub, args) { r => // r = n1(args(0)) - n2(args(1))
        tempLetL(1) { c1 =>         // c1 = 1
          L.LetP(name, CPSAdd, Seq(r, c1), transform(body, currentEnv, locals)) } } // name = r + c1 = (n1 - n2) + 1

    // For the next operations I am just going to decode each tagged integer,
    // perform the raw operation and then encode the result again
    // encode(n) = (n << 1) | 1
    // decode(n) = n >> 1
    case H.LetP(name, MiniScalaIntMul, Seq(a1, a2), body) =>
      // We will use a literal 1 to decode by shifting right
      tempLetL(1) { one =>
        // a1_decoded = a1 >> 1
        tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
          // a2_decoded = a2 >> 1
          tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
            // raw_result = a1_decoded * a2_decoded
            tempLetP(CPSMul, Seq(a1_decoded, a2_decoded)) { raw_result =>
              // shifted = raw_result << 1
              tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
                // final_result = shifted | 1
                L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    case H.LetP(name, MiniScalaIntDiv, Seq(a1, a2), body) =>
      // We will use a literal 1 to decode by shifting right
      tempLetL(1) { one =>
        // a1_decoded = a1 >> 1
        tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
          // a2_decoded = a2 >> 1
          tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
            // raw_result = a1_decoded / a2_decoded
            tempLetP(CPSDiv, Seq(a1_decoded, a2_decoded)) { raw_result =>
              // shifted = raw_result << 1
              tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
                // final_result = shifted | 1
                L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    case H.LetP(name, MiniScalaIntMod, Seq(a1, a2), body) =>
    // We will use a literal 1 to decode by shifting right
    tempLetL(1) { one =>
      // a1_decoded = a1 >> 1
      tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
        // a2_decoded = a2 >> 1
        tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
          // raw_result = a1_decoded % a2_decoded
          tempLetP(CPSMod, Seq(a1_decoded, a2_decoded)) { raw_result =>
            // shifted = raw_result << 1
            tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
              // final_result = shifted | 1
              L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    case H.LetP(name, MiniScalaIntArithShiftLeft, Seq(a1, a2), body) =>
    // We will use a literal 1 to decode by shifting right
    tempLetL(1) { one =>
      // a1_decoded = a1 >> 1
      tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
        // a2_decoded = a2 >> 1
        tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
          // raw_result = a1_decoded << a2_decoded
          tempLetP(CPSArithShiftL, Seq(a1_decoded, a2_decoded)) { raw_result =>
            // shifted = raw_result << 1
            tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
              // final_result = shifted | 1
              L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    case H.LetP(name, MiniScalaIntArithShiftRight, Seq(a1, a2), body) =>
    // We will use a literal 1 to decode by shifting right
    tempLetL(1) { one =>
      // a1_decoded = a1 >> 1
      tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
        // a2_decoded = a2 >> 1
        tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
          // raw_result = a1_decoded >> a2_decoded
          tempLetP(CPSArithShiftR, Seq(a1_decoded, a2_decoded)) { raw_result =>
            // shifted = raw_result << 1
            tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
              // final_result = shifted | 1
              L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    // case H.LetP(name, MiniScalaIntBitwiseAnd, Seq(a1, a2), body) =>
    // // We will use a literal 1 to decode by shifting right
    // tempLetL(1) { one =>
    //   // a1_decoded = a1 >> 1
    //   tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
    //     // a2_decoded = a2 >> 1
    //     tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
    //       // raw_result = a1_decoded & a2_decoded
    //       tempLetP(CPSAnd, Seq(a1_decoded, a2_decoded)) { raw_result =>
    //         // shifted = raw_result << 1
    //         tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
    //           // final_result = shifted | 1
    //           L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    // case H.LetP(name, MiniScalaIntBitwiseOr, Seq(a1, a2), body) =>
    // // We will use a literal 1 to decode by shifting right
    // tempLetL(1) { one =>
    //   // a1_decoded = a1 >> 1
    //   tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
    //     // a2_decoded = a2 >> 1
    //     tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
    //       // raw_result = a1_decoded | a2_decoded
    //       tempLetP(CPSOr, Seq(a1_decoded, a2_decoded)) { raw_result =>
    //         // shifted = raw_result << 1
    //         tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
    //           // final_result = shifted | 1
    //           L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    // case H.LetP(name, MiniScalaIntBitwiseXOr, Seq(a1, a2), body) =>
    // // We will use a literal 1 to decode by shifting right
    // tempLetL(1) { one =>
    //   // a1_decoded = a1 >> 1
    //   tempLetP(CPSArithShiftR, Seq(a1, one)) { a1_decoded =>
    //     // a2_decoded = a2 >> 1
    //     tempLetP(CPSArithShiftR, Seq(a2, one)) { a2_decoded =>
    //       // raw_result = a1_decoded ^ a2_decoded
    //       tempLetP(CPSXOr, Seq(a1_decoded, a2_decoded)) { raw_result =>
    //         // shifted = raw_result << 1
    //         tempLetP(CPSArithShiftL, Seq(raw_result, one)) { shifted =>
    //           // final_result = shifted | 1
    //           L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } } } }

    // I can just directly encode both integers isnce the LSB is preserved
    case H.LetP(name, MiniScalaIntBitwiseAnd, Seq(a1, a2), body) =>
      L.LetP(name, CPSAnd, Seq(a1, a2), transform(body, currentEnv, locals))

    case H.LetP(name, MiniScalaIntBitwiseOr, Seq(a1, a2), body) =>
      L.LetP(name, CPSOr, Seq(a1, a2), transform(body, currentEnv, locals))

    // For the XOR operation, I need to take into account that the LSB will be 
    // cleaned since 1 ⊕ 1 = 0 so I need to add 1 at the end
    case H.LetP(name, MiniScalaIntBitwiseXOr, Seq(a1, a2), body) =>
      tempLetP(CPSXOr, Seq(a1, a2)) { r =>  // r = n1(a1) ⊕ n2(a2)
        tempLetL(1) { c1 =>                 // c1 = 1
          L.LetP(name, CPSAdd, Seq(r, c1), transform(body, currentEnv, locals)) } }

    // Block primitives
    // For the block allocation, I fist need to decode the argument since it 
    // is a tagged integer and then allocate the block by calling the low-level
    // block allocation primitive
    case H.LetP(name, MiniScalaBlockAlloc(tag), Seq(size), body) =>
      tempLetL(1) { one =>
        // size_decoded = size >> 1
        tempLetP(CPSArithShiftR, Seq(size, one)) { size_decoded =>
          // Allocate the block: name = block-alloc-tag(size_decoded)
          L.LetP(name, CPSBlockAlloc(tag), Seq(size_decoded), transform(body, currentEnv, locals)) } }

    case H.LetP(name, MiniScalaBlockSet, Seq(block_ptr, index, value), body) =>
      // We need to take into account that the index is a tagged integer so we need to decode it first
      tempLetL(1) { one =>
        // index_decoded = inex >> 1
        tempLetP(CPSArithShiftR, Seq(index, one)) { index_decoded =>
          // Set the block: block-set(block_ptr, index_decoded, value)
          L.LetP(name, CPSBlockSet, Seq(block_ptr, index_decoded, value), transform(body, currentEnv, locals)) } }
      
    case H.LetP(name, MiniScalaBlockGet, Seq(block_ptr, index), body) =>
      // We need to take into account that the index is a tagged integer so we need to decode it first
      tempLetL(1) { one =>
        // index_decoded = inex >> 1
        tempLetP(CPSArithShiftR, Seq(index, one)) { index_decoded =>
          // Get the block: name = block-get(block_ptr, index_decoded)
          L.LetP(name, CPSBlockGet, Seq(block_ptr, index_decoded), transform(body, currentEnv, locals)) } }

    case H.LetP(name, MiniScalaBlockTag, Seq(block_ptr), body) =>
      // We will pass through the block_ptr since it is already a tagged pointer
      tempLetL(1) {one =>
        // raw_tag = block-tag(block_ptr)
        tempLetP(CPSBlockTag, Seq(block_ptr)) { raw_tag =>
          // shifted = raw_tag << 1
          tempLetP(CPSArithShiftL, Seq(raw_tag, one)) { shifted =>
            // final_tag = shifted | 1
            L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } }

    case H.LetP(name, MiniScalaBlockLength, Seq(block_ptr), body) =>
      // We will pass through the block_ptr since it is already a tagged pointer
      tempLetL(1) {one =>
        // raw_len = block-length(block_ptr)
        tempLetP(CPSBlockLength, Seq(block_ptr)) { raw_len =>
          // shifted = raw_len << 1
          tempLetP(CPSArithShiftL, Seq(raw_len, one)) { shifted =>
            // final_length = shifted | 1
            L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } }

    // Conversion primitives int->char/char->int
    case H.LetP(name, MiniScalaCharToInt, Seq(char), body) =>
      // We need to take into account that the char is taged as (char << 3) | 110
      // and the int is tagged as (int << 1) | 1, so I will just shift 2 bits to the right
      // and with that the last bit will be 1 that is the tag for an integer
      tempLetL(2) { two =>
        // single shift by 2
        L.LetP(name, CPSArithShiftR, Seq(char, two), transform(body, currentEnv, locals)) }

    case H.LetP(name, MiniScalaIntToChar, Seq(int), body) =>
      // We need to take into account that the char is taged as (char << 3) | 110
      // and the int is tagged as (int << 1) | 1, so in this case I will shift
      // 2 bits to the left and then add 6 to the result (110)
      tempLetL(2) { two =>
        // shift left by 2 => int << 2
        tempLetP(CPSArithShiftL, Seq(int, two)) { shifted =>
          // now OR with 6 => (int << 2) | 110
          tempLetL(6) { six =>
            L.LetP(name, CPSOr, Seq(shifted, six), transform(body, currentEnv, locals)) } } }

    // IO primitives
    case H.LetP(name, MiniScalaByteRead, Seq(), body) =>
      // raw = byte-read()
      tempLetP(CPSByteRead, Seq()) { raw =>
        // shifted = raw << 1
        tempLetL(1) { one =>
          tempLetP(CPSArithShiftL, Seq(raw, one)) { shifted =>
            // result = shifted | 1
            L.LetP(name, CPSOr, Seq(shifted, one), transform(body, currentEnv, locals)) } } }

    case H.LetP(name, MiniScalaByteWrite, Seq(arg), body) =>
      // We need to decode the argument to get an untagged byte
      tempLetL(1) { one =>
        // decoded = arg >> 1
        tempLetP(CPSArithShiftR, Seq(arg, one)) { decoded =>
          // We apply the byte-write primitive with the decoded argument
          tempLetP(CPSByteWrite, Seq(decoded)) { _ =>
            // We return the unit tagged
            L.LetL(name, unitLit, transform(body, currentEnv, locals)) } } }

    // Other primitives
    case H.LetP(name, MiniScalaId, args, body) =>
      // We just return the argument since it is already a tagged integer
      L.LetP(name, CPSId, args, transform(body, currentEnv, locals))

    // Continuations nodes (LetC, AppC)
    // Continuation definitions: transform each continuation's body
    case H.LetC(cntDefs, body) =>
      // Recursively transform each continuation definition and then the body
      L.LetC(cntDefs.map { cd =>
        // For each continuation, the arguments are already names and we transform the body
        L.CntDef(cd.name, cd.args, transform(cd.body, currentEnv, locals)) 
      }, transform(body, currentEnv, locals))

    // Continuation applications: simply pass through the continuation name and the arguments
    case H.AppC(cnt, args) =>
      L.AppC(cnt, args)

    // Functions nodes (LetF, AppF)
    // Function definitions: transform each function's body
    case H.LetF(funDefs, body) =>
      val transformedBody = transform(body, currentEnv, locals)

      // Recursively transform each function definition and then the body
      val (allocCnts, new_funDefs) = funDefs.foldRight((transformedBody, Seq[L.FunDef]())) {
        case (H.FunDef(fName, retC, args, fBody), (innerBody, accFuns)) =>
          // Compute the free variables of the function body
          val freeVars = computeFreeVars(fBody, (fName +: retC +: args).toSet)

          // Make a freh "wrapper" continuation that binds the free variables
          val wFName = Symbol.fresh(fName.name + "_wrapper")

          // Create the environment parameter symbol
          val env = Symbol.fresh(fName.name + "_env")

          // Transform the function body naively ignoring the free variables for the moment
          val naiveBody = transform(fBody, None, locals)

          // Pre-load the free variables: build a chain of LetP that does block-get
          // and returns (chain, aMap) where aMap is fv -> loadedSym
          val (preloadChain, fvSubstMap) = preloadFreeVars(env, freeVars, naiveBody)

          // Now we substitute references to each fv in naiveBody => x_loaded
          // We'll do the substitution on the final body, once we nest it in preloadChain
          // so we define a small helper:
          def nestBody(chain: L.Tree): L.Tree = chain match {
            case L.AppC(placeholder, _) if placeholder.name.startsWith("END") =>
              // We have reached the dummy placeholder; nest the substituted body here.
              substTree(naiveBody, fvSubstMap)
            case L.LetP(n, p, args, inner) =>
              L.LetP(n, p, args, nestBody(inner))

            case L.LetL(n, v, inner) =>
              L.LetL(n, v, nestBody(inner))

            case L.LetC(cnts, inner) =>
              L.LetC(cnts, nestBody(inner))

            case L.LetF(funs, inner) =>
              L.LetF(funs, nestBody(inner))

            // If we accidentally see something else, just continue...  
            case other => other
          }

          val finalFBody = nestBody(preloadChain)

          // Build the new low-level function definition (the "wrapper")
          val wrapper = L.FunDef(wFName, retC, env +: args, finalFBody)

          // Now allocate the closure block for fName
          // Suppose we have (|freeVars| + 1) slots: slot 0 is wFName, slots [1...|freeVars|] are the free variables
          // We produce something like:
          //    val fName = block-alloc-tag(|freeVars| + 1);
          //    block-set(fName, 0, wFName);
          //    block-set(fName, i, freeVars(i-1)) for i in 1 to |freeVars|

          // Make a chain of LetP calls to do the block allocation and set the values
          val closureAlloc = {
            val closureSize = freeVars.size + 1
            tempLetL(closureSize) { size =>
              L.LetP(fName, CPSBlockAlloc(202), Seq(size),  { // fName = block-alloc-202(closureSize)
                // Fill slot 0 with wFName
                tempLetL(0) { zero =>
                  L.LetP(Symbol.fresh(fName.name + "_set_0"), CPSBlockSet, Seq(fName, zero, wFName), {
                    // Fill each slot i with the corresponding free variable
                    freeVars.zipWithIndex.foldRight(innerBody) {
                      case ((fv, i), bodySoFar) =>
                        tempLetL(i+1) { slotIndex =>
                          L.LetP(Symbol.fresh("setFV_" + i), CPSBlockSet, Seq(fName, slotIndex, fv), bodySoFar)}
                    }
                  })}
              })}
          }

          // Add wrapper to the list of new function definitions and chain
          // closureAlloc before the innerBody
          (closureAlloc, wrapper +: accFuns)

      }

      // Now wrap everything in a L.LetF that defines all wFuns at once 
      // and then run the allocCnts that allocates the closures and ends with the transformedBody
      L.LetF(new_funDefs, allocCnts)

    // Function applications: transform the arguments and pass through the function name
    case H.AppF(fun, retC, args) =>
      // We know that "fun" is actually the closure block pointer
      // so we must do block-get(fun, 0) to get the function name
      tempLetL(0) { zero=>
        tempLetP(CPSBlockGet, Seq(fun, zero)) { funName =>
          // Now we can apply the function
          L.AppF(funName, retC, fun +: args) 
          } 
        }
  
    // ********************* Conditionnals ***********************
    // Type tests
    // Check if the argument is a block
    case H.If(MiniScalaBlockP, Seq(a), thenC, elseC) =>
      ifEqLSB(a, Seq(0, 0), thenC, elseC)
    
    // Check if the argument is an integer
    case H.If(MiniScalaIntP, Seq(a), thenC, elseC) =>
      ifEqLSB(a, Seq(1), thenC, elseC)

    // Check if the argument is a boolean
    case H.If(MiniScalaBoolP, Seq(a), thenC, elseC) =>
      ifEqLSB(a, Seq(1, 0, 1, 0), thenC, elseC)
    
    // Check if the argument is a char
    case H.If(MiniScalaCharP, Seq(a), thenC, elseC) =>
      ifEqLSB(a, Seq(1, 1, 0), thenC, elseC)

    // Check if the argument is a unit
    case H.If(MiniScalaUnitP, Seq(a), thenC, elseC) =>
      ifEqLSB(a, Seq(0, 0, 1, 0), thenC, elseC)

    // Test primitives (<, >, ==, ...) we just need to pass the arguments from the
    // high-level tree to the low-level tree
    // "<" test
    case H.If(MiniScalaIntLt, Seq(a1, a2), thenC, elseC) =>
      L.If(CPSLt, Seq(a1, a2), thenC, elseC)

    // "<=" test
    case H.If(MiniScalaIntLe, Seq(a1, a2), thenC, elseC) =>
      L.If(CPSLe, Seq(a1, a2), thenC, elseC)

    // ">" test
    case H.If(MiniScalaIntGt, Seq(a1, a2), thenC, elseC) =>
      L.If(CPSGt, Seq(a1, a2), thenC, elseC)
    
    // ">=" test
    case H.If(MiniScalaIntGe, Seq(a1, a2), thenC, elseC) =>
      L.If(CPSGe, Seq(a1, a2), thenC, elseC)
  
    // "==" test
    case H.If(MiniScalaEq, Seq(a1, a2), thenC, elseC) =>
      L.If(CPSEq, Seq(a1, a2), thenC, elseC)
    
    // "!=" test
    case H.If(MiniScalaNe, Seq(a1, a2), thenC, elseC) =>
      L.If(CPSNe, Seq(a1, a2), thenC, elseC)

    // Halt case
    case H.Halt(arg) =>
      // We need to decode the argument before halting
      tempLetL(1) { one =>
        tempLetP(CPSArithShiftR, Seq(arg, one)) { decoded =>
          L.Halt(decoded)}}
  }

  /*
   * Auxilary function.
   *
   * Example:
   *  // assuming we have a function with symbol f and the return continuation is rc:
   *
   *  val names = Seq("first", "second")
   *  val values = Seq(42, 112)
   *  val inner = L.AppF(f, rc, names)
   *  val res = wrap(names zip values , inner) {
   *    case ((n, v), inner) => L.LetL(n, v, inner)
   *  }
   *
   *  // res is going to be the following L.Tree
   *  L.LetL("first", 42,
   *    L.LetL("second", 112,
   *      L.AppF(f, rc, Seq("first", "second"))
   *    )
   *  )
   */
  private def wrap[T](args: Seq[T], inner: L.Tree)(createLayer: (T, L.Tree) => L.Tree) = {
    def addLayers(args: Seq[T]): L.Tree = args match {
      case h +: t => createLayer(h, addLayers(t))
      case _ => inner
    }
    addLayers(args)
  }

  /*
  * Builds a chain of LetP(...) nodes that do:
  *   x_loaded_i = block-get(envSym, i+1)
  * for each free variable x in order, then eventually calls:
  *   AppC(Symbol("END"), ...)
  * so we have a placeholder to nest the real body later.
  *
  * Returns:
  *   (the chain: L.Tree,  Map[Symbol, Symbol])
  * where the map is from fv -> x_loaded_i
  */
  private def preloadFreeVars(envSym: Symbol,
                      fvs: List[Symbol],
                      bodyPlaceholder: L.Tree): (L.Tree, Map[Symbol, Symbol]) = {
    val endCont = Symbol.fresh("END") // placeholder "continuation"

    // Start from a dummy "AppC(END, ...)" to mark where real body goes
    val startChain: L.Tree = L.AppC(endCont, Seq())

    // Fold from last to first so we nest them inside
    val (chain, fvMap) =
      fvs.zipWithIndex.foldRight((startChain, Map.empty[Symbol, Symbol])) {
        case ((fv, i), (chainSoFar, mapSoFar)) =>
          val slotIndex = i + 1
          val loadedSym = Symbol.fresh(fv.name + "_loaded")

          // build: LetL(tmpSlot, slotIndex,
          //         LetP(loadedSym, CPSBlockGet, [envSym, tmpSlot], chainSoFar))
          val newChain =
            tempLetL(slotIndex) { tmpSlot =>
              L.LetP(loadedSym, CPSBlockGet, Seq(envSym, tmpSlot), chainSoFar)
            }

          val newMap = mapSoFar + (fv -> loadedSym)
          (newChain, newMap)
      }

    (chain, fvMap)
  }

  /**
  * Replaces each key in fvSubstMap with the fresh loadedSym.
  * In a real compiler, you might have a more robust approach,
  * or you might rely on `tree.subst(...)` if it’s already defined.
  */
  private def substTree(tree: L.Tree, fvSubstMap: Map[Symbol, Symbol]): L.Tree = {
    // Define remap here so it's visible inside the match
    def remap(x: Symbol): Symbol =
      fvSubstMap.getOrElse(x, x)

    tree match {
      case L.LetL(name, value, body) =>
        L.LetL(remap(name), value, substTree(body, fvSubstMap))

      case L.LetP(name, prim, args, body) =>
        L.LetP(remap(name), prim, args.map(remap), substTree(body, fvSubstMap))

      case L.LetC(cnts, body) =>
        L.LetC(cnts.map { c =>
          L.CntDef(remap(c.name), c.args.map(remap), substTree(c.body, fvSubstMap))
        }, substTree(body, fvSubstMap))

      case L.LetF(funs, body) =>
        L.LetF(funs.map { f =>
          L.FunDef(
            remap(f.name),
            remap(f.retC),
            f.args.map(remap),
            substTree(f.body, fvSubstMap)
          )
        }, substTree(body, fvSubstMap))

      case L.AppC(cnt, args) =>
        L.AppC(remap(cnt), args.map(remap))

      case L.AppF(fun, retC, args) =>
        L.AppF(remap(fun), remap(retC), args.map(remap))

      case L.If(cond, args, thenC, elseC) =>
        L.If(cond, args.map(remap), remap(thenC), remap(elseC))

      case L.Halt(arg) =>
        L.Halt(remap(arg))
    }
  }

  def computeFreeVars(fBody: H.Tree, boundVars: Set[Symbol]): List[Symbol] = {
    // rawFVs is just freeVariables(fBody) but we might need to pass an implicit map
    val rawFVs: Set[Symbol] = freeVariables(fBody)(Map.empty)  
    // remove anything already bound
    val actualFVs = rawFVs -- boundVars
    // sorted list
    actualFVs.toList.sortBy(_.name)
  }

  private def freeVariables(tree: H.Tree)
                         (implicit worker: Map[Symbol, Set[Symbol]]): Set[Symbol] =
  tree match {
    case H.LetL(name, _, body) =>
      freeVariables(body) - name

    case H.LetP(name, _, args, body) =>
      freeVariables(body) - name ++ args

    // case H.LetC(cnts, body) =>
    //   val contNames = cnts.map(_.name).toSet
    //   val cntsFVs = cnts.foldLeft(Set.empty[Symbol]) { (acc, cntDef) =>
    //     acc union (freeVariables(cntDef.body) -- (cntDef.args.toSet + cntDef.name))
    //   }
    //   (freeVariables(body) -- contNames) ++ cntsFVs

    case H.LetC(cnts, body) =>
        val boundCnts = cnts.map(_.name).toSet
        val bodyFVs = freeVariables(body) -- boundCnts
        val cntsFVs = cnts.foldLeft(Set.empty[Symbol]) { (acc, cntDef) =>
          acc union (freeVariables(cntDef.body) -- boundCnts -- cntDef.args.toSet -- Set(cntDef.name))
        }
        bodyFVs ++ cntsFVs

    // case H.LetF(funs, body) =>
    //   // Union of body’s free variables + each function’s free variables
    //   // minus (function name, retC, and args)
    //   val funsFVs = funs.foldLeft(Set.empty[Symbol]) { (acc, funDef) =>
    //     acc ++ (freeVariables(funDef.body) -- (funDef.name +: funDef.retC +: funDef.args))
    //   }
    //   freeVariables(body) ++ funsFVs

    case H.LetF(funs, body) =>
      val boundFuns = funs.map(_.name).toSet
      val bodyFVs = freeVariables(body) -- boundFuns
      val funsFVs = funs.foldLeft(Set.empty[Symbol]) { (acc, funDef) =>
        acc union (freeVariables(funDef.body) -- boundFuns -- funDef.args.toSet -- Set(funDef.name, funDef.retC))
      }
      bodyFVs ++ funsFVs

    // AppC: gather the continuation name + arguments
    case H.AppC(cnt, args) =>
      Set(cnt) ++ args

    // AppF: gather function name, return continuation, plus arguments
    case H.AppF(fun, retC, args) =>
      Set(fun, retC) ++ args

    // If: gather the tested arguments plus thenC/elseC
    case H.If(_, args, thenC, elseC) =>
      args.toSet + thenC + elseC

    // Halt: gather the single arg
    case H.Halt(arg) =>
      Set(arg)
  }

  private def freeVariables(cnt: H.CntDef)
                           (implicit worker: Map[Symbol, Set[Symbol]])
      : Set[Symbol] =
    freeVariables(cnt.body) -- cnt.args

  private def freeVariables(fun: H.FunDef)
                           (implicit worker: Map[Symbol, Set[Symbol]])
      : Set[Symbol] =
    freeVariables(fun.body) - fun.name -- fun.args

  // Tree builders

  /**
   * Call body with a fresh name, and wraps its resulting tree in one
   * that binds the fresh name to the given literal value.
   */
  private def tempLetL(v: Int)(body: L.Name => L.Tree): L.Tree = {
    val tempSym = Symbol.fresh("t")
    L.LetL(tempSym, v, body(tempSym))
  }

  /**
   * Call body with a fresh name, and wraps its resulting tree in one
   * that binds the fresh name to the result of applying the given
   * primitive to the given arguments.
   */
  private def tempLetP(p: L.ValuePrimitive, args: Seq[L.Name])
                      (body: L.Name => L.Tree): L.Tree = {
    val tempSym = Symbol.fresh("t")
    L.LetP(tempSym, p, args, body(tempSym))
  }

  /**
   * Generate an If tree to check whether the least-significant bits
   * of the value bound to the given name are equal to those passed as
   * argument. The generated If tree will apply continuation tC if it
   * is the case, and eC otherwise. The bits should be ordered with
   * the most-significant one first (e.g. the list (1,1,0) represents
   * the decimal value 6).
   */
  private def ifEqLSB(arg: L.Name, bits: Seq[Int], tC: L.Name, eC: L.Name)
      : L.Tree =
    tempLetL(bitsToIntMSBF(bits map { b => 1 } : _*)) { mask =>
      tempLetP(CPSAnd, Seq(arg, mask)) { masked =>
        tempLetL(bitsToIntMSBF(bits : _*)) { value =>
          L.If(CPSEq, Seq(masked, value), tC, eC) } } }
}
