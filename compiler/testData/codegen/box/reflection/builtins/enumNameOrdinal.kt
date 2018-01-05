// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

enum class E { X, Y, Z }

fun box(): String {
    E::class.members
    assertEquals("Y", E::name.call(E.Y))
    assertEquals(2, E::ordinal.call(E.Z))
    return "OK"
}
