// !API_VERSION: 1.0
// IGNORE_IF_NEW_INFERENCE_ENABLED

@SinceKotlin("1.1")
open class Foo

class Bar @SinceKotlin("1.1") constructor()

@SinceKotlin("1.0")
class Baz @SinceKotlin("1.1") constructor()


fun t1(): <!API_NOT_AVAILABLE!>Foo<!> = <!UNRESOLVED_REFERENCE!>Foo<!>()

// TODO: do not report API_NOT_AVAILABLE twice
fun t2() = object : <!API_NOT_AVAILABLE!>Foo<!>() {}

fun t3(): Bar? = <!UNRESOLVED_REFERENCE!>Bar<!>()

fun t4(): Baz = <!UNRESOLVED_REFERENCE!>Baz<!>()
