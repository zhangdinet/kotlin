/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl.extensions

import kotlinx.metadata.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.serialization.StringTable
import java.util.*

interface MetadataExtensions {
    fun readFunctionExtensions(proto: ProtoBuf.Function, strings: NameResolver, types: TypeTable): FunctionVisitor.Extensions

    fun readPropertyExtensions(proto: ProtoBuf.Property, strings: NameResolver, types: TypeTable): PropertyVisitor.Extensions

    fun readConstructorExtensions(proto: ProtoBuf.Constructor, strings: NameResolver, types: TypeTable): ConstructorVisitor.Extensions

    fun readTypeParameterExtensions(proto: ProtoBuf.TypeParameter, strings: NameResolver): TypeParameterVisitor.Extensions

    fun readTypeExtensions(proto: ProtoBuf.Type, strings: NameResolver): TypeVisitor.Extensions

    fun createStringTable(): StringTable

    fun writeFunctionExtensions(ext: FunctionVisitor.Extensions, proto: ProtoBuf.Function.Builder, strings: StringTable)

    fun writePropertyExtensions(ext: PropertyVisitor.Extensions, proto: ProtoBuf.Property.Builder, strings: StringTable)

    fun writeConstructorExtensions(ext: ConstructorVisitor.Extensions, proto: ProtoBuf.Constructor.Builder, strings: StringTable)

    fun writeTypeParameterExtensions(ext: TypeParameterVisitor.Extensions, proto: ProtoBuf.TypeParameter.Builder, strings: StringTable)

    fun writeTypeExtensions(ext: TypeVisitor.Extensions, proto: ProtoBuf.Type.Builder, strings: StringTable)

    companion object {
        val INSTANCE: MetadataExtensions by lazy {
            ServiceLoader.load(MetadataExtensions::class.java).toList().firstOrNull()
                    ?: error(
                        "No MetadataExtensions instances found in the classpath. Please ensure that the META-INF/services/ " +
                                "is not stripped from your application and that the Java virtual machine is not running " +
                                "under a security manager"
                    )
        }
    }
}
