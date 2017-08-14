// FILE: 1.kt

package test

class A {

    inline fun a(f: () -> Any): Any {
        try {
            return f()
        } finally {
            val x = 1
            val x2 = 2
            throw java.lang.Exception("ex")
        }
    }

    inline fun b(rule: () -> Unit) {
        try {
            rule()
        } catch (e: Exception) {

        }
    }

    fun c(function: String): Any = a {
        val z = "1"
        val z2 = "2"
        val z3 = "3"
        b { return function }
        throw Throwable(z + z2 + z3)
    }
}


// FILE: 2.kt
import test.*

fun box(): String {
    A().c("O")
    return "OK"
}