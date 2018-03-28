// WITH_RUNTIME
// PROBLEM: none
import java.io.File

fun File.foo(absolutePath: String) {
    getAbsolutePath<caret>()
}