package project2

import org.scalatest._
import java.io.{ByteArrayOutputStream, PrintWriter}

// Define the stream method
trait TestOutput {
  import Language._

  val out = new ByteArrayOutputStream
  val pOut = new PrintWriter(out, true)
  def stream = pOut
  def emitCode(ast: Exp): Unit

  def code(ast: Exp) = {
    emitCode(ast)
    out.toString.stripLineEnd
  }
}

class CompilerTest extends TimedSuite {
  import Language._

  def runner(src: String, gData: Map[Char,Int] = Map()) = new ASMRunner(src, gData)

  def testCompiler(ast: Exp, res: Int) = {
    val interpreter = new X86Compiler with TestOutput

    val code = interpreter.code(ast)
    val asm = runner(code)

    assert(asm.assemble == 0, "Code generated couldn't be assembled")
    assert(asm.run == res, "Invalid result")
  }

  test("37") {
    testCompiler(Lit(-21), -21)
    testCompiler(Prim("-", Lit(10), Lit(2)), 8)
  }
  test("38") {
    testCompiler(Let("x",Lit(10),Prim("*",Ref("x"),Lit(2))), 20)
    testCompiler(Prim("-",Prim("*",Prim("*",Prim("*",Lit(2),Lit(9)),Lit(5)),Lit(3)),Prim("/",Prim("/",Lit(18),Lit(6)),Lit(3))), 269)
  }
  test("39") {
    testCompiler(If(Cond(">", Lit(3), Lit(5)), Lit(1), Lit(2)), 2)
    testCompiler(If(Cond(">", Lit(3), Lit(5)),
      If(Cond("<", Lit(9), Lit(0)), Lit(1), Lit(2)),
      Lit(4)), 4)
  }
  test("40") {
    testCompiler(VarDec("x",Lit(1),Let("x",Lit(5),Lit(4))), 4)
    testCompiler(VarDec("x",Lit(1),VarAssign("x",Lit(5))), 5)
  }
  test("41") {
    testCompiler(VarDec("a",Lit(4),VarDec("b",Lit(1),VarAssign("a",VarAssign("b",Lit(7))))), 7)
  }
  // test with While
  test("42") {
    testCompiler(VarDec("x",Lit(2),VarDec("y",Lit(0),While(Cond("<",Ref("y"),Lit(3)),Let("dummy",
      VarAssign("x",Prim("*",Ref("x"),Ref("x"))),VarAssign("y",Prim("+",Ref("y"),Lit(1)))),Ref("x")))), 256)
    testCompiler(VarDec("x",Lit(5),
      While(Cond(">",Ref("x"),Lit(0)),VarAssign("x",Prim("-",Ref("x"),Lit(1))),Ref("x"))), 0)
  }

}
