// IGNORE_IF_NEW_INFERENCE_ENABLED

package f

fun <R> h(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>r<!>: R, <!UNUSED_PARAMETER!>f<!>: (Boolean) -> Int) = 1
fun <R> h(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>r<!>: R, <!UNUSED_PARAMETER!>f<!>: (Boolean) -> Int) = 1

fun test() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>h<!>(1, 1, 1, { b -> <!UNUSED_EXPRESSION!>42<!> })
