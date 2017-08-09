// !DIAGNOSTICS: -USELESS_ELVIS
// IGNORE_IF_NEW_INFERENCE_ENABLED

fun test() {
    bar(<!TYPE_MISMATCH!>if (true) {
        1
    } else {
        2
    }<!>)

    bar(<!TYPE_MISMATCH!>1 ?: 2<!>)
}

fun bar(s: String) = s
