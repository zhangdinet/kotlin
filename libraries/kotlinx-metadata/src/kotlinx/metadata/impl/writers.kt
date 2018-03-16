/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.*
import kotlinx.metadata.Annotation
import kotlinx.metadata.impl.extensions.MetadataExtensions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.metadata.serialization.StringTable

class WriteContext {
    internal val extensions = MetadataExtensions.INSTANCE
    val strings: StringTable = extensions.createStringTable()
    val versionRequirements: MutableVersionRequirementTable = MutableVersionRequirementTable()

    operator fun get(string: String): Int =
        strings.getStringIndex(string)

    operator fun get(name: ClassName): Int =
        strings.getClassNameIndex(name)
}

private fun writeTypeParameter(
    c: WriteContext, flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions,
    output: (ProtoBuf.TypeParameter.Builder) -> Unit
): TypeParameterVisitor =
    object : TypeParameterVisitor() {
        private val t = ProtoBuf.TypeParameter.newBuilder()

        override fun visitUpperBound(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
            writeType(c, flags, ext) { t.addUpperBound(it) }

        override fun visitEnd() {
            t.name = c[name]
            t.id = id
            val reified = Flags.TypeParameter.isReified(flags)
            if (reified != ProtoBuf.TypeParameter.getDefaultInstance().reified) {
                t.reified = reified
            }
            if (variance == Variance.IN) {
                t.variance = ProtoBuf.TypeParameter.Variance.IN
            } else if (variance == Variance.OUT) {
                t.variance = ProtoBuf.TypeParameter.Variance.OUT
            }
            c.extensions.writeTypeParameterExtensions(ext, t, c.strings)
            output(t)
        }
    }

private fun writeType(c: WriteContext, flags: Int, ext: TypeVisitor.Extensions, output: (ProtoBuf.Type.Builder) -> Unit): TypeVisitor =
    object : TypeVisitor() {
        private val t = ProtoBuf.Type.newBuilder()

        override fun visitClass(name: ClassName) {
            t.className = c[name]
        }

        override fun visitTypeAlias(name: ClassName) {
            t.typeAliasName = c[name]
        }

        override fun visitStarProjection() {
            t.addArgument(ProtoBuf.Type.Argument.newBuilder().apply {
                projection = ProtoBuf.Type.Argument.Projection.STAR
            })
        }

        override fun visitArgument(flags: Int, variance: Variance, ext: TypeVisitor.Extensions): TypeVisitor? =
            writeType(c, flags, ext) { argument ->
                t.addArgument(ProtoBuf.Type.Argument.newBuilder().apply {
                    if (variance == Variance.IN) {
                        projection = ProtoBuf.Type.Argument.Projection.IN
                    } else if (variance == Variance.OUT) {
                        projection = ProtoBuf.Type.Argument.Projection.OUT
                    }
                    type = argument.build()
                })
            }

        override fun visitTypeParameter(id: Int) {
            t.typeParameter = id
        }

        override fun visitAbbreviatedType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
            writeType(c, flags, ext) { t.abbreviatedType = it.build() }

        override fun visitOuterType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
            writeType(c, flags, ext) { t.outerType = it.build() }

        override fun visitFlexibleTypeUpperBound(flags: Int, typeFlexibilityId: String?, ext: TypeVisitor.Extensions): TypeVisitor? =
            writeType(c, flags, ext) {
                if (typeFlexibilityId != null) {
                    t.flexibleTypeCapabilitiesId = c[typeFlexibilityId]
                }
                t.flexibleUpperBound = it.build()
            }

        override fun visitEnd() {
            if (Flags.Type.isNullable(flags)) {
                t.nullable = true
            }
            c.extensions.writeTypeExtensions(ext, t, c.strings)
            val flagsToWrite = flags shr 1
            if (flagsToWrite != ProtoBuf.Type.getDefaultInstance().flags) {
                t.flags = flagsToWrite
            }
            output(t)
        }
    }

private fun writeConstructor(
    c: WriteContext, flags: Int, ext: ConstructorVisitor.Extensions,
    output: (ProtoBuf.Constructor.Builder) -> Unit
): ConstructorVisitor = object : ConstructorVisitor() {
    val t = ProtoBuf.Constructor.newBuilder()

    override fun visitValueParameter(flags: Int, name: String): ValueParameterVisitor? =
        writeValueParameter(c, flags, name) { t.addValueParameter(it.build()) }

    override fun visitVersionRequirement(): VersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitEnd() {
        if (flags != ProtoBuf.Constructor.getDefaultInstance().flags) {
            t.flags = flags
        }
        c.extensions.writeConstructorExtensions(ext, t, c.strings)
        output(t)
    }
}

private fun writeFunction(
    c: WriteContext, flags: Int, name: String, ext: FunctionVisitor.Extensions,
    output: (ProtoBuf.Function.Builder) -> Unit
): FunctionVisitor = object : FunctionVisitor() {
    val t = ProtoBuf.Function.newBuilder()

    override fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance, ext) { t.addTypeParameter(it) }

    override fun visitReceiverParameterType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.receiverType = it.build() }

    override fun visitValueParameter(flags: Int, name: String): ValueParameterVisitor? =
        writeValueParameter(c, flags, name) { t.addValueParameter(it) }

    override fun visitReturnType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.returnType = it.build() }

    override fun visitVersionRequirement(): VersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitContract(): ContractVisitor? =
        writeContract(c) { t.contract = it.build() }

    override fun visitEnd() {
        t.name = c[name]
        if (flags != ProtoBuf.Function.getDefaultInstance().flags) {
            t.flags = flags
        }
        c.extensions.writeFunctionExtensions(ext, t, c.strings)
        output(t)
    }
}

private fun writeProperty(
    c: WriteContext,
    flags: Int,
    name: String,
    getterFlags: Int,
    setterFlags: Int,
    ext: PropertyVisitor.Extensions,
    output: (ProtoBuf.Property.Builder) -> Unit
): PropertyVisitor = object : PropertyVisitor() {
    val t = ProtoBuf.Property.newBuilder()

    override fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance, ext) { t.addTypeParameter(it) }

    override fun visitReceiverParameterType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.receiverType = it.build() }

    override fun visitSetterParameter(flags: Int, name: String): ValueParameterVisitor? =
        writeValueParameter(c, flags, name) { t.setterValueParameter = it.build() }

    override fun visitReturnType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.returnType = it.build() }

    override fun visitVersionRequirement(): VersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitEnd() {
        t.name = c[name]
        if (flags != ProtoBuf.Property.getDefaultInstance().flags) {
            t.flags = flags
        }
        // TODO: do not write getterFlags/setterFlags if not needed
        t.getterFlags = getterFlags
        t.setterFlags = setterFlags
        c.extensions.writePropertyExtensions(ext, t, c.strings)
        output(t)
    }
}

private fun writeValueParameter(
    c: WriteContext, flags: Int, name: String,
    output: (ProtoBuf.ValueParameter.Builder) -> Unit
): ValueParameterVisitor = object : ValueParameterVisitor() {
    val t = ProtoBuf.ValueParameter.newBuilder()

    override fun visitType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.type = it.build() }

    override fun visitVarargElementType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.varargElementType = it.build() }

    override fun visitEnd() {
        if (flags != ProtoBuf.ValueParameter.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.name = c[name]
        output(t)
    }
}

private fun writeTypeAlias(
    c: WriteContext, flags: Int, name: String,
    output: (ProtoBuf.TypeAlias.Builder) -> Unit
): TypeAliasVisitor = object : TypeAliasVisitor() {
    val t = ProtoBuf.TypeAlias.newBuilder()

    override fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance, ext) { t.addTypeParameter(it) }

    override fun visitUnderlyingType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.underlyingType = it.build() }

    override fun visitExpandedType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.expandedType = it.build() }

    override fun visitAnnotation(annotation: Annotation) {
        t.addAnnotation(annotation.writeAnnotation(c.strings))
    }

    override fun visitVersionRequirement(): VersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitEnd() {
        if (flags != ProtoBuf.TypeAlias.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.name = c[name]
        output(t)
    }
}

private fun writeVersionRequirement(
    c: WriteContext, output: (Int) -> Unit
): VersionRequirementVisitor = object : VersionRequirementVisitor() {
    var t: ProtoBuf.VersionRequirement.Builder? = null

    override fun visit(kind: VersionRequirementVersionKind, level: VersionRequirementLevel, errorCode: Int?, message: String?) {
        t = ProtoBuf.VersionRequirement.newBuilder().apply {
            if (kind != defaultInstanceForType.versionKind) {
                this.versionKind = when (kind) {
                    VersionRequirementVersionKind.LANGUAGE_VERSION -> ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION
                    VersionRequirementVersionKind.COMPILER_VERSION -> ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
                    VersionRequirementVersionKind.API_VERSION -> ProtoBuf.VersionRequirement.VersionKind.API_VERSION
                }
            }
            if (level != defaultInstanceForType.level) {
                this.level = when (level) {
                    VersionRequirementLevel.WARNING -> ProtoBuf.VersionRequirement.Level.WARNING
                    VersionRequirementLevel.ERROR -> ProtoBuf.VersionRequirement.Level.ERROR
                    VersionRequirementLevel.HIDDEN -> ProtoBuf.VersionRequirement.Level.HIDDEN
                }
            }
            if (errorCode != null) {
                this.errorCode = errorCode
            }
            if (message != null) {
                this.message = c[message]
            }
        }
    }

    override fun visitVersion(major: Int, minor: Int, patch: Int) {
        if (t == null) {
            throw IllegalStateException("VersionRequirementVisitor.visit has not been called")
        }
        VersionRequirement.Version(major, minor, patch).encode(
            writeVersion = { t!!.version = it },
            writeVersionFull = { t!!.versionFull = it }
        )
    }

    override fun visitEnd() {
        if (t == null) {
            throw IllegalStateException("VersionRequirementVisitor.visit has not been called")
        }
        output(c.versionRequirements[t!!])
    }
}

private fun writeContract(c: WriteContext, output: (ProtoBuf.Contract.Builder) -> Unit): ContractVisitor =
    object : ContractVisitor() {
        val t = ProtoBuf.Contract.newBuilder()

        override fun visitEffect(type: EffectType?, invocationKind: EffectInvocationKind?): EffectVisitor? =
            writeEffect(c, type, invocationKind) { t.addEffect(it) }

        override fun visitEnd() {
            output(t)
        }
    }

private fun writeEffect(
    c: WriteContext, type: EffectType?, invocationKind: EffectInvocationKind?,
    output: (ProtoBuf.Effect.Builder) -> Unit
): EffectVisitor = object : EffectVisitor() {
    val t = ProtoBuf.Effect.newBuilder()

    override fun visitConstructorArgument(): EffectExpressionVisitor? =
        writeEffectExpression(c) { t.addEffectConstructorArgument(it) }

    override fun visitConclusionOfConditionalEffect(): EffectExpressionVisitor? =
        writeEffectExpression(c) { t.conclusionOfConditionalEffect = it.build() }

    @Suppress("UNUSED_VARIABLE") // force exhaustive whens
    override fun visitEnd() {
        val unused = when (type) {
            EffectType.RETURNS_CONSTANT -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
            EffectType.CALLS -> t.effectType = ProtoBuf.Effect.EffectType.CALLS
            EffectType.RETURNS_NOT_NULL -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL
            null -> null
        }
        val unused2 = when (invocationKind) {
            EffectInvocationKind.AT_MOST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE
            EffectInvocationKind.EXACTLY_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE
            EffectInvocationKind.AT_LEAST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE
            null -> null
        }
        output(t)
    }
}

private fun writeEffectExpression(c: WriteContext, output: (ProtoBuf.Expression.Builder) -> Unit): EffectExpressionVisitor =
    object : EffectExpressionVisitor() {
        val t = ProtoBuf.Expression.newBuilder()

        override fun visit(flags: Int, parameterIndex: Int?) {
            if (flags != ProtoBuf.Expression.getDefaultInstance().flags) {
                t.flags = flags
            }
            if (parameterIndex != null) {
                t.valueParameterReference = parameterIndex
            }
        }

        override fun visitConstantValue(value: Any?) {
            @Suppress("UNUSED_VARIABLE") // force exhaustive when
            val unused = when (value) {
                true -> t.constantValue = ProtoBuf.Expression.ConstantValue.TRUE
                false -> t.constantValue = ProtoBuf.Expression.ConstantValue.FALSE
                null -> null
                else -> throw IllegalArgumentException("Only true, false or null constant values are allowed for effects (was=$value)")
            }
        }

        override fun visitIsInstanceType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
            writeType(c, flags, ext) { t.isInstanceType = it.build() }

        override fun visitAndArgument(): EffectExpressionVisitor? =
            writeEffectExpression(c) { t.addAndArgument(it) }

        override fun visitOrArgument(): EffectExpressionVisitor? =
            writeEffectExpression(c) { t.addOrArgument(it) }

        override fun visitEnd() {
            output(t)
        }
    }

open class ClassWriter : ClassVisitor() {
    val t = ProtoBuf.Class.newBuilder()!!
    val c = WriteContext()

    override fun visit(flags: Int, name: ClassName) {
        if (flags != ProtoBuf.Class.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.fqName = c[name]
    }

    override fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance, ext) { t.addTypeParameter(it) }

    override fun visitSupertype(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        writeType(c, flags, ext) { t.addSupertype(it) }

    override fun visitConstructor(flags: Int, ext: ConstructorVisitor.Extensions): ConstructorVisitor? =
        writeConstructor(c, flags, ext) { t.addConstructor(it) }

    override fun visitFunction(flags: Int, name: String, ext: FunctionVisitor.Extensions): FunctionVisitor? =
        writeFunction(c, flags, name, ext) { t.addFunction(it) }

    override fun visitProperty(
        flags: Int,
        name: String,
        getterFlags: Int,
        setterFlags: Int,
        ext: PropertyVisitor.Extensions
    ): PropertyVisitor? =
        writeProperty(c, flags, name, getterFlags, setterFlags, ext) { t.addProperty(it) }

    override fun visitTypeAlias(flags: Int, name: String): TypeAliasVisitor? =
        writeTypeAlias(c, flags, name) { t.addTypeAlias(it) }

    override fun visitCompanionObject(name: String) {
        t.companionObjectName = c[name]
    }

    override fun visitNestedClass(name: String) {
        t.addNestedClassName(c[name])
    }

    override fun visitEnumEntry(name: String) {
        t.addEnumEntry(ProtoBuf.EnumEntry.newBuilder().also { enumEntry ->
            enumEntry.name = c[name]
        })
    }

    override fun visitSealedSubclass(name: ClassName) {
        t.addSealedSubclassFqName(c[name])
    }

    override fun visitVersionRequirement(): VersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitEnd() {
        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

open class PackageWriter : PackageVisitor() {
    val t = ProtoBuf.Package.newBuilder()!!
    val c = WriteContext()

    override fun visitFunction(flags: Int, name: String, ext: FunctionVisitor.Extensions): FunctionVisitor? =
        writeFunction(c, flags, name, ext) { t.addFunction(it) }

    override fun visitProperty(
        flags: Int,
        name: String,
        getterFlags: Int,
        setterFlags: Int,
        ext: PropertyVisitor.Extensions
    ): PropertyVisitor? =
        writeProperty(c, flags, name, getterFlags, setterFlags, ext) { t.addProperty(it) }

    override fun visitTypeAlias(flags: Int, name: String): TypeAliasVisitor? =
        writeTypeAlias(c, flags, name) { t.addTypeAlias(it) }

    override fun visitEnd() {
        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

open class LambdaWriter : LambdaVisitor() {
    var t: ProtoBuf.Function.Builder? = null
    val c = WriteContext()

    override fun visitFunction(flags: Int, name: String, ext: FunctionVisitor.Extensions): FunctionVisitor? =
        writeFunction(c, flags, name, ext) { t = it }
}
