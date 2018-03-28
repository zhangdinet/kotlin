// TREAT_AS_ONE_FILE

data class Data(val x: String, val y: Int)

suspend fun test() {
    foo(Data("A", 1)) { (x_param, y_param) ->
        "$x_param / $y_param"
    }
}

suspend fun foo(data: Data, body: suspend (Data) -> Unit) {
    body(data)
}

// 1 LOCALVARIABLE \$x_param_y_param LData;
// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param I