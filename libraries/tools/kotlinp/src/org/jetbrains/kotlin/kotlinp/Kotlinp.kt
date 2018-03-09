/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.jvm.KotlinClassFile
import java.io.File

object Kotlinp {
    internal fun renderClassFile(classFile: KotlinClassFile?): String =
        when (classFile) {
            is KotlinClassFile.Class -> ClassPrinter().print(classFile)
            is KotlinClassFile.FileFacade -> FileFacadePrinter().print(classFile)
            is KotlinClassFile.Lambda -> LambdaPrinter().print(classFile)
            is KotlinClassFile.SyntheticClass -> buildString { appendln("synthetic class") }
            is KotlinClassFile.MultiFileClassFacade -> MultiFileClassFacadePrinter().print(classFile)
            is KotlinClassFile.MultiFileClassPart -> MultiFileClassPartPrinter().print(classFile)
            is KotlinClassFile.Unknown -> buildString { appendln("unknown file (k=${classFile.metadata.kind})") }
            null -> buildString { appendln("unsupported file") }
        }

    internal fun readClassFile(file: File): KotlinClassFile? {
        val metadata = file.readKotlinMetadata() ?: throw KotlinpException("file is not a Kotlin class file: $file")
        return try {
            KotlinClassFile.read(metadata)
        } catch (e: InconsistentKotlinMetadataException) {
            throw KotlinpException("inconsistent Kotlin metadata: ${e.message}")
        }
    }
}
