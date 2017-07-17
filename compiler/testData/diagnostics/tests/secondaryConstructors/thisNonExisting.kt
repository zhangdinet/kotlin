// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_IF_NEW_INFERENCE_ENABLED
class A {
    constructor(x: Int) {}
    constructor(x: String) {}
    constructor(): <!NONE_APPLICABLE!>this<!>('a') {}
}
