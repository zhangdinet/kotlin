class A(val prop: Int)

fun A.ext() = prop

class B {
    fun some() = A(1).ext()
}

B().some()