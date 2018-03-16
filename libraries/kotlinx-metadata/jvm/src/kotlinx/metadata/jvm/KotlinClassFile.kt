/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.ClassVisitor
import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.LambdaVisitor
import kotlinx.metadata.PackageVisitor
import kotlinx.metadata.impl.ClassWriter
import kotlinx.metadata.impl.LambdaWriter
import kotlinx.metadata.impl.PackageWriter
import kotlinx.metadata.impl.accept
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
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

        class Writer : ClassWriter() {
            fun write(
                metadataVersion: IntArray = KotlinMetadata.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinMetadata.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassFile.Class {
                val (d1, d2) = JvmProtoBufUtil.writeData(t.build(), c.strings as JvmStringTable)
                val metadata = KotlinMetadata(KotlinMetadata.CLASS_KIND, metadataVersion, bytecodeVersion, d1, d2, "", "", extraInt)
                return KotlinClassFile.Class(metadata)
            }
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

        class Writer : PackageWriter() {
            fun write(
                metadataVersion: IntArray = KotlinMetadata.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinMetadata.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassFile.FileFacade {
                val (d1, d2) = JvmProtoBufUtil.writeData(t.build(), c.strings as JvmStringTable)
                val metadata = KotlinMetadata(KotlinMetadata.FILE_FACADE_KIND, metadataVersion, bytecodeVersion, d1, d2, "", "", extraInt)
                return KotlinClassFile.FileFacade(metadata)
            }
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

        class Writer : LambdaWriter() {
            fun write(
                metadataVersion: IntArray = KotlinMetadata.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinMetadata.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassFile.Lambda {
                val proto = t?.build() ?: error("LambdaVisitor.visitFunction has not been called")
                val (d1, d2) = JvmProtoBufUtil.writeData(proto, c.strings as JvmStringTable)
                val metadata = KotlinMetadata(
                    KotlinMetadata.SYNTHETIC_CLASS_KIND, metadataVersion, bytecodeVersion, d1, d2, "", "", extraInt
                )
                return KotlinClassFile.Lambda(metadata)
            }
        }
    }

    class SyntheticClass internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        class Writer {
            fun write(
                metadataVersion: IntArray = KotlinMetadata.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinMetadata.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassFile.SyntheticClass {
                val metadata = KotlinMetadata(
                    KotlinMetadata.SYNTHETIC_CLASS_KIND, metadataVersion, bytecodeVersion, emptyArray(), emptyArray(), "", "", extraInt
                )
                return KotlinClassFile.SyntheticClass(metadata)
            }
        }
    }

    class MultiFileClassFacade internal constructor(metadata: KotlinMetadata) : KotlinClassFile(metadata) {
        val partClassNames: List<String> = metadata.data1.asList()

        class Writer {
            fun write(
                partClassNames: List<String>,
                metadataVersion: IntArray = KotlinMetadata.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinMetadata.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassFile.MultiFileClassFacade {
                val metadata = KotlinMetadata(
                    KotlinMetadata.MULTI_FILE_CLASS_FACADE_KIND, metadataVersion, bytecodeVersion, partClassNames.toTypedArray(),
                    emptyArray(), "", "", extraInt
                )
                return KotlinClassFile.MultiFileClassFacade(metadata)
            }
        }
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

        class Writer : PackageWriter() {
            fun write(
                facadeClassName: String,
                metadataVersion: IntArray = KotlinMetadata.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinMetadata.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassFile.MultiFileClassPart {
                val (d1, d2) = JvmProtoBufUtil.writeData(t.build(), c.strings as JvmStringTable)
                val metadata = KotlinMetadata(
                    KotlinMetadata.MULTI_FILE_CLASS_PART_KIND, metadataVersion, bytecodeVersion, d1, d2, facadeClassName, "", extraInt
                )
                return KotlinClassFile.MultiFileClassPart(metadata)
            }
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
