// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)

suspend inline fun test1(noinline c: () -> Unit) {
    c()
}

suspend inline fun test2(noinline c: () -> Unit) {
    val l = { c() }
    l()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var res = "FAIL 1"
    builder {
        test1 {
            res = "OK"
        }
    }
    if (res != "OK") return res

    res = "FAIL 2"
    builder {
        test2 {
            res = "OK"
        }
    }
    return res
}
