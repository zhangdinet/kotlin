/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.protobuf.MessageLite

class ReadContext(
    internal val strings: NameResolver,
    internal val types: TypeTable,
    internal val versionRequirements: VersionRequirementTable,
    private val parent: ReadContext? = null
) {
    private val extensions = MetadataExtensions.INSTANCE
    private val typeParameterNameToId = mutableMapOf<Int, Int>()

    operator fun get(index: Int): String =
        strings.getString(index)

    fun className(index: Int): ClassName =
        strings.getClassName(index)

    fun ext(proto: ProtoBuf.Function): FunctionVisitor.Extensions = extensions.readFunctionExtensions(proto, strings, types)

    fun ext(proto: ProtoBuf.Property): PropertyVisitor.Extensions = extensions.readPropertyExtensions(proto, strings, types)

    fun ext(proto: ProtoBuf.Constructor): ConstructorVisitor.Extensions = extensions.readConstructorExtensions(proto, strings, types)

    fun ext(proto: ProtoBuf.TypeParameter): TypeParameterVisitor.Extensions = extensions.readTypeParameterExtensions(proto, strings)

    fun ext(proto: ProtoBuf.Type): TypeVisitor.Extensions = extensions.readTypeExtensions(proto, strings)

    fun getTypeParameterId(name: Int): Int? =
        typeParameterNameToId[name] ?: parent?.getTypeParameterId(name)

    fun withTypeParameters(typeParameters: List<ProtoBuf.TypeParameter>): ReadContext =
        ReadContext(strings, types, versionRequirements, this).apply {
            for (typeParameter in typeParameters) {
                typeParameterNameToId[typeParameter.name] = typeParameter.id
            }
        }
}

fun ProtoBuf.Class.accept(v: ClassVisitor, strings: NameResolver) {
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.create(versionRequirementTable))
        .withTypeParameters(typeParameterList)

    v.visit(flags, c.className(fqName))

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    for (supertype in supertypes(c.types)) {
        v.visitSupertype(supertype.typeFlags, c.ext(supertype))?.let { supertype.accept(it, c) }
    }

    for (constructor in constructorList) {
        v.visitConstructor(constructor.flags, c.ext(constructor))?.let { constructor.accept(it, c) }
    }

    v.visitDeclarations(functionList, propertyList, typeAliasList, c)

    if (hasCompanionObjectName()) {
        v.visitCompanionObject(c[companionObjectName])
    }

    for (nestedClassName in nestedClassNameList) {
        v.visitNestedClass(c[nestedClassName])
    }

    for (enumEntry in enumEntryList) {
        if (!enumEntry.hasName()) {
            throw InconsistentKotlinMetadataException("No name for EnumEntry")
        }
        v.visitEnumEntry(c[enumEntry.name])
    }

    for (sealedSubclassFqName in sealedSubclassFqNameList) {
        v.visitSealedSubclass(c.className(sealedSubclassFqName))
    }

    if (hasVersionRequirement()) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(it, c) }
    }

    v.visitEnd()
}

fun ProtoBuf.Package.accept(v: PackageVisitor, strings: NameResolver) {
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.create(versionRequirementTable))

    v.visitDeclarations(functionList, propertyList, typeAliasList, c)

    v.visitEnd()
}

private fun DeclarationContainerVisitor.visitDeclarations(
    functions: List<ProtoBuf.Function>,
    properties: List<ProtoBuf.Property>,
    typeAliases: List<ProtoBuf.TypeAlias>,
    c: ReadContext
) {
    for (function in functions) {
        visitFunction(function.flags, c[function.name], c.ext(function))?.let { function.accept(it, c) }
    }

    for (property in properties) {
        val flags = property.flags
        val defaultAccessorFlags = Flags.getAccessorFlags(
            Flags.HAS_ANNOTATIONS.get(flags), Flags.VISIBILITY.get(flags), Flags.MODALITY.get(flags), false, false, false
        )
        visitProperty(
            flags,
            c[property.name],
            if (property.hasGetterFlags()) property.getterFlags else defaultAccessorFlags,
            if (property.hasSetterFlags()) property.setterFlags else defaultAccessorFlags,
            c.ext(property)
        )?.let { property.accept(it, c) }
    }

    for (typeAlias in typeAliases) {
        visitTypeAlias(typeAlias.flags, c[typeAlias.name])?.let { typeAlias.accept(it, c) }
    }
}

fun ProtoBuf.Function.accept(v: LambdaVisitor, strings: NameResolver) {
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.EMPTY)

    v.visitFunction(flags, c[name], c.ext(this))?.let { accept(it, c) }

    v.visitEnd()
}

private fun ProtoBuf.Constructor.accept(v: ConstructorVisitor, c: ReadContext) {
    for (parameter in valueParameterList) {
        v.visitValueParameter(parameter.flags, c[parameter.name])?.let { parameter.accept(it, c) }
    }

    if (hasVersionRequirement()) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Function.accept(v: FunctionVisitor, outer: ReadContext) {
    val c = outer.withTypeParameters(typeParameterList)

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    receiverType(c.types)?.let { receiverType ->
        v.visitReceiverParameterType(receiverType.typeFlags, c.ext(receiverType))?.let { receiverType.accept(it, c) }
    }

    for (parameter in valueParameterList) {
        v.visitValueParameter(parameter.flags, c[parameter.name])?.let { parameter.accept(it, c) }
    }

    returnType(c.types).let { returnType ->
        v.visitReturnType(returnType.typeFlags, c.ext(returnType))?.let { returnType.accept(it, c) }
    }

    if (hasContract()) {
        v.visitContract()?.let { contract.accept(it, c) }
    }

    if (hasVersionRequirement()) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Property.accept(v: PropertyVisitor, outer: ReadContext) {
    val c = outer.withTypeParameters(typeParameterList)

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    receiverType(c.types)?.let { receiverType ->
        v.visitReceiverParameterType(receiverType.typeFlags, c.ext(receiverType))?.let { receiverType.accept(it, c) }
    }

    if (hasSetterValueParameter()) {
        val parameter = setterValueParameter
        v.visitSetterParameter(parameter.flags, c[parameter.name])?.let { parameter.accept(it, c) }
    }

    returnType(c.types).let { returnType ->
        v.visitReturnType(returnType.typeFlags, c.ext(returnType))?.let { returnType.accept(it, c) }
    }

    if (hasVersionRequirement()) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.TypeAlias.accept(v: TypeAliasVisitor, outer: ReadContext) {
    val c = outer.withTypeParameters(typeParameterList)

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    underlyingType(c.types).let { underlyingType ->
        v.visitUnderlyingType(underlyingType.typeFlags, c.ext(underlyingType))?.let { underlyingType.accept(it, c) }
    }

    expandedType(c.types).let { expandedType ->
        v.visitExpandedType(expandedType.typeFlags, c.ext(expandedType))?.let { expandedType.accept(it, c) }
    }

    for (annotation in annotationList) {
        v.visitAnnotation(annotation.readAnnotation(c.strings))
    }

    if (hasVersionRequirement()) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.ValueParameter.accept(v: ValueParameterVisitor, c: ReadContext) {
    type(c.types).let { type ->
        v.visitType(type.typeFlags, c.ext(type))?.let { type.accept(it, c) }
    }

    varargElementType(c.types)?.let { varargElementType ->
        v.visitVarargElementType(varargElementType.typeFlags, c.ext(varargElementType))?.let { varargElementType.accept(it, c) }
    }

    v.visitEnd()
}

private inline fun ProtoBuf.TypeParameter.accept(
    visit: (flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions) -> TypeParameterVisitor?,
    c: ReadContext
) {
    val variance = when (variance!!) {
        ProtoBuf.TypeParameter.Variance.IN -> Variance.IN
        ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT
        ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
    }

    visit(typeParameterFlags, c[name], id, variance, c.ext(this))?.let { accept(it, c) }
}

private fun ProtoBuf.TypeParameter.accept(v: TypeParameterVisitor, c: ReadContext) {
    for (upperBound in upperBounds(c.types)) {
        v.visitUpperBound(upperBound.typeFlags, c.ext(upperBound))?.let { upperBound.accept(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Type.accept(v: TypeVisitor, c: ReadContext) {
    when {
        hasClassName() -> v.visitClass(c.className(className))
        hasTypeAliasName() -> v.visitTypeAlias(c.className(typeAliasName))
        hasTypeParameter() -> v.visitTypeParameter(typeParameter)
        hasTypeParameterName() -> {
            val id = c.getTypeParameterId(typeParameterName)
                    ?: throw InconsistentKotlinMetadataException("No type parameter id for ${c[typeParameterName]}")
            v.visitTypeParameter(id)
        }
        else -> {
            throw InconsistentKotlinMetadataException("No classifier (class, type alias or type parameter) recorded for Type")
        }
    }

    for (argument in argumentList) {
        val variance = when (argument.projection!!) {
            ProtoBuf.Type.Argument.Projection.IN -> Variance.IN
            ProtoBuf.Type.Argument.Projection.OUT -> Variance.OUT
            ProtoBuf.Type.Argument.Projection.INV -> Variance.INVARIANT
            ProtoBuf.Type.Argument.Projection.STAR -> null
        }

        if (variance != null) {
            val argumentType = argument.type(c.types)
                    ?: throw InconsistentKotlinMetadataException("No type argument for non-STAR projection in Type")
            v.visitArgument(argumentType.typeFlags, variance, c.ext(argumentType))?.let { argumentType.accept(it, c) }
        } else {
            v.visitStarProjection()
        }
    }

    abbreviatedType(c.types)?.let { abbreviatedType ->
        v.visitAbbreviatedType(abbreviatedType.typeFlags, c.ext(abbreviatedType))?.let { abbreviatedType.accept(it, c) }
    }

    outerType(c.types)?.let { outerType ->
        v.visitOuterType(outerType.typeFlags, c.ext(outerType))?.let { outerType.accept(it, c) }
    }

    flexibleUpperBound(c.types)?.let { upperBound ->
        v.visitFlexibleTypeUpperBound(
            upperBound.typeFlags,
            if (hasFlexibleTypeCapabilitiesId()) c[flexibleTypeCapabilitiesId] else null,
            c.ext(upperBound)
        )?.let { upperBound.accept(it, c) }
    }

    v.visitEnd()
}

private fun MessageLite.acceptVersionRequirementVisitor(v: VersionRequirementVisitor, c: ReadContext) {
    val message = VersionRequirement.create(this, c.strings, c.versionRequirements)
            ?: throw InconsistentKotlinMetadataException("No VersionRequirement with the given id in the table")

    val kind = when (message.kind) {
        ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION -> VersionRequirementVersionKind.LANGUAGE_VERSION
        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION -> VersionRequirementVersionKind.COMPILER_VERSION
        ProtoBuf.VersionRequirement.VersionKind.API_VERSION -> VersionRequirementVersionKind.API_VERSION
    }

    val level = when (message.level) {
        DeprecationLevel.WARNING -> VersionRequirementLevel.WARNING
        DeprecationLevel.ERROR -> VersionRequirementLevel.ERROR
        DeprecationLevel.HIDDEN -> VersionRequirementLevel.HIDDEN
    }

    v.visit(kind, level, message.errorCode, message.message)

    val (major, minor, patch) = message.version
    v.visitVersion(major, minor, patch)

    v.visitEnd()
}

private fun ProtoBuf.Contract.accept(v: ContractVisitor, c: ReadContext) {
    for (effect in effectList) {
        val effectType = if (!effect.hasEffectType()) null else when (effect.effectType!!) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> EffectType.RETURNS_CONSTANT
            ProtoBuf.Effect.EffectType.CALLS -> EffectType.CALLS
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> EffectType.RETURNS_NOT_NULL
        }

        val effectKind = if (!effect.hasKind()) null else when (effect.kind!!) {
            ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> EffectInvocationKind.AT_MOST_ONCE
            ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> EffectInvocationKind.EXACTLY_ONCE
            ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> EffectInvocationKind.AT_LEAST_ONCE
        }

        v.visitEffect(effectType, effectKind)?.let { effect.accept(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Effect.accept(v: EffectVisitor, c: ReadContext) {
    for (constructorArgument in effectConstructorArgumentList) {
        v.visitConstructorArgument()?.let { constructorArgument.accept(it, c) }
    }

    if (hasConclusionOfConditionalEffect()) {
        v.visitConclusionOfConditionalEffect()?.let { conclusionOfConditionalEffect.accept(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Expression.accept(v: EffectExpressionVisitor, c: ReadContext) {
    v.visit(
        flags,
        if (hasValueParameterReference()) valueParameterReference else null
    )

    if (hasConstantValue()) {
        v.visitConstantValue(
            when (constantValue!!) {
                ProtoBuf.Expression.ConstantValue.TRUE -> true
                ProtoBuf.Expression.ConstantValue.FALSE -> false
                ProtoBuf.Expression.ConstantValue.NULL -> null
            }
        )
    }

    isInstanceType(c.types)?.let { type ->
        v.visitIsInstanceType(type.typeFlags, c.ext(type))?.let { type.accept(it, c) }
    }

    for (andArgument in andArgumentList) {
        v.visitAndArgument()?.let { andArgument.accept(it, c) }
    }

    for (orArgument in orArgumentList) {
        v.visitOrArgument()?.let { orArgument.accept(it, c) }
    }

    v.visitEnd()
}

private val ProtoBuf.Type.typeFlags: Int
    get() = (if (nullable) 1 shl 0 else 0) +
            (flags shl 1)

private val ProtoBuf.TypeParameter.typeParameterFlags: Int
    get() = if (reified) 1 else 0
