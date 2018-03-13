// !LANGUAGE: +JvmFieldInInterface
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_REFLECT
import kotlin.reflect.full.declaredMemberProperties

public class Bar(public val value: String)

class Foo {

    companion object {
        @JvmField
        val z = Bar("OK")
    }
}


fun box(): String {
    val field = Foo.Companion::class.declaredMemberProperties.single()
    return (field.get(Foo.Companion) as Bar).value
}
