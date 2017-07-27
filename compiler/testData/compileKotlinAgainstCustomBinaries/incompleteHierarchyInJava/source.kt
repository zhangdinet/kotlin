import b.B

class BSubclass : B()

fun getB(): B? = null
fun takeB(b: B?) {}

fun takeBWithOverload(b: B?) {}
fun takeBWithOverload(b: Any?) {}

fun test() {
    val b: B = getB()!!
    takeB(b)
    takeBWithOverload(b)

    b.bar()

    object : B() {}
}
