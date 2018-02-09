// FILE: test.kt
// WITH_RUNTIME

import kotlin.coroutines.experimental.*

// suspend calls possible inside lambda matching to the parameter
// NB. Added noinline, since inline suspend parameter of ordinary inline function is forbidden.

inline fun test(noinline c: suspend () -> Unit) {
    builder {
        c()
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

// FILE: box.kt

import kotlin.coroutines.experimental.*

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL"
    test {
        res = calculate()
    }
    return res
}
