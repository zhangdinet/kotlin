// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// Block is allowed to be called inside the body of owner inline function
// suspend calls possible inside lambda matching to the parameter

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline fun test(c: () -> Unit) {
    c()
}

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL"
    builder {
        test {
            res = calculate()
        }
    }
    return res
}
