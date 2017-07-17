// IGNORE_IF_NEW_INFERENCE_ENABLED

// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.doSomething(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p<!> = 1) {
        bar()
    }
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass {
    public void doSomething(int p, Runnable runnable) { runnable.run(); }
}