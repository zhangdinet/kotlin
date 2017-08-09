// IGNORE_IF_NEW_INFERENCE_ENABLED

package f

fun <T> f(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>c<!>: Collection<T>): List<T> {throw Exception()}
fun <T> f(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>l<!>: List<T>): Collection<T> {throw Exception()}

fun <T> test(<!UNUSED_PARAMETER!>l<!>: List<T>) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>(1, emptyList())
}

fun <T> emptyList(): List<T> {throw Exception()}
