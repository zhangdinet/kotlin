// WITH_RUNTIME
// WITH_COROUTINES

// FILE: test.kt
import helpers.*
import kotlin.coroutines.experimental.*

// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// Are suspend calls possible inside lambda matching to the parameter
// Start coroutine call is possible
// Block is allowed to be called directly inside inline function

suspend inline fun test1(noinline c: suspend () -> Unit)  {
    val l : suspend () -> Unit = { c() }
    builder { l() }
}

suspend inline fun test2(noinline c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend inline fun test3(noinline c: suspend () -> Unit) {
    c()
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
    builder {
        test1 {
            res = calculate()
        }
    }
    if (res != "OK") return res
    res = "FAIL 2"
    builder {
        test2 {
            res = "OK"
        }
    }
    if (res != "OK") return res
    res = "FAIL 3"
    builder {
        test3 {
            res = "OK"
        }
    }
    if (res != "OK") return res
    res = "FAIL 4"
    builder {
        test1 {
            test1 {
                test1 {
                    test1 {
                        test1 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    if (res != "OK") return res
    res = "FAIL 5"
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
    if (res != "OK") return res
    res = "FAIL 6"
    builder {
        test3 {
            test3 {
                test3 {
                    test3 {
                        test3 {
                            res = calculate()
                        }
                    }
                }
            }
        }
    }
    if (res != "OK") return res
    res = "FAIL 7"
    builder {
        test1 {
            test2 {
                test3 {
                    test1 {
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
