class A {
    private companion object {
        var z = 0

        fun test2() {
            z++
            ++z
        }
    }

    fun test1(): Int {
        test2()
        z++
        return ++z
    }

}

fun box(): String {
    val test1 = A().test1()
    return if (test1 == 4) "OK" else "fail $test1"
}