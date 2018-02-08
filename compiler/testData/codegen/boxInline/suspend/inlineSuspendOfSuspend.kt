// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

// FILE: test.kt
import helpers.*
import kotlin.coroutines.experimental.*

// Block is allowed to be called inside the body of owner inline function
// suspend calls possible inside lambda matching to the parameter

suspend inline fun test(c: suspend () -> Unit) {
    c()
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
        test {
            res = calculate()
        }
    }
    if (res != "OK") return res
    res = "FAIL 2"
    builder {
        test {
            test {
                test {
                    test {
                        test {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    return res
}
