// TREAT_AS_ONE_FILE

data class A<T, F>(val x: T, val y: F)

suspend fun <X, Y> foo(a: A<X, Y>, block: suspend (A<X, Y>) -> String) = block(a)

suspend fun test() = foo(A("OK", 1)) { (x_param, y_param) -> x_param + (y_param.toString()) }

// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param I