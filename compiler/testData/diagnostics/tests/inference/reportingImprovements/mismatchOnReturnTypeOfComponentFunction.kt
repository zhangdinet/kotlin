// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

abstract class A {
    abstract operator fun component1()
    abstract operator fun component2(): Int
}

fun foo(a: A) {
    val (<!NI;TYPE_MISMATCH!>w: Int<!>, x: Int) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>a<!>
}