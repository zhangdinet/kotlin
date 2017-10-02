package kt17514

fun <T> checkSucceeds(callable: () -> T) = callable()

fun main(args: Array<String>) {
    val isPresent = true
    val expectedLibs = listOf("aa", "bb", "cc")
    expectedLibs
            .forEach {
                checkSucceeds {
                    //Breakpoint!
                    val v = isPresent
                }
            }
}

// EXPRESSION: isPresent
// RESULT: 1: Z