// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Start coroutine call is possible
// Are suspend calls possible inside lambda matching to the parameter

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline fun test1(noinline c: suspend () -> Unit)  {
    val l : suspend () -> Unit = { c() }
    builder { l() }
}

inline fun test2(noinline c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box(): String {
    var res = "FAIL 1"
    test1 {
        res = calculate()
    }
    if (res != "OK") return res
    res = "FAIL 2"
    test2 {
        res = "OK"
    }
    return res
}
