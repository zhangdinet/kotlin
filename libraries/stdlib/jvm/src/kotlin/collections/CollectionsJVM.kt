/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

import kotlin.comparisons.compareValues





/**
 * Returns an immutable list containing only the specified object [element].
 * The returned list is serializable.
 * @sample samples.collections.Collections.Lists.singletonReadOnlyList
 */
@JvmVersion
public fun <T> listOf(element: T): List<T> = java.util.Collections.singletonList(element)

/**
 * Returns a list containing the elements returned by this enumeration
 * in the order they are returned by the enumeration.
 * @sample samples.collections.Collections.Lists.listFromEnumeration
 */
@JvmVersion
@kotlin.internal.InlineOnly
public inline fun <T> java.util.Enumeration<T>.toList(): List<T> = java.util.Collections.list(this)

@JvmVersion
@kotlin.internal.InlineOnly
internal actual inline fun copyToArrayImpl(collection: Collection<*>): Array<Any?> =
        kotlin.jvm.internal.CollectionToArray.toArray(collection)

@JvmVersion
@kotlin.internal.InlineOnly
internal actual inline fun <T> copyToArrayImpl(collection: Collection<*>, array: Array<T>): Array<T> =
        kotlin.jvm.internal.CollectionToArray.toArray(collection, array)

// copies typed varargs array to array of objects
@JvmVersion
internal actual fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> =
        if (isVarargs && this.javaClass == Array<Any?>::class.java)
            // if the array came from varargs and already is array of Any, copying isn't required
            @Suppress("UNCHECKED_CAST") (this as Array<Any?>)
        else
            java.util.Arrays.copyOf(this, this.size, Array<Any?>::class.java)


