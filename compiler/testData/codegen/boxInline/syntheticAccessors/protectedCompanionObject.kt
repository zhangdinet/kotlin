// FILE: 1.kt
package util

inline fun myRun(fn: () -> String) = fn()

fun myRunNoInline(fn: () -> String) = fn()

open class Base {
    protected companion object {
        val o = "O"
        val k = "K"
    }
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import util.*

class Derived : Base() {
    fun test() = myRun { o } + myRunNoInline { k }
}

fun box() = Derived().test()