// !LANGUAGE: +JvmFieldInInterface
// WITH_RUNTIME
// WITH_REFLECT
import kotlin.reflect.full.memberProperties

class Bar(val value: String)

class Foo {

    companion object {
        @JvmField
        val z = Bar("OK")
    }
}


fun box(): String {
    val field = Foo.Companion::class.memberProperties.single()
    return (field.get(Foo.Companion) as Bar).value
}
