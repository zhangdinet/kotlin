@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")

package kotlin.collections


/**
 * Returns an immutable set containing only the specified object [element].
 * The returned set is serializable.
 */
@JvmVersion
public fun <T> setOf(element: T): Set<T> = java.util.Collections.singleton(element)


/**
 * Returns a new [SortedSet] with the given elements.
 */
@JvmVersion
public fun <T> sortedSetOf(vararg elements: T): java.util.TreeSet<T> = elements.toCollection(java.util.TreeSet<T>())

/**
 * Returns a new [SortedSet] with the given [comparator] and elements.
 */
@JvmVersion
public fun <T> sortedSetOf(comparator: Comparator<in T>, vararg elements: T): java.util.TreeSet<T> = elements.toCollection(java.util.TreeSet<T>(comparator))

