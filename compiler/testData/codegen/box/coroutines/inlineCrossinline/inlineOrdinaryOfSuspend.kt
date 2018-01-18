// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// suspend calls possible inside lambda matching to the parameter
// NB. Added noinline, since inline suspend parameter of ordinary inline function is forbidden.

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline fun test(noinline c: suspend () -> Unit) {
    builder {
        c()
    }
}

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL"
    test {
        res = calculate()
    }
    return res
}
