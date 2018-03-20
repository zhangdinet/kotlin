// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
import helpers.*
// TREAT_AS_ONE_FILE
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
suspend fun suspendHere() = ""

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {

    for (i in 1..3) {
        builder {
            if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 1")
        }
    }

    return "OK"
}

// 2 GETSTATIC kotlin/Unit.INSTANCE
// 1 GETSTATIC helpers/EmptyContinuation.Companion
// 3 GETSTATIC kotlin\/coroutines\/experimental\/EmptyCoroutineContext.INSTANCE
// 6 GETSTATIC
