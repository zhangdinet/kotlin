//KT-13330 AssertionError: Illegal resolved call to variable with invoke
// IGNORE_IF_NEW_INFERENCE_ENABLED

fun foo(exec: (String.() -> Unit)?) = "".<!UNSAFE_IMPLICIT_INVOKE_CALL!>exec<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><<!UNRESOLVED_REFERENCE!>caret<!>><!>() // <caret> is test data tag here