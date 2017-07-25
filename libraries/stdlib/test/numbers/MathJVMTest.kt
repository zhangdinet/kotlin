@file:kotlin.jvm.JvmVersion
package test.numbers

import java.math.BigInteger
import java.math.BigDecimal

import kotlin.test.*
import org.junit.Test
import kotlin.math.*

class MathTest {
    @Test fun testBigInteger() {
        val a = BigInteger("2")
        val b = BigInteger("3")

        assertEquals(BigInteger("5"), a + b)
        assertEquals(BigInteger("-1"), a - b)
        assertEquals(BigInteger("6"), a * b)
        assertEquals(BigInteger("0"), a / b)
        assertEquals(BigInteger("-2"), -a)
        assertEquals(BigInteger("-2"), -a % b)
        assertEquals(BigInteger("1"), (-a).mod(b))
        assertEquals(BigInteger("-2"), (-a).remainder(b))
    }

    @Test fun testBigDecimal() {
        val a = BigDecimal("2")
        val b = BigDecimal("3")

        assertEquals(BigDecimal("5"), a + b)
        assertEquals(BigDecimal("-1"), a - b)
        assertEquals(BigDecimal("6"), a * b)
        assertEquals(BigDecimal("2"), BigDecimal("4") / a)
        assertEquals(BigDecimal("-2"), -a)
        assertEquals(BigDecimal("-2"), -a % b)
        assertEquals(BigDecimal("-2"), (-a).mod(b))
        assertEquals(BigDecimal("-2"), (-a).rem(b))
    }
}

fun main(args: Array<String>) {
    MathTest().testBigInteger()
    MathTest().testBigDecimal()
}


class MathJVMTest {

    @Test fun IEEEremainder() {
        val data = arrayOf(  //  a    a IEEErem 2.5
                doubleArrayOf(-2.0,   0.5),
                doubleArrayOf(-1.25, -1.25),
                doubleArrayOf( 0.0,   0.0),
                doubleArrayOf( 1.0,   1.0),
                doubleArrayOf( 1.25,  1.25),
                doubleArrayOf( 1.5,  -1.0),
                doubleArrayOf( 2.0,  -0.5),
                doubleArrayOf( 2.5,   0.0),
                doubleArrayOf( 3.5,   1.0),
                doubleArrayOf( 3.75, -1.25),
                doubleArrayOf( 4.0,  -1.0)
        )
        for ((a, r) in data) {
            assertEquals(r, a.IEEErem(2.5), "($a).IEEErem(2.5)")
        }

        assertTrue(Double.NaN.IEEErem(2.5).isNaN())
        assertTrue(2.0.IEEErem(Double.NaN).isNaN())
        assertTrue(Double.POSITIVE_INFINITY.IEEErem(2.0).isNaN())
        assertTrue(2.0.IEEErem(0.0).isNaN())
        assertEquals(PI, PI.IEEErem(Double.NEGATIVE_INFINITY))
    }

    @Test fun nextAndPrev() {
        for (value in listOf(0.0, -0.0, Double.MIN_VALUE, -1.0, 2.0.pow(10))) {
            val next = value.nextUp()
            if (next > 0) {
                assertEquals(next, value + value.ulp)
            } else {
                assertEquals(value, next - next.ulp)
            }

            val prev = value.nextDown()
            if (prev > 0) {
                assertEquals(value, prev + prev.ulp)
            }
            else {
                assertEquals(prev, value - value.ulp)
            }

            val toZero = value.nextTowards(0.0)
            if (toZero != 0.0) {
                assertEquals(value, toZero + toZero.ulp.withSign(toZero))
            }

            assertEquals(Double.POSITIVE_INFINITY, Double.MAX_VALUE.nextUp())
            assertEquals(Double.MAX_VALUE, Double.POSITIVE_INFINITY.nextDown())

            assertEquals(Double.NEGATIVE_INFINITY, (-Double.MAX_VALUE).nextDown())
            assertEquals((-Double.MAX_VALUE), Double.NEGATIVE_INFINITY.nextUp())

            assertTrue(Double.NaN.ulp.isNaN())
            assertTrue(Double.NaN.nextDown().isNaN())
            assertTrue(Double.NaN.nextUp().isNaN())
            assertTrue(Double.NaN.nextTowards(0.0).isNaN())
        }
    }
}