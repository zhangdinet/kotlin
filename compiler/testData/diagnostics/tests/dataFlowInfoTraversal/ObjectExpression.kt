fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    val <!UNUSED_VARIABLE!>a<!> = object {
        fun baz() = bar(if (x == null) 0 else <!DEBUG_INFO_SMARTCAST!>x<!>)
        fun quux(): Int = <!TYPE_MISMATCH!>if (x == null) <!DEBUG_INFO_CONSTANT!>x<!> else <!DEBUG_INFO_SMARTCAST!>x<!><!>
    }
}
