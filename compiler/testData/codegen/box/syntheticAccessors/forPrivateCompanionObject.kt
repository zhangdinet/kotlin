class C {
    fun test() = runNoInline { ok() }

    private companion object {
        fun ok() = "OK"
    }
}

fun runNoInline(fn: () -> String) = fn()

fun box() = C().test()