// !WITH_NEW_INFERENCE
class A {
    operator fun component1() : Int = 1
    operator fun component2() : Int = 2
}

fun a(aa : A) {
    val (<!NI;TYPE_MISMATCH!><!UNUSED_VARIABLE!>a<!>: String<!>, <!NI;TYPE_MISMATCH!><!UNUSED_VARIABLE!>b1<!>: String<!>) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>aa<!>
}
