package miniscala.test

import miniscala.test.infrastructure.CPSLowTest
import org.junit.Test

/** Whitebox testing for entire program outputs */
class CPSValueRepresentation_Whitebox extends CPSLowTest {

  // Starting this "Value representation" assignment, we will not have whitebox
  // tests anymore. We will keep the black box tests, to check your submission
  // correctness, but from now on it's up to you to find the best translation.

  // Nevertheless, here's a test, to have an example:
  @Test def testValueReprOnePlusTwo =
    testCPSLowTreeEquality("1 + 2",
      """vall v$1 = 3;
        |vall v$2 = 5;
        |valp v$3 = v$1 + v$2;
        |vall v$4 = 1;
        |valp v$5 = v$3 - v$4;
        |vall v$6 = 1;
        |vall v$7 = 1;
        |valp v$8 = v$6 >> v$7;
        |halt(v$8)""".stripMargin)

  @Test def testValueReprSixMinusThree =
    testCPSLowTreeEquality("6 - 3",
      """vall v$1 = 13;
        |vall v$2 = 7;
        |valp v$3 = v$1 - v$2;
        |vall v$4 = 1;
        |valp v$5 = v$3 + v$4;
        |vall v$6 = 1;
        |vall v$7 = 1;
        |valp v$8 = v$6 >> v$7;
        |halt(v$8)""".stripMargin)

  @Test def testValueRprThreeTimesFour =
    testCPSLowTreeEquality("3 * 4",
      """vall v$1 = 7;
        |vall v$2 = 9;
        |vall v$3 = 1;
        |valp v$4 = v$1 >> v$3;
        |valp v$5 = v$2 >> v$3;
        |valp v$6 = v$4 * v$5;
        |valp v$7 = v$6 << v$3;
        |valp v$8 = v$7 | v$3;
        |vall v$9 = 1;
        |vall v$10 = 1;
        |valp v$11 = v$9 >> v$10;
        |halt(v$11)""".stripMargin)

  @Test def testValueRprFourDivTwo =
    testCPSLowTreeEquality("4 / 2",
      """vall v$1 = 9;
        |vall v$2 = 5;
        |vall v$3 = 1;
        |valp v$4 = v$1 >> v$3;
        |valp v$5 = v$2 >> v$3;
        |valp v$6 = v$4 / v$5;
        |valp v$7 = v$6 << v$3;
        |valp v$8 = v$7 | v$3;
        |vall v$9 = 1;
        |vall v$10 = 1;
        |valp v$11 = v$9 >> v$10;
        |halt(v$11)""".stripMargin)

  @Test def testValueRprFourModTwo =
    testCPSLowTreeEquality("4 % 2",
      """vall v$1 = 9;
        |vall v$2 = 5;
        |vall v$3 = 1;
        |valp v$4 = v$1 >> v$3;
        |valp v$5 = v$2 >> v$3;
        |valp v$6 = v$4 % v$5;
        |valp v$7 = v$6 << v$3;
        |valp v$8 = v$7 | v$3;
        |vall v$9 = 1;
        |vall v$10 = 1;
        |valp v$11 = v$9 >> v$10;
        |halt(v$11)""".stripMargin)

  @Test def testValueReprShiftLeft =
    testCPSLowTreeEquality("2 << 1",
      """vall v$1 = 5;
        |vall v$2 = 3;
        |vall v$3 = 1;
        |valp v$4 = v$1 >> v$3;
        |valp v$5 = v$2 >> v$3;
        |valp v$6 = v$4 << v$5;
        |valp v$7 = v$6 << v$3;
        |valp v$8 = v$7 | v$3;
        |vall v$9 = 1;
        |vall v$10 = 1;
        |valp v$11 = v$9 >> v$10;
        |halt(v$11)""".stripMargin)

  @Test def testValueReprShiftRight =
    testCPSLowTreeEquality("1 >> 2",
      """vall v$1 = 3;
        |vall v$2 = 5;
        |vall v$3 = 1;
        |valp v$4 = v$1 >> v$3;
        |valp v$5 = v$2 >> v$3;
        |valp v$6 = v$4 >> v$5;
        |valp v$7 = v$6 << v$3;
        |valp v$8 = v$7 | v$3;
        |vall v$9 = 1;
        |vall v$10 = 1;
        |valp v$11 = v$9 >> v$10;
        |halt(v$11)""".stripMargin)

  @Test def testValueReprAnd =
    testCPSLowTreeEquality("1 & 3",
      """vall v$1 = 3;
        |vall v$2 = 7;
        |valp v$3 = v$1 & v$2;
        |vall v$4 = 1;
        |vall v$5 = 1;
        |valp v$6 = v$4 >> v$5;
        |halt(v$6)""".stripMargin)

  @Test def testValueReprOr =
    testCPSLowTreeEquality("1 | 0",
      """vall v$1 = 3;
        |vall v$2 = 1;
        |valp v$3 = v$1 | v$2;
        |vall v$4 = 1;
        |vall v$5 = 1;
        |valp v$6 = v$4 >> v$5;
        |halt(v$6)""".stripMargin)

  @Test def testValueReprXor =
    testCPSLowTreeEquality("1 ^ 3",
      """vall v$1 = 3;
        |vall v$2 = 7;
        |valp v$3 = v$1 ^ v$2;
        |vall v$4 = 1;
        |valp v$5 = v$3 + v$4;
        |vall v$6 = 1;
        |vall v$7 = 1;
        |valp v$8 = v$6 >> v$7;
        |halt(v$8)""".stripMargin)

  @Test def testValueReprCharToInt =
    testCPSLowTreeEquality("'a'.toInt",
      """vall v$1 = 782;
        |vall v$2 = 2;
        |valp v$3 = v$1 >> v$2;
        |vall v$4 = 1;
        |vall v$5 = 1;
        |valp v$6 = v$4 >> v$5;
        |halt(v$6)""".stripMargin)
}
