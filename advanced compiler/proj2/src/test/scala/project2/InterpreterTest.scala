package project2

import org.scalatest._

class InterpretTest extends TimedSuite {
  import Language._

  def testInterpreter(ast: Exp, res: Int) = {
    val interpreter = new StackInterpreter

    assert(res == interpreter.run(ast), "Interpreter does not return the correct value")
  }

  test("33") {
    testInterpreter(Lit(-21), -21)
    testInterpreter(Prim("-", Lit(10), Lit(2)), 8)
  }
  test("34") {
    testInterpreter(Let("x",Lit(10),Prim("*",Ref("x"),Lit(2))), 20)
    testInterpreter(Prim("-",Prim("*",Prim("*",Prim("*",Lit(2),Lit(9)),Lit(5)),Lit(3)),Prim("/",Prim("/",Lit(18),Lit(6)),Lit(3))), 269)
  }
  test("35") {
    testInterpreter(If(Cond(">", Lit(3), Lit(5)), Lit(1), Lit(2)), 2)
    testInterpreter(If(Cond(">", Lit(3), Lit(5)),
      If(Cond("<", Lit(9), Lit(0)), Lit(1), Lit(2)),
      Lit(4)), 4)
  }
  test("36") {
    testInterpreter(VarDec("x",Lit(1),Let("x",Lit(5),Lit(4))), 4)
    testInterpreter(VarDec("x",Lit(1),VarAssign("x",Lit(5))), 5)
  }

}
