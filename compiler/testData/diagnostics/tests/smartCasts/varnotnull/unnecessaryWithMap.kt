// IGNORE_IF_NEW_INFERENCE_ENABLED

fun create(): Map<String, String> = null!!

operator fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> = null!!

operator fun <K, V> Map.Entry<K, V>.component1() = key

operator fun <K, V> Map.Entry<K, V>.component2() = value

class MyClass {
    private var m: Map<String, String>? = null
    fun foo(): Int {
        var res = 0
        m = create()
        // See KT-7428
        for ((k, v) in <!ITERATOR_MISSING!>m<!>)
            res <!NONE_APPLICABLE!>+=<!> (k.<!UNRESOLVED_REFERENCE!>length<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> v.<!UNRESOLVED_REFERENCE!>length<!>)
        return res
    }
}
