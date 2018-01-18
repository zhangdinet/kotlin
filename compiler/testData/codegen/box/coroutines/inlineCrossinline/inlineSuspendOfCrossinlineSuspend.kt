// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// suspend calls possible inside lambda matching to the parameter

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend inline fun test1(crossinline c: suspend () -> Unit) {
    c()
}

suspend inline fun test2(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = { c() }
    l()
}

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL 1"
    builder {
        test1 {
            res = calculate()
        }
    }
    if (res != "OK") return res

    res = "FAIL 2"
    builder {
        test2 {
            res = calculate()
        }
    }
    return res
}
