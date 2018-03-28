// TREAT_AS_ONE_FILE

data class A(val x: String, val y: String)

suspend fun foo(a: A, block: suspend (Int, A, String) -> String): String = block(1, a, "#")

suspend fun test() = foo(A("O", "K")) { i_param, (x_param, y_param), v_param -> i_param.toString() + x_param + y_param + v_param }

// i_param and v_param appear in both create and doResume

// 2 LOCALVARIABLE i_param I
// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param Ljava/lang/String;
// 2 LOCALVARIABLE v_param Ljava/lang/String;
