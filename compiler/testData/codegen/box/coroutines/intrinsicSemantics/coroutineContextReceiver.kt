// !API_VERSION: 1.2
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import kotlin.test.assertEquals

class Controller {
    suspend fun controllerSuspendHereOld() =
        if (COROUTINES_PACKAGE.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${COROUTINES_PACKAGE.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerMultipleArgsOld(a: Any, b: Any, c: Any) =
        if (COROUTINES_PACKAGE.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${COROUTINES_PACKAGE.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerSuspendHereNew() =
        if (COROUTINES_PACKAGE.coroutineContext != EmptyCoroutineContext)
            "${COROUTINES_PACKAGE.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerMultipleArgsNew(a: Any, b: Any, c: Any) =
        if (COROUTINES_PACKAGE.coroutineContext != EmptyCoroutineContext)
            "${COROUTINES_PACKAGE.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    fun builder(c: suspend Controller.() -> String): String {
        var fromSuspension: String? = null

        c.startCoroutine(this, object : Continuation<String> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWithException(exception: Throwable) {
                fromSuspension = "Exception: " + exception.message!!
            }

            override fun resume(value: String) {
                fromSuspension = value
            }
        })

        return fromSuspension as String
    }
}

fun box(): String {
    val c = Controller()
    var res = c.builder { controllerSuspendHereOld() }
    if (res != "OK") {
        return "fail 1 $res"
    }
    res = c.builder { controllerMultipleArgsOld(1, 1, 1) }
    if (res != "OK") {
        return "fail 2 $res"
    }
    res = c.builder {
        if (COROUTINES_PACKAGE.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${COROUTINES_PACKAGE.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"
    }
    if (res != "OK") {
        return "fail 3 $res"
    }
    res = c.builder { controllerSuspendHereNew() }
    if (res != "OK") {
        return "fail 4 $res"
    }
    res = c.builder { controllerMultipleArgsNew(1, 1, 1) }
    if (res != "OK") {
        return "fail 5 $res"
    }
    res = c.builder {
        if (COROUTINES_PACKAGE.coroutineContext != EmptyCoroutineContext)
            "${COROUTINES_PACKAGE.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"
    }
    if (res != "OK") {
        return "fail 6 $res"
    }

    return "OK"
}