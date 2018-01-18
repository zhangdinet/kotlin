// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline fun test1(crossinline runner: suspend () -> Unit)  {
    val l : suspend () -> Unit = { runner() }
    builder { l() }
}

suspend fun calculate() = "OK"

fun box(): String {
    var res = "FAIL 1"
    test1 {
        res = calculate()
    }
    return res
}
