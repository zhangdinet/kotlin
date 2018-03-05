/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.ClassVisitor
import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.LambdaVisitor
import kotlinx.metadata.PackageVisitor
import kotlinx.metadata.impl.accept
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import kotlin.LazyThreadSafetyMode.PUBLICATION

sealed class KotlinClassFile(val metadata: KotlinMetadata) {
    class Class internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        private val classData by lazy(PUBLICATION) {
            val data1 = (metadata.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("No d1 in metadata"))
            JvmProtoBufUtil.readClassDataFrom(data1, metadata.data2)
        }

        fun accept(v: ClassVisitor) {
            val (strings, proto) = classData
            proto.accept(v, strings)
        }
    }

    class FileFacade internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        private val packageData by lazy(PUBLICATION) {
            val data1 = (metadata.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("No d1 in metadata"))
            JvmProtoBufUtil.readPackageDataFrom(data1, metadata.data2)
        }

        fun accept(v: PackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }
    }

    class Lambda internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        private val functionData by lazy(PUBLICATION) {
            val data1 = (metadata.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("No d1 in metadata"))
            JvmProtoBufUtil.readFunctionDataFrom(data1, metadata.data2)
        }

        fun accept(v: LambdaVisitor) {
            val (strings, proto) = functionData
            proto.accept(v, strings)
        }
    }

    class SyntheticClass internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata)

    class MultiFileClassFacade internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        val partClassNames: List<String> = metadata.data1.asList()
    }

    class MultiFileClassPart internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        private val packageData by lazy(PUBLICATION) {
            val data1 = (metadata.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("No d1 in metadata"))
            JvmProtoBufUtil.readPackageDataFrom(data1, metadata.data2)
        }

        val facadeClassName: String
            get() = metadata.extraString

        fun accept(v: PackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }
    }

    class Unknown internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata)

    companion object {
        /**
         * Reads and parses the given Kotlin metadata and returns the correct type of [KotlinClassFile] encoded by this metadata,
         * or `null` if this metadata encodes an unsupported kind of Kotlin classes or has an unsupported metadata version.
         *
         * Throws [InconsistentKotlinMetadataException] if the metadata has inconsistencies which signal that it may have been
         * modified by a separate tool.
         */
        @JvmStatic
        fun read(metadata: KotlinMetadata): KotlinClassFile? {
            // We only support metadata of version 1.1.* (this is Kotlin from 1.0 until today)
            val version = metadata.metadataVersion
            if (version.size < 2 || version[0] != 1 || version[1] != 1) return null

            return try {
                when (metadata.kind) {
                    KotlinMetadata.CLASS_KIND -> KotlinClassFile.Class(metadata)
                    KotlinMetadata.FILE_FACADE_KIND -> KotlinClassFile.FileFacade(metadata)
                    KotlinMetadata.SYNTHETIC_CLASS_KIND -> {
                        if (metadata.data1.isNotEmpty()) KotlinClassFile.Lambda(metadata) else KotlinClassFile.SyntheticClass(metadata)
                    }
                    KotlinMetadata.MULTI_FILE_CLASS_FACADE_KIND -> KotlinClassFile.MultiFileClassFacade(metadata)
                    KotlinMetadata.MULTI_FILE_CLASS_PART_KIND -> KotlinClassFile.MultiFileClassPart(metadata)
                    else -> KotlinClassFile.Unknown(metadata)
                }
            } catch (e: InconsistentKotlinMetadataException) {
                throw e
            } catch (e: Throwable) {
                throw InconsistentKotlinMetadataException("Exception occurred when reading Kotlin metadata", e)
            }
        }
    }
}
