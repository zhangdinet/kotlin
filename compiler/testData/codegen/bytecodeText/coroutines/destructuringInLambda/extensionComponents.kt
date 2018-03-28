// TREAT_AS_ONE_FILE

class A<T>(val x: String, val y: String, val z: T)

suspend fun <T> foo(a: A<T>, block: suspend (A<T>) -> String): String = block(a)

operator fun A<*>.component1() = x

object B {
    operator fun A<*>.component2() = y
}

suspend fun B.bar(): String {
    operator fun <R> A<R>.component3() = z

    return foo(A("O", "K", 123)) { (x_param, y_param, z_param) -> x_param + y_param + z_param.toString() }
}

suspend fun test() = B.bar()

// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param Ljava/lang/String;
// 1 LOCALVARIABLE z_param I
