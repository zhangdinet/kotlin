// !LANGUAGE: +ReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun nullWhenNull(x: Int?): Int? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return x?.inc()
}

fun testA(x: Int?) {
    if (nullWhenNull(x) == null) {
        x<!UNSAFE_CALL!>.<!>dec()
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }
}

fun testB(x: Int?) {
    if (nullWhenNull(x) != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }
    else {
        x<!UNSAFE_CALL!>.<!>dec()
    }
}

// NB. it is the same function as `nullWhenNull`, but annotations specifies other facet of the function behaviour
fun notNullWhenNotNull(x: Int?): Int? {
    contract {
        returns(null) implies (x == null)
    }
    return x?.inc()
}

fun testC(x: Int?) {
    if (notNullWhenNotNull(x) == null) {
        <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>
    }
    else {
        x<!UNSAFE_CALL!>.<!>dec()
    }
}

fun testD(x: Int?) {
    if (notNullWhenNotNull(x) != null) {
        x<!UNSAFE_CALL!>.<!>dec()
    }
    else {
        <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>
    }
}