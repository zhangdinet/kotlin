/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.Annotation
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

class JvmFunctionExtensions(
    val desc: String?
) : FunctionVisitor.Extensions

val FunctionVisitor.Extensions.jvmSignature: String?
    get() = (this as? JvmFunctionExtensions)?.desc


class JvmPropertyExtensions(
    val fieldName: String?,
    val fieldTypeDesc: String?,
    val getterDesc: String?,
    val setterDesc: String?,
    val syntheticMethodForAnnotationsDesc: String?
) : PropertyVisitor.Extensions

val PropertyVisitor.Extensions.jvmFieldName: String?
    get() = (this as? JvmPropertyExtensions)?.fieldName

val PropertyVisitor.Extensions.jvmFieldType: String?
    get() = (this as? JvmPropertyExtensions)?.fieldTypeDesc

val PropertyVisitor.Extensions.jvmGetterSignature: String?
    get() = (this as? JvmPropertyExtensions)?.getterDesc

val PropertyVisitor.Extensions.jvmSetterSignature: String?
    get() = (this as? JvmPropertyExtensions)?.setterDesc

val PropertyVisitor.Extensions.jvmSyntheticMethodForAnnotationsSignature: String?
    get() = (this as? JvmPropertyExtensions)?.syntheticMethodForAnnotationsDesc


class JvmConstructorExtensions(
    val desc: String?
) : ConstructorVisitor.Extensions

val ConstructorVisitor.Extensions.jvmSignature: String?
    get() = (this as? JvmConstructorExtensions)?.desc


class JvmTypeParameterExtensions(
    val annotations: List<Annotation>
) : TypeParameterVisitor.Extensions

val TypeParameterVisitor.Extensions.jvmAnnotations: List<Annotation>
    get() = (this as? JvmTypeParameterExtensions)?.annotations.orEmpty()


class JvmTypeExtensions(
    val annotations: List<Annotation>,
    val isRaw: Boolean
) : TypeVisitor.Extensions {
    companion object {
        const val PLATFORM_TYPE_ID = JvmProtoBufUtil.PLATFORM_TYPE_ID
    }
}

val TypeVisitor.Extensions.isRaw: Boolean
    get() = (this as? JvmTypeExtensions)?.isRaw == true

val TypeVisitor.Extensions.jvmAnnotations: List<Annotation>
    get() = (this as? JvmTypeExtensions)?.annotations.orEmpty()
