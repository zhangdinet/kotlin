// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun foo() = 42
open class Bar(val x: Int) {}
open class Baz(val bar: Bar) {}

fun builder(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        val bar = object : Bar(foo()) {}
        if (bar.x != 42) throw RuntimeException("FAIL")
        val baz = object : Baz(object : Bar(foo()) {}) {}
        if (baz.bar.x != 42) throw RuntimeException("FAIL")
    }
    return "OK"
}
