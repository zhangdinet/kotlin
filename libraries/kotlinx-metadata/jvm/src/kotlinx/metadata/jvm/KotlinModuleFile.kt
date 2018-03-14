/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.Annotation
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping

class KotlinModuleFile(internal val mapping: ModuleMapping) {
    companion object {
        @JvmStatic
        fun read(bytes: ByteArray): KotlinModuleFile? {
            val mapping = ModuleMapping.create(bytes, "KotlinModuleFile", { true }, false, true)
            return if (mapping == ModuleMapping.EMPTY) null else KotlinModuleFile(mapping)
        }
    }
}

abstract class ModuleVisitor(private val delegate: ModuleVisitor? = null) {
    open fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        delegate?.visitPackageParts(fqName, fileFacades, multiFileClassParts)
    }

    open fun visitAnnotation(annotation: Annotation) {
        delegate?.visitAnnotation(annotation)
    }

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

fun KotlinModuleFile.accept(v: ModuleVisitor) {
    for ((fqName, parts) in mapping.packageFqName2Parts) {
        val allPackageParts = parts.parts
        val (fileFacades, multiFileClassParts) = allPackageParts.partition { parts.getMultifileFacadeName(it) == null }
        v.visitPackageParts(fqName, fileFacades, multiFileClassParts.associate { it to parts.getMultifileFacadeName(it)!! })
    }

    for (annotation in mapping.moduleData.annotations) {
        // TODO
    }

    v.visitEnd()
}
