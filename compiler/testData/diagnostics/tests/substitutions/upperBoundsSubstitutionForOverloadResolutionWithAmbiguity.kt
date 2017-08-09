// FILE: foo.kt
// IGNORE_IF_NEW_INFERENCE_ENABLED
package foo

fun <T> f(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

// FILE: bar.kt
package bar

fun <T> f(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

// FILE: main.kt

import foo.*
import bar.*

fun <T> test(l: List<T>) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>(l)
}
