// FILE: 1.kt
package util

inline fun myRun(fn: () -> String) = fn()

fun myRunNoInline(fn: () -> String) = fn()

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import util.*

class C {
    fun test() = myRun { o } + myRunNoInline { k }

    private companion object {
        val o = "O"
        val k = "K"
    }
}

fun box() = C().test()