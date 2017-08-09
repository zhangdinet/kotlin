// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_IF_NEW_INFERENCE_ENABLED

object X1
object X2

fun <T1> foo(x: T1, f: (T1) -> T1) = X1
fun <T2> foo(xf: () -> T2, f: (T2) -> T2) = X2

val test: X2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>({ <!UNUSED_EXPRESSION!>0<!> }, { it -> <!DEBUG_INFO_MISSING_UNRESOLVED!>it<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1 })
