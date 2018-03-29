// EXPECTED_REACHABLE_NODES: 1114
package foo

open class Base(val ll: Int) {

    val bs1: String?
    val bs2: String

    val llx = ll

    init {
        bs1 = "BaseString1"
        bs2 = "OK"
    }

}

class Test(val p1: Int, val p2:Long, val v1:String, val v2:Test?, v3:Int) : Base(v3) {

    val s1: String
    var s2: String?

    val p3: Int = 3333

    constructor(p: Int) : this(p, 6666, "ctor1", null, 22) {
//        p1 = p
//        p2 = 6666
    }

    constructor(p: Int, pp: Long) : this(p, pp,"ctor2", null, 33) {
//        p1 = p
//        p2 = pp
    }

    constructor(p:Int, pp: Int, ppp: Long) : this(p * pp, ppp)

    init {
        s1 = "String1"
        s2 = null
    }

    fun foo(): String { return "OK" }
    fun bar(): String { return bs2 }
}


//class Test2 : Base(3333) {
//
//    val s1:String
//    val s2:String?
//
//    val p1: Int = 1
//    val p2: Long =2
//
//    init {
//        s1 = "String1"
//        s2 = null
//    }
//
//    fun foo() = "OK"
//}

fun box(): String {
//    val test = Test2()
//    return test.foo()

    var test = Test(1, 2)
    return test.bar()
}