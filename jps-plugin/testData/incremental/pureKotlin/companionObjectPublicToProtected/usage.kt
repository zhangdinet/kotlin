package test.usage

import test.*

fun test(c: C) = c.lambda.invoke()

class D : C() {
    fun test() = bar()
}