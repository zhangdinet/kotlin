// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_IF_NEW_INFERENCE_ENABLED
class A {
    constructor(x: Int) {}
    constructor(x: String) {}
    constructor(): this(<!TOO_MANY_ARGUMENTS!>'a'<!>) {}
}
