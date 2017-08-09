// IGNORE_IF_NEW_INFERENCE_ENABLED

fun foo() {
    var v: String? = null
    v<!UNSAFE_CALL!>.<!>length
    v = "abc"
    <!DEBUG_INFO_SMARTCAST!>v<!>.length
    v = null
    v<!UNSAFE_CALL!>.<!>length
    v = "abc"
    <!DEBUG_INFO_SMARTCAST!>v<!>.length
}
