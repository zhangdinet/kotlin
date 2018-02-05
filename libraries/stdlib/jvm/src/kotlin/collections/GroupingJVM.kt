package kotlin.collections


/**
 * Groups elements from the [Grouping] source by key and counts elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of elements in the group.
 *
 * @sample samples.collections.Collections.Transformations.groupingByEachCount
 */
@SinceKotlin("1.1")
@JvmVersion
public actual fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> =
        // fold(0) { acc, e -> acc + 1 } optimized for boxing
        foldTo( destination = mutableMapOf(),
                initialValueSelector = { _, _ -> kotlin.jvm.internal.Ref.IntRef() },
                operation = { _, acc, _ -> acc.apply { element += 1 } })
        .mapValuesInPlace { it.value.element }


/**
/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group.
 *
 * @return a [Map] associating the key of each group with the sum of elements in the group.
 */
@SinceKotlin("1.1")
@JvmVersion
public inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int> =
        // fold(0) { acc, e -> acc + valueSelector(e)} optimized for boxing
        foldTo( destination = mutableMapOf(),
                initialValueSelector = { _, _ -> kotlin.jvm.internal.Ref.IntRef() },
                operation = { _, acc, e -> acc.apply { element += valueSelector(e) } })
        .mapValuesInPlace { it.value.element }

/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group
 * to the given [destination] map.
 *
 *
 * If the [destination] map already has a value corresponding to the key of some group,
 * that value is used as an initial value of the sum for that group.
 *
 * @return the [destination] map associating the key of each group with the sum of elements in the group.
 */
@SinceKotlin("1.1")
public inline fun <T, K, M : MutableMap<in K, Int>> Grouping<T, K>.eachSumOfTo(destination: M, valueSelector: (T) -> Int): M =
        foldTo(destination, 0) { acc, e -> acc + valueSelector(e)}
*/

@JvmVersion
@PublishedApi
@kotlin.internal.InlineOnly
@Suppress("UNCHECKED_CAST") // tricks with erased generics go here, do not repeat at reified platforms
internal inline fun <K, V, R> MutableMap<K, V>.mapValuesInPlace(f: (Map.Entry<K, V>) -> R): MutableMap<K, R> {
    entries.forEach {
        (it as MutableMap.MutableEntry<K, R>).setValue(f(it))
    }
    return (this as MutableMap<K, R>)
}

