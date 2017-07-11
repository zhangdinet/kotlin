package test

open class C {
    val lambda = { foo() }

    inline fun bar() = foo()

    companion object {
        fun foo() = "OK"
    }
}