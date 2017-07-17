// !DIAGNOSTICS: -USELESS_ELVIS
// IGNORE_IF_NEW_INFERENCE_ENABLED

fun test() {
    bar(if (true) {
        <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
    } else {
        <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>
    })

    bar(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> ?: <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
}

fun bar(s: String) = s