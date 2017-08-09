// IGNORE_IF_NEW_INFERENCE_ENABLED

class MyClass(var p: String?)

fun bar(s: String): Int {
    return s.length
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return bar(<!SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>m.p<!>)
}
