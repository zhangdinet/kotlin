// TREAT_AS_ONE_FILE

class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

suspend fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A()) { (x_param, _, y_param) -> x_param + y_param }

// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param Ljava/lang/String;