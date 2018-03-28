// WITH_RUNTIME
// WITH_JDK
// PROBLEM: none
import java.net.Socket

fun main(args: Array<String>) {
    val s = Socket()
    val stream = s.getInputStream<caret>()
}