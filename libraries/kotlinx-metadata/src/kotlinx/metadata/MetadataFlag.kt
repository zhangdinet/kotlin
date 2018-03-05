/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import org.jetbrains.kotlin.metadata.deserialization.Flags.BooleanFlagField
import org.jetbrains.kotlin.metadata.deserialization.Flags.FlagField

class MetadataFlag internal constructor(
    private val offset: Int,
    private val bitWidth: Int,
    private val value: Int
) {
    constructor(field: FlagField<*>, value: Int) : this(field.offset, field.bitWidth, value)

    constructor(field: BooleanFlagField) : this(field, 1)

    operator fun invoke(flags: Int): Boolean =
        (flags ushr offset) and ((1 shl bitWidth) - 1) == value

    operator fun plus(flags: Int): Int =
        (flags and (((1 shl bitWidth) - 1) shl offset).inv()) + (value shl offset)
}

operator fun Int.contains(flag: MetadataFlag): Boolean = flag(this)

operator fun Int.plus(flag: MetadataFlag): Int = flag + this

// Read:
//     Flags.Class.isInterface(flags)
// or
//     Flags.Class.isInterface in flags
//
// Write:
//     flags + Flags.Class.isInterface
// or
//     Flags.Class.isInterface + flags
