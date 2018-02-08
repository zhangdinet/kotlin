// WITH_RUNTIME
// WITH_COROUTINES

// FILE: test.kt
import helpers.*
import kotlin.coroutines.experimental.*

// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// suspend calls possible inside lambda matching to the parameter

suspend inline fun test1(crossinline c: suspend () -> Unit) {
    c()
}

suspend inline fun test2(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = { c() }
    l()
}

// FILE: box.kt
import helpers.*
import kotlin.coroutines.experimental.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() : String {
    var res = "FAIL 1"
    builder {
        test2 {
            test2 {
                test2 {
                    test2 {
                        test2 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    return res
}
