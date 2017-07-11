// FILE: 1.kt
package test

class Foo {
    private companion object {
        const val ok = "OK"
    }

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun test() = ok
}

// FILE: 2.kt
import test.*

fun box() = Foo().test()