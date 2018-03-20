// WITH_RUNTIME
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendHere(ctx: CoroutineContext) = suspendCoroutineOrReturn<String> { x ->
    if (x.context == ctx) x.resume("OK") else x.resume("FAIL")
}

suspend fun mustBeTailCallOld(): String {
    return suspendHere(COROUTINES_PACKAGE.intrinsics.coroutineContext)
}

suspend fun mustBeTailCallNew(): String {
    return suspendHere(COROUTINES_PACKAGE.coroutineContext)
}

suspend fun retrieveCoroutineContext(): CoroutineContext =
        suspendCoroutineOrReturn { cont -> cont.context }

suspend fun notTailCall(): String {
    return suspendHere(retrieveCoroutineContext())
}