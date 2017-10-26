enum class Foo(
        val x: String,
        val callback: () -> String
) {
    FOO(
            "OK",
            object : () -> String {
                override fun invoke() = FOO.x
            }
    )
}

fun box() = Foo.FOO.callback()