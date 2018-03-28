// TREAT_AS_ONE_FILE

data class A(val x: String, val y: String)

suspend inline fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A("O", "K")) { (x_param, y_param) -> x_param + y_param }

// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param Ljava/lang/String;