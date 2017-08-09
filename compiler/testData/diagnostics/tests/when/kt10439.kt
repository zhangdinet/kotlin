// IGNORE_IF_NEW_INFERENCE_ENABLED

fun foo(x: Int) = x

fun test0(flag: Boolean) {
    foo(<!TYPE_MISMATCH!>if (flag) true else ""<!>)
}

fun test1(flag: Boolean) {
    foo(when (flag) {
        true -> true
        else -> ""
    })
}
