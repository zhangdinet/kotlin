// WITH_RUNTIME
import kotlin.test.assertEquals

fun box(): String {
    testForEachInIntRange()
    testForEachInLongRange()
    testForEachWithReturnEmulatingBreak()
    testForEachWithReturnEmulatingContinue()
    testNestedForEachInRange()
    testNestedForEachInRangeWithBreak1()
    testNestedForEachInRangeWithBreak2()
    testNestedForEachInRangeWithContinue1()
    testNestedForEachInRangeWithContinue2()

    return "OK"
}

private fun testForEachInIntRange() {
    var sum = 0
    (1..5).forEach { sum += it }
    assertEquals(15, sum)
}

private fun testForEachInLongRange() {
    var sumL = 0L
    (1L..5L).forEach { sumL += it }
    assertEquals(15L, sumL)
}

private fun testForEachWithReturnEmulatingBreak() {
    var sum = 0
    run BREAK@{
        (1..5).forEach {
            if (it == 3) return@BREAK
            sum += it
        }
    }
    assertEquals(3, sum)
}

private fun testForEachWithReturnEmulatingContinue() {
    var sum = 0
    (1..5).forEach CONTINUE@{
        if (it == 3) return@CONTINUE
        sum += it
    }
    assertEquals(12, sum)
}

private fun testNestedForEachInRange() {
    var sum = 0
    (1..4).forEach { i ->
        (1..4).forEach { j ->
            sum += i*j
        }
    }
    // 1*1 + 1*2 + ... + 4*3 + 4*4 = (1+2+3+4)*(1+2+3+4) = 100
    assertEquals(100, sum)
}

private fun testNestedForEachInRangeWithBreak1() {
    var sum = 0
    (1..4).forEach { i ->
        run BREAK@{
            (1..4).forEach { j ->
                if (j == 3) return@BREAK
                sum += i * j
            }
        }
    }
    assertEquals(30, sum)
}

private fun testNestedForEachInRangeWithBreak2() {
    var sum = 0
    run BREAK@ {
        (1..4).forEach { i ->
            (1..4).forEach { j ->
                if (j == 3) return@BREAK
                sum += i * j
            }
        }
    }
    assertEquals(3, sum)
}

private fun testNestedForEachInRangeWithContinue1() {
    var sum = 0
    (1..4).forEach { i ->
        (1..4).forEach CONTINUE@{ j ->
            if (j == 3) return@CONTINUE
            sum += i * j
        }
    }
    assertEquals(70, sum)
}

private fun testNestedForEachInRangeWithContinue2() {
    var sum = 0
    (1..4).forEach CONTINUE@{ i ->
        (1..4).forEach { j ->
            if (j == 4) return@CONTINUE
            sum += i * j
        }
    }
    assertEquals(60, sum)
}