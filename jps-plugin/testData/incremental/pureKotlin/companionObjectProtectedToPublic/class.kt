package test

open class C {
    val lambda = { foo() }

    protected inline fun bar() = foo()

    protected companion object {
        fun foo() = "OK"
    }
}