// IGNORE_IF_NEW_INFERENCE_ENABLED

object Host {
    val `____` = { -> }
    fun testFunTypeVal() {
        <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>()
    }
}
