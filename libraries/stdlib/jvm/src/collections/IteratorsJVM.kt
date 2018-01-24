@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

/**
 * Creates an [Iterator] for an [java.util.Enumeration], allowing to use it in `for` loops.
 * @sample samples.collections.Iterators.iteratorForEnumeration
 */
@kotlin.jvm.JvmVersion
public operator fun <T> java.util.Enumeration<T>.iterator(): Iterator<T> = object : Iterator<T> {
    override fun hasNext(): Boolean = hasMoreElements()

    public override fun next(): T = nextElement()
}

