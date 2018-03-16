/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import kotlinx.metadata.impl.readAnnotation
import kotlinx.metadata.impl.writeAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.metadata.serialization.StringTable

class JvmMetadataExtensions : MetadataExtensions {
    override fun readFunctionExtensions(proto: ProtoBuf.Function, strings: NameResolver, types: TypeTable): FunctionVisitor.Extensions {
        return JvmFunctionExtensions(JvmProtoBufUtil.getJvmMethodSignature(proto, strings, types))
    }

    override fun readPropertyExtensions(proto: ProtoBuf.Property, strings: NameResolver, types: TypeTable): PropertyVisitor.Extensions {
        val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, strings, types)
        val propertySignature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
        val getterSignature =
            if (propertySignature != null && propertySignature.hasGetter()) propertySignature.getter else null
        val setterSignature =
            if (propertySignature != null && propertySignature.hasSetter()) propertySignature.setter else null
        val syntheticMethod =
            if (propertySignature != null && propertySignature.hasSyntheticMethod()) propertySignature.syntheticMethod else null
        return JvmPropertyExtensions(
            fieldSignature?.name,
            fieldSignature?.desc,
            getterSignature?.run { strings.getString(name) + strings.getString(desc) },
            setterSignature?.run { strings.getString(name) + strings.getString(desc) },
            syntheticMethod?.run { strings.getString(name) + strings.getString(desc) }
        )
    }

    override fun readConstructorExtensions(
        proto: ProtoBuf.Constructor, strings: NameResolver, types: TypeTable
    ): ConstructorVisitor.Extensions {
        return JvmConstructorExtensions(JvmProtoBufUtil.getJvmConstructorSignature(proto, strings, types))
    }

    override fun readTypeParameterExtensions(proto: ProtoBuf.TypeParameter, strings: NameResolver): TypeParameterVisitor.Extensions {
        return JvmTypeParameterExtensions(proto.getExtension(JvmProtoBuf.typeParameterAnnotation).map { it.readAnnotation(strings) })
    }

    override fun readTypeExtensions(proto: ProtoBuf.Type, strings: NameResolver): TypeVisitor.Extensions {
        return JvmTypeExtensions(
            proto.getExtension(JvmProtoBuf.typeAnnotation).map { it.readAnnotation(strings) },
            proto.getExtension(JvmProtoBuf.isRaw)
        )
    }

    override fun createStringTable(): StringTable = JvmStringTable()

    override fun writeFunctionExtensions(ext: FunctionVisitor.Extensions, proto: ProtoBuf.Function.Builder, strings: StringTable) {
        ext.jvmSignature?.let { desc ->
            proto.setExtension(JvmProtoBuf.methodSignature, desc.toJvmMethodSignature(strings))
        }
    }

    override fun writePropertyExtensions(ext: PropertyVisitor.Extensions, proto: ProtoBuf.Property.Builder, strings: StringTable) {
        val fieldName = ext.jvmFieldName
        val fieldType = ext.jvmFieldType
        val getterDesc = ext.jvmGetterSignature
        val setterDesc = ext.jvmSetterSignature
        val syntheticMethodDesc = ext.jvmSyntheticMethodForAnnotationsSignature

        if (fieldName == null && fieldType == null && getterDesc == null && setterDesc == null && syntheticMethodDesc == null) return

        proto.setExtension(JvmProtoBuf.propertySignature, JvmProtoBuf.JvmPropertySignature.newBuilder().apply {
            if (fieldName != null || fieldType != null) {
                field = JvmProtoBuf.JvmFieldSignature.newBuilder().also { field ->
                    if (fieldName != null) {
                        field.name = strings.getStringIndex(fieldName)
                    }
                    if (fieldType != null) {
                        field.desc = strings.getStringIndex(fieldType)
                    }
                }.build()
            }
            if (getterDesc != null) {
                getter = getterDesc.toJvmMethodSignature(strings)
            }
            if (setterDesc != null) {
                setter = setterDesc.toJvmMethodSignature(strings)
            }
            if (syntheticMethodDesc != null) {
                syntheticMethod = syntheticMethodDesc.toJvmMethodSignature(strings)
            }
        }.build())
    }

    override fun writeConstructorExtensions(ext: ConstructorVisitor.Extensions, proto: ProtoBuf.Constructor.Builder, strings: StringTable) {
        ext.jvmSignature?.let { desc ->
            proto.setExtension(JvmProtoBuf.constructorSignature, desc.toJvmMethodSignature(strings))
        }
    }

    override fun writeTypeParameterExtensions(
        ext: TypeParameterVisitor.Extensions, proto: ProtoBuf.TypeParameter.Builder, strings: StringTable
    ) {
        for (annotation in ext.jvmAnnotations) {
            proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotation.writeAnnotation(strings).build())
        }
    }

    override fun writeTypeExtensions(ext: TypeVisitor.Extensions, proto: ProtoBuf.Type.Builder, strings: StringTable) {
        for (annotation in ext.jvmAnnotations) {
            proto.addExtension(JvmProtoBuf.typeAnnotation, annotation.writeAnnotation(strings).build())
        }
        if (ext.isRaw) {
            proto.setExtension(JvmProtoBuf.isRaw, true)
        }
    }

    private fun String.toJvmMethodSignature(strings: StringTable): JvmProtoBuf.JvmMethodSignature =
        JvmProtoBuf.JvmMethodSignature.newBuilder().apply {
            name = strings.getStringIndex(substringBefore('('))
            desc = strings.getStringIndex("(" + substringAfter('('))
        }.build()
}
