// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)

inline fun test1(crossinline c: () -> Unit) {
    c()
}

inline fun test2(crossinline c: () -> Unit) {
    val l = { c() }
    l()
}

fun box() : String {
    var res = "FAIL 1"
    test1 {
        res = "OK"
    }
    if (res != "OK") return res

    res = "FAIL 2"
    test2 {
        res = "OK"
    }
    return res
}
