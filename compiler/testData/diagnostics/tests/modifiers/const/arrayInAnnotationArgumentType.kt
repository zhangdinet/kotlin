// IGNORE_IF_NEW_INFERENCE_ENABLED

annotation class A(val a: IntArray = <!TYPE_MISMATCH!>arrayOf(1)<!>)
annotation class B(val a: IntArray = intArrayOf(1))
