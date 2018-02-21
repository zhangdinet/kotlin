// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// SKIP_TXT

open class Bar {}

suspend fun ok() = "OK"
fun builder(c: suspend () -> Unit) {}

fun test() {
    builder {
        object : Bar() {
            val ok = <!NON_LOCAL_SUSPENSION_POINT!>ok<!>()
        }
    }
}
