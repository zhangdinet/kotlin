// IGNORE_IF_NEW_INFERENCE_ENABLED
//!DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(i: Int, t: T) {}
fun <T> foo(s: String, t: T) {}

fun bar(i: Int) {}
fun bar(s: String) {}

fun test() {
    foo(<!UNRESOLVED_REFERENCE!>rrr<!>, 1)
    bar(<!UNRESOLVED_REFERENCE!>rrr<!>)
}
