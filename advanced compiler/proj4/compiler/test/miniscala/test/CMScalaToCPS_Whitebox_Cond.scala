package miniscala.test

import miniscala.test.infrastructure.CPSHighTest
import org.junit.Test

class CMScalaToCPS_Whitebox_Cond extends CPSHighTest {
  @Test def testCondNestedTrueTrue =
    testCPSHighTreeEquality("if (if (3 == 4) true else true) 1 else 2", """defc v$1(v$4) = { vall v$5 = 0; halt(v$5) };
      |defc v$2() = { vall v$6 = 1; v$1(v$6) };
      |defc v$3() = { vall v$7 = 2; v$1(v$7) };
      |vall v$8 = 3;
      |vall v$9 = 4;
      |if (v$8 == v$9) v$2 else v$2
      """.stripMargin)

  @Test def testCondNestedTrueTrueSimple =
    testCPSHighTreeEquality(
      "if (if (true) true else true) 1 else 2",
      """defc v$1(v$4) = { vall v$5 = 0; halt(v$5) };
        |defc v$2() = { vall v$6 = 1; v$1(v$6) };
        |defc v$3() = { vall v$7 = 2; v$1(v$7) };
        |vall v$8 = true;
        |vall v$9 = false;
        |if (v$8 != v$9) v$2 else v$2
        """.stripMargin)

  @Test def testCondNestedTrueFalse =
  testCPSHighTreeEquality(
    "if (if (true) true else false) 1 else 2",
    """defc v$1(v$4) = { vall v$5 = 0; halt(v$5) };
      |defc v$2() = { vall v$6 = 1; v$1(v$6) };
      |defc v$3() = { vall v$7 = 2; v$1(v$7) };
      |vall v$8 = true;
      |vall v$9 = false;
      |if (v$8 != v$9) v$2 else v$3
      """.stripMargin
  )

  @Test def testCondNestedTrueFalseWithPrim =
  testCPSHighTreeEquality(
    "if (if (3 == 4) true else false) 1 else 2",
    """defc v$1(v$4) = { vall v$5 = 0; halt(v$5) };
      |defc v$2() = { vall v$6 = 1; v$1(v$6) };
      |defc v$3() = { vall v$7 = 2; v$1(v$7) };
      |vall v$8 = 3;
      |vall v$9 = 4;
      |if (v$8 == v$9) v$2 else v$3
      """.stripMargin
  )
}
