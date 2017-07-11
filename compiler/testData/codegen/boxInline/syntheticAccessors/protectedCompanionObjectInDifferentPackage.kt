// FILE: 1.kt
package util

open class Base {
    protected companion object {
        val o = "O"
        val k = "K"
    }

    protected inline fun myRun() = o + k
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import util.*

class Derived : Base() {
    fun test() = myRun()
}

fun box() = Derived().test()