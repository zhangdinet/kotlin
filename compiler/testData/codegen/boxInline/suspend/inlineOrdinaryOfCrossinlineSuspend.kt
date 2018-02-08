// WITH_RUNTIME
// WITH_COROUTINES

// FILE: test.kt
import helpers.*
import kotlin.coroutines.experimental.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter

inline fun test1(crossinline runner: suspend () -> Unit)  {
    val l : suspend () -> Unit = { runner() }
    builder { l() }
}

// FILE: box.kt
import helpers.*
import kotlin.coroutines.experimental.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box(): String {
    var res = "FAIL 1"
    test1 {
        res = calculate()
    }
    return res
}
