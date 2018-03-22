import kotlin.*
import kotlin.collections.*

abstract class MyStringList : List<String>

fun List<String>.convert(): MyStringList = this as MyStringList