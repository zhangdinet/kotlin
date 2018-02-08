// WITH_RUNTIME
// WITH_COROUTINES

// FILE: test.kt
import helpers.*
import kotlin.coroutines.experimental.*

// suspend calls possible inside lambda matching to the parameter
// NB. Added noinline, since inline suspend parameter of ordinary inline function is forbidden.

inline fun test(noinline c: suspend () -> Unit) {
    builder {
        c()
    }
}

// FILE: box.kt

import helpers.*
import kotlin.coroutines.experimental.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL"
    test {
        res = calculate()
    }
    return res
}
