/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import kotlinx.metadata.impl.readAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

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
}
