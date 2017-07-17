// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_IF_NEW_INFERENCE_ENABLED
class X<T>(val t: T) {
    constructor(t: T, i: Int) : this(<!TYPE_MISMATCH!>i<!>)
}