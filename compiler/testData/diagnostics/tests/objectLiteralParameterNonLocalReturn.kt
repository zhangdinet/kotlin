// !DIAGNOSTICS: -UNREACHABLE_CODE
// SKIP_TXT

open class Bar(val x: Int) {}
open class Baz(val bar: Bar) {}

fun test() {
    object : Bar(<!RETURN_NOT_ALLOWED!>return<!>) {}
    object : Baz(object : Bar(<!RETURN_NOT_ALLOWED, RETURN_TYPE_MISMATCH!>return<!>) {}) {}
}
