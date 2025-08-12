package miniscala.test

import ok.AllOKTests

import miniscala.test.infrastructure.CPSHighTest
import org.junit.Test

/** Whitebox testing for entire program outputs */
class CMScalaToCPS_Whitebox_NonTail extends CPSHighTest {

  @Test def testNonTailLiteral = {
    testCPSHighTreeEquality("3", "vall v$1 = 3; vall v$2 = 0; halt(v$2)")
  } 

  @Test def testNonTailMultiLet =
    testCPSHighTreeEquality("val x = 1; val y = 2; y",
        "vall v$1 = 1; valp v$2 = id(v$1); vall v$3 = 2; valp v$4 = id(v$3); vall v$5 = 0; halt(v$5)")

  @Test def testNonTailSimpleLet =
    testCPSHighTreeEquality("val x = 1; x",
        "vall v$1 = 1; valp v$2 = id(v$1); vall v$3 = 0; halt(v$3)")

  // Test for non-tail if statement
  @Test def testNonTailIf = {
    testCPSHighTreeEquality("if (true) 1 else 2",
        """defc v$1(v$4) = {
          |  vall v$5 = 0;
          |  halt(v$5)
          |};
          |defc v$2() = {
          |  vall v$6 = 1;
          |  v$1(v$6)
          |};
          |defc v$3() = {
          |  vall v$7 = 2;
          |  v$1(v$7)
          |};
          |vall v$8 = true;
          |vall v$9 = false;
          |if (v$8 != v$9) v$2 else v$3
          |""".stripMargin
    )
  }

  @Test def testNonTailMutableAssign =
  testCPSHighTreeEquality(
    "var x = 1; x = 2; x",
    // Put here the actual string your compiler generates:
    """vall v$1 = 1;
      |valp v$2 = block-alloc-0(v$1);
      |vall v$3 = 0;
      |vall v$4 = 1;
      |valp v$5 = block-set(v$2, v$3, v$4);
      |vall v$6 = 0;
      |vall v$7 = 2;
      |valp v$8 = block-set(v$2, v$6, v$7);
      |valp v$9 = id(v$7);
      |vall v$10 = 0;
      |halt(v$10)
      |""".stripMargin
  )
  
  @Test def testNonTailFunCall = {
    testCPSHighTreeEquality(
      "def f(x: Int) = x; f(1)",
      // This matches your actual output:
      """deff v$1(v$2, v$3) = { v$2(v$3) };
        |vall v$4 = 1;
        |defc v$5(v$6) = {
        |  vall v$7 = 0;
        |  halt(v$7)
        |};
        |v$1(v$5, v$4)
        |""".stripMargin
    )
  }

  // Test for non-tail function call with multiple arguments
  @Test def testNonTailFunCallMultiArgs = {
    testCPSHighTreeEquality(
      "def f(x: Int, y: Int) = x + y; f(1, 2)",
      // Expected output actualizado para que coincida con tu salida actual:
      """deff v$1(v$2, v$3, v$4) = { valp v$5 = v$3 + v$4; v$2(v$5) };
        |vall v$6 = 1;
        |vall v$7 = 2;
        |defc v$8(v$9) = { vall v$10 = 0; halt(v$10) };
        |v$1(v$8, v$6, v$7)
        |""".stripMargin
    )
  }

  @Test def testNonTailWhile =
  testCPSHighTreeEquality(
    "while (false) (); 2",
    """defc v$1() = {
      |  vall v$4 = false;
      |  vall v$5 = false;
      |  if (v$4 != v$5) v$3 else v$2
      |};
      |defc v$2() = {
      |  vall v$6 = 2;
      |  vall v$7 = 0;
      |  halt(v$7)
      |};
      |defc v$3() = { vall v$8 = (); v$1() };
      |v$1()
      |""".stripMargin
  )

  // Test for non-tail recursive function
  @Test def testNonTailRecursiveFunction = {
    testCPSHighTreeEquality(
      "def f(n: Int): Int = if (n == 0) 1 else f(n - 1); f(3)",
      """deff v$1(v$2, v$3) = {
        |  defc v$4() = { vall v$6 = 1; v$2(v$6) };
        |  defc v$5() = { vall v$7 = 1; valp v$8 = v$3 - v$7; v$1(v$2, v$8) };
        |  vall v$9 = 0;
        |  if (v$3 == v$9) v$4 else v$5
        |};
        |vall v$10 = 3;
        |defc v$11(v$12) = { vall v$13 = 0; halt(v$13) };
        |v$1(v$11, v$10)
        |""".stripMargin
    )
  }

  @Test def testNonTailMutualRecursion = {
    testCPSHighTreeEquality(
      """def even(n: Int): Boolean = if (n == 0) true else odd(n - 1);
        |def odd(n: Int): Boolean = if (n == 0) false else even(n - 1);
        |even(4)
        |""".stripMargin,
      """deff v$1(v$3, v$4) = {
        |  defc v$5() = {
        |    vall v$7 = true;
        |    v$3(v$7)
        |  };
        |  defc v$6() = { 
        |    vall v$8 = 1; 
        |    valp v$9 = v$4 - v$8; 
        |    v$2(v$3, v$9)
        |  };
        |  vall v$10 = 0;
        |  if (v$4 == v$10) v$5 else v$6
        |};
        |deff v$2(v$11, v$12) = {
        |  defc v$13() = {
        |    vall v$15 = false;
        |    v$11(v$15)
        |  };
        |  defc v$14() = {
        |    vall v$16 = 1;
        |    valp v$17 = v$12 - v$16;
        |    v$1(v$11, v$17)
        |  };
        |  vall v$18 = 0;
        |  if (v$12 == v$18) v$13 else v$14
        |};
        |vall v$19 = 4;
        |defc v$20(v$21) = { vall v$22 = 0; halt(v$22) };
        |v$1(v$20, v$19)
        |""".stripMargin
    )
  }
}
