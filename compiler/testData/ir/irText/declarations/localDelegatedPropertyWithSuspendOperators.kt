// WITH_RUNTIME
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.intrinsics.COROUTINE_SUSPENDED
import COROUTINES_PACKAGE.intrinsics.suspendCoroutineOrReturn
import kotlin.reflect.KProperty

class A {
    var z: Int = 42

    operator suspend fun getValue(thisRef: Any?, property: KProperty<*>) = z

    operator suspend fun setValue(thisRef: Any?, property: KProperty<*>, value: Int): Unit = suspendCoroutineOrReturn { x ->
        z = value
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }

    operator suspend fun provideDelegate(host: Any?, p: Any): A = suspendCoroutineOrReturn { x ->
        x.resume(this)
        COROUTINE_SUSPENDED
    }
}

suspend fun test() {
    val testVal by A()
    var testVar by A()
}