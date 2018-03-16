/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

class KotlinMetadata(
    val kind: Int,
    val metadataVersion: IntArray,
    val bytecodeVersion: IntArray,
    val data1: Array<String>,
    val data2: Array<String>,
    val extraString: String,
    val packageName: String,
    val extraInt: Int
) {
    companion object {
        const val CLASS_KIND = 1
        const val FILE_FACADE_KIND = 2
        const val SYNTHETIC_CLASS_KIND = 3
        const val MULTI_FILE_CLASS_FACADE_KIND = 4
        const val MULTI_FILE_CLASS_PART_KIND = 5

        @JvmField
        val COMPATIBLE_METADATA_VERSION = intArrayOf(1, 1, 10)

        @JvmField
        val COMPATIBLE_BYTECODE_VERSION = intArrayOf(1, 0, 2)
    }
}
