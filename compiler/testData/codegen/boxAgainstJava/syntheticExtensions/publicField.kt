// FILE: JavaClass.java

class JavaClass {
    public String str = "OK";

    public String getStr() {
        return "FAIL 0";
    }

    public String getSTR() {
        return "FAIL 1";
    }
}

// FILE: test.kt

fun box(): String {
    return JavaClass().str
}
