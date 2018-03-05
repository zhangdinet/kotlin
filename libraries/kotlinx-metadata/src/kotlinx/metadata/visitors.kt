/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

data class ClassName(val name: String, val isLocal: Boolean = false) {
    override fun toString(): String = name
}

abstract class DeclarationContainerVisitor(protected open val delegate: DeclarationContainerVisitor? = null) {
    open fun visitFunction(flags: Int, name: String, ext: FunctionVisitor.Extensions): FunctionVisitor? =
        delegate?.visitFunction(flags, name, ext)

    open fun visitProperty(
        flags: Int,
        name: String,
        getterFlags: Int,
        setterFlags: Int,
        ext: PropertyVisitor.Extensions
    ): PropertyVisitor? =
        delegate?.visitProperty(flags, name, getterFlags, setterFlags, ext)

    open fun visitTypeAlias(flags: Int, name: String): TypeAliasVisitor? =
        delegate?.visitTypeAlias(flags, name)
}

abstract class ClassVisitor(delegate: ClassVisitor? = null) : DeclarationContainerVisitor(delegate) {
    override val delegate: ClassVisitor?
        get() = super.delegate as ClassVisitor?

    open fun visit(flags: Int, name: ClassName) {
        delegate?.visit(flags, name)
    }

    open fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance, ext)

    open fun visitSupertype(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitSupertype(flags, ext)

    open fun visitConstructor(flags: Int, ext: ConstructorVisitor.Extensions): ConstructorVisitor? =
        delegate?.visitConstructor(flags, ext)

    open fun visitCompanionObject(name: String) {
        delegate?.visitCompanionObject(name)
    }

    open fun visitNestedClass(name: String) {
        delegate?.visitNestedClass(name)
    }

    open fun visitEnumEntry(name: String) {
        delegate?.visitEnumEntry(name)
    }

    open fun visitSealedSubclass(name: ClassName) {
        delegate?.visitSealedSubclass(name)
    }

    open fun visitVersionRequirement(): VersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class PackageVisitor(delegate: PackageVisitor? = null) : DeclarationContainerVisitor(delegate) {
    override val delegate: PackageVisitor?
        get() = super.delegate as PackageVisitor?

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class LambdaVisitor(private val delegate: LambdaVisitor? = null) {
    open fun visitFunction(flags: Int, name: String, ext: FunctionVisitor.Extensions): FunctionVisitor? =
        delegate?.visitFunction(flags, name, ext)

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class ConstructorVisitor(private val delegate: ConstructorVisitor? = null) {
    interface Extensions

    open fun visitValueParameter(flags: Int, name: String): ValueParameterVisitor? =
        delegate?.visitValueParameter(flags, name)

    open fun visitVersionRequirement(): VersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class FunctionVisitor(private val delegate: FunctionVisitor? = null) {
    interface Extensions

    open fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance, ext)

    open fun visitReceiverParameterType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitReceiverParameterType(flags, ext)

    open fun visitValueParameter(flags: Int, name: String): ValueParameterVisitor? =
        delegate?.visitValueParameter(flags, name)

    open fun visitReturnType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitReturnType(flags, ext)

    open fun visitVersionRequirement(): VersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    open fun visitContract(): ContractVisitor? =
        delegate?.visitContract()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class PropertyVisitor(private val delegate: PropertyVisitor? = null) {
    interface Extensions

    open fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance, ext)

    open fun visitReceiverParameterType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitReceiverParameterType(flags, ext)

    open fun visitSetterParameter(flags: Int, name: String): ValueParameterVisitor? =
        delegate?.visitSetterParameter(flags, name)

    open fun visitReturnType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitReturnType(flags, ext)

    open fun visitVersionRequirement(): VersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class TypeAliasVisitor(private val delegate: TypeAliasVisitor? = null) {
    open fun visitTypeParameter(
        flags: Int, name: String, id: Int, variance: Variance, ext: TypeParameterVisitor.Extensions
    ): TypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance, ext)

    open fun visitUnderlyingType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitUnderlyingType(flags, ext)

    open fun visitExpandedType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitExpandedType(flags, ext)

    open fun visitAnnotation(annotation: Annotation) {
        delegate?.visitAnnotation(annotation)
    }

    open fun visitVersionRequirement(): VersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class ValueParameterVisitor(private val delegate: ValueParameterVisitor? = null) {
    open fun visitType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitType(flags, ext)

    open fun visitVarargElementType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitVarargElementType(flags, ext)

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class TypeParameterVisitor(private val delegate: TypeParameterVisitor? = null) {
    interface Extensions

    open fun visitUpperBound(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitUpperBound(flags, ext)

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class TypeVisitor(private val delegate: TypeVisitor? = null) {
    interface Extensions

    open fun visitClass(name: ClassName) {
        delegate?.visitClass(name)
    }

    open fun visitTypeAlias(name: ClassName) {
        delegate?.visitTypeAlias(name)
    }

    open fun visitStarProjection() {
        delegate?.visitStarProjection()
    }

    open fun visitArgument(flags: Int, variance: Variance, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitArgument(flags, variance, ext)

    open fun visitTypeParameter(id: Int) {
        delegate?.visitTypeParameter(id)
    }

    open fun visitAbbreviatedType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitAbbreviatedType(flags, ext)

    open fun visitOuterType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitOuterType(flags, ext)

    open fun visitFlexibleTypeUpperBound(flags: Int, typeFlexibilityId: String?, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitFlexibleTypeUpperBound(flags, typeFlexibilityId, ext)

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class VersionRequirementVisitor(private val delegate: VersionRequirementVisitor? = null) {
    open fun visit(kind: VersionRequirementVersionKind, level: VersionRequirementLevel, errorCode: Int?, message: String?) {
        delegate?.visit(kind, level, errorCode, message)
    }

    open fun visitVersion(major: Int, minor: Int, patch: Int) {
        delegate?.visitVersion(major, minor, patch)
    }

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class ContractVisitor(private val delegate: ContractVisitor? = null) {
    open fun visitEffect(type: EffectType?, invocationKind: EffectInvocationKind?): EffectVisitor? =
        delegate?.visitEffect(type, invocationKind)

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class EffectVisitor(private val delegate: EffectVisitor? = null) {
    open fun visitConstructorArgument(): EffectExpressionVisitor? =
        delegate?.visitConstructorArgument()

    open fun visitConclusionOfConditionalEffect(): EffectExpressionVisitor? =
        delegate?.visitConclusionOfConditionalEffect()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

abstract class EffectExpressionVisitor(private val delegate: EffectExpressionVisitor? = null) {
    open fun visit(flags: Int, parameterIndex: Int?) {
        delegate?.visit(flags, parameterIndex)
    }

    open fun visitConstantValue(value: Any?) {
        delegate?.visitConstantValue(value)
    }

    open fun visitIsInstanceType(flags: Int, ext: TypeVisitor.Extensions): TypeVisitor? =
        delegate?.visitIsInstanceType(flags, ext)

    open fun visitAndArgument(): EffectExpressionVisitor? =
        delegate?.visitAndArgument()

    open fun visitOrArgument(): EffectExpressionVisitor? =
        delegate?.visitOrArgument()

    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

enum class Variance {
    INVARIANT,
    IN,
    OUT,
}

enum class EffectType {
    RETURNS_CONSTANT,
    CALLS,
    RETURNS_NOT_NULL,
}

enum class EffectInvocationKind {
    AT_MOST_ONCE,
    EXACTLY_ONCE,
    AT_LEAST_ONCE,
}

enum class VersionRequirementLevel {
    WARNING,
    ERROR,
    HIDDEN,
}

enum class VersionRequirementVersionKind {
    LANGUAGE_VERSION,
    COMPILER_VERSION,
    API_VERSION,
}

class Annotation(val className: ClassName, val arguments: Map<String, AnnotationArgument<*>>)

sealed class AnnotationArgument<out T : Any> {
    abstract val value: T
    
    data class ByteValue(override val value: Byte) : AnnotationArgument<Byte>()
    data class CharValue(override val value: Char) : AnnotationArgument<Char>()
    data class ShortValue(override val value: Short) : AnnotationArgument<Short>()
    data class IntValue(override val value: Int) : AnnotationArgument<Int>()
    data class LongValue(override val value: Long) : AnnotationArgument<Long>()
    data class FloatValue(override val value: Float) : AnnotationArgument<Float>()
    data class DoubleValue(override val value: Double) : AnnotationArgument<Double>()
    data class BooleanValue(override val value: Boolean) : AnnotationArgument<Boolean>()
    
    data class StringValue(override val value: String) : AnnotationArgument<String>()
    data class KClassValue(override val value: ClassName) : AnnotationArgument<ClassName>()
    data class EnumValue(val enumClassName: ClassName, val enumEntryName: String) : AnnotationArgument<String>() {
        override val value: String = "$enumClassName.$enumEntryName"
    }
    data class AnnotationValue(override val value: Annotation) : AnnotationArgument<Annotation>()
    data class ArrayValue(override val value: List<AnnotationArgument<*>>) : AnnotationArgument<List<AnnotationArgument<*>>>()
}
