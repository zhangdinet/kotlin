fun test1() =
        5 in Math.abs(-1) .. 10

fun test2() =
        5 in 1 .. Math.abs(-10)

fun test3() =
        5L in Math.abs(-1L) .. 10L

fun test4() =
        5L in 1L .. Math.abs(-10L)


fun box(): String {
    if (!test1()) return "Fail 1"
    if (!test2()) return "Fail 2"
    if (!test3()) return "Fail 3"
    if (!test4()) return "Fail 4"

    return "OK"
}