package project1

import java.io._
import org.scalatest._

// Define the stream method
trait TestOutput {
  val out = new ByteArrayOutputStream
  val pOut = new PrintWriter(out, true)
  def stream = pOut
  def emitCode(ast: Exp): Unit

  def code(ast: Exp) = {
    emitCode(ast)
    out.toString.stripLineEnd
  }
}

class StackGeneratorTest extends TimedSuite {

  def runner(src: String, gData: Map[Char,Int] = Map()) = new ASMRunner(src, gData)

  // Function Helper for StackASMGenerator
  def testStackASMGenerator(ast: Exp, res: Int) = {
    val gen = new StackASMGenerator with TestOutput

    val code = gen.code(ast)
    val asm = runner(code)

    assert(asm.assemble == 0, "Code generated couldn't be assembled")
    assert(asm.run == res, "Invalid result")
  }

  test("SingleDigit") {
    testStackASMGenerator(Lit(2), 2)
  }

  test("SingleAddopAdd") {
    testStackASMGenerator(Plus(Lit(1),Lit(1)), 2)
  }

  test("MultipleAddopAdd") {
    testStackASMGenerator(Lit(1), 1)
    testStackASMGenerator(Plus(Lit(1), Lit(2)), 3)
    testStackASMGenerator(Minus(Lit(4), Lit(2)), 2)
    testStackASMGenerator(Plus(Plus(Lit(1), Lit(2)),Lit(3)), 6)
  }

  test("ArithOpParser") {
    testStackASMGenerator(Lit(1), 1)
    testStackASMGenerator(Times(Lit(4),Lit(2)), 8)
    testStackASMGenerator(Plus(Lit(1),Times(Lit(2),Lit(3))), 7)
    testStackASMGenerator(Minus(Lit(9),Times(Lit(4),Lit(2))), 1)
  }

  test("ArithParOpParser") {
    testStackASMGenerator(Minus(Lit(0),Lit(1)), -1)
    testStackASMGenerator(Plus(Minus(Lit(0),Lit(3)),Lit(4)), 1)
    testStackASMGenerator(Times(Lit(4),Minus(Lit(0),Lit(3))), -12)
    testStackASMGenerator(Plus(Plus(Lit(1),Lit(2)), Lit(3)), 6)
    testStackASMGenerator(Times(Plus(Lit(1),Lit(2)),Lit(3)), 9)
  }

}

class RegGeneratorTest extends TimedSuite {

  def runner(src: String, gData: Map[Char,Int] = Map()) = new ASMRunner(src, gData)

  // Function Helper for RegASMGenerator
  def testRegASMGenerator(ast: Exp, res: Int) = {
    val gen = new RegASMGenerator with TestOutput

    val code = gen.code(ast)
    val asm = runner(code)

    assert(asm.assemble == 0, "Code generated couldn't be assembled")
    assert(asm.run == res, "Invalid result")
  }

  test("SingleDigit") {
    testRegASMGenerator(Lit(2), 2)
  }

  test("SingleAddopAdd") {
    testRegASMGenerator(Plus(Lit(1),Lit(1)), 2)
  }

  test("MultipleAddopAdd") {
    testRegASMGenerator(Lit(1), 1)
    testRegASMGenerator(Plus(Lit(1), Lit(2)), 3)
    testRegASMGenerator(Minus(Lit(4), Lit(2)), 2)
    testRegASMGenerator(Plus(Plus(Lit(1), Lit(2)),Lit(3)), 6)
  }

  test("ArithOpParser") {
    testRegASMGenerator(Lit(1), 1)
    testRegASMGenerator(Times(Lit(4),Lit(2)), 8)
    testRegASMGenerator(Plus(Lit(1),Times(Lit(2),Lit(3))), 7)
    testRegASMGenerator(Minus(Lit(9),Times(Lit(4),Lit(2))), 1)
  }

  test("ArithParOpParser") {
    testRegASMGenerator(Minus(Lit(0),Lit(1)), -1)
    testRegASMGenerator(Plus(Minus(Lit(0),Lit(3)),Lit(4)), 1)
    testRegASMGenerator(Times(Lit(4),Minus(Lit(0),Lit(3))), -12)
    testRegASMGenerator(Plus(Plus(Lit(1),Lit(2)), Lit(3)), 6)
    testRegASMGenerator(Times(Plus(Lit(1),Lit(2)),Lit(3)), 9)
  }
}
