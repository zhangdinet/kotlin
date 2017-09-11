// LANGUAGE_VERSION: 1.2

enum class X {
    B {

        override val value = "OK"

        val bmr = ::value.get()

        override fun foo(): String {
            return bmr
        }
    };

    abstract val value: String

    abstract fun foo(): String
}

fun box() = X.B.foo()