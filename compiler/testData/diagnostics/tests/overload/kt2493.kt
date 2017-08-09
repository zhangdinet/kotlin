// IGNORE_IF_NEW_INFERENCE_ENABLED

interface A
interface B

fun <R: A> R.f() {
}

fun <R: B> R.f() {
}

class AImpl: A
class BImpl: B

class C: A, B

fun main(args: Array<String>) {
    AImpl().f()
    BImpl().f()
    C().<!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
}
