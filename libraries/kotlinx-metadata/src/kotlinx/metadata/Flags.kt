/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import org.jetbrains.kotlin.metadata.ProtoBuf.*
import org.jetbrains.kotlin.metadata.deserialization.Flags.*
import org.jetbrains.kotlin.metadata.ProtoBuf.Class.Kind as ClassKind

// TODO: @JvmField for Java API?
object Flags {
    operator fun invoke(vararg flags: MetadataFlag): Int =
        flags.fold(0) { acc, flag -> acc + flag }

    val hasAnnotations = MetadataFlag(HAS_ANNOTATIONS)

    val isInternal = MetadataFlag(VISIBILITY, Visibility.INTERNAL_VALUE)
    val isPrivate = MetadataFlag(VISIBILITY, Visibility.PRIVATE_VALUE)
    val isProtected = MetadataFlag(VISIBILITY, Visibility.PROTECTED_VALUE)
    val isPublic = MetadataFlag(VISIBILITY, Visibility.PUBLIC_VALUE)
    val isPrivateToThis = MetadataFlag(VISIBILITY, Visibility.PRIVATE_TO_THIS_VALUE)
    val isLocal = MetadataFlag(VISIBILITY, Visibility.LOCAL_VALUE)

    val isFinal = MetadataFlag(MODALITY, Modality.FINAL_VALUE)
    val isOpen = MetadataFlag(MODALITY, Modality.OPEN_VALUE)
    val isAbstract = MetadataFlag(MODALITY, Modality.ABSTRACT_VALUE)
    val isSealed = MetadataFlag(MODALITY, Modality.SEALED_VALUE)

    object Class {
        val isClass = MetadataFlag(CLASS_KIND, ClassKind.CLASS_VALUE)
        val isInterface = MetadataFlag(CLASS_KIND, ClassKind.INTERFACE_VALUE)
        val isEnumClass = MetadataFlag(CLASS_KIND, ClassKind.ENUM_CLASS_VALUE)
        val isEnumEntry = MetadataFlag(CLASS_KIND, ClassKind.ENUM_ENTRY_VALUE)
        val isAnnotationClass = MetadataFlag(CLASS_KIND, ClassKind.ANNOTATION_CLASS_VALUE)
        val isObject = MetadataFlag(CLASS_KIND, ClassKind.OBJECT_VALUE)
        val isCompanionObject = MetadataFlag(CLASS_KIND, ClassKind.COMPANION_OBJECT_VALUE)

        val isInner = MetadataFlag(IS_INNER)
        val isData = MetadataFlag(IS_DATA)
        val isExternal = MetadataFlag(IS_EXTERNAL_CLASS)
        val isExpect = MetadataFlag(IS_EXPECT_CLASS)
        val isInline = MetadataFlag(IS_INLINE_CLASS)
    }

    object Constructor {
        val isPrimary = MetadataFlag(IS_SECONDARY, 0)
    }

    object Function {
        val isDeclaration = MetadataFlag(MEMBER_KIND, MemberKind.DECLARATION_VALUE)
        val isFakeOverride = MetadataFlag(MEMBER_KIND, MemberKind.FAKE_OVERRIDE_VALUE)
        val isDelegation = MetadataFlag(MEMBER_KIND, MemberKind.DELEGATION_VALUE)
        val isSynthesized = MetadataFlag(MEMBER_KIND, MemberKind.SYNTHESIZED_VALUE)

        val isOperator = MetadataFlag(IS_OPERATOR)
        val isInfix = MetadataFlag(IS_INFIX)
        val isInline = MetadataFlag(IS_INLINE)
        val isTailrec = MetadataFlag(IS_TAILREC)
        val isExternal = MetadataFlag(IS_EXTERNAL_FUNCTION)
        val isSuspend = MetadataFlag(IS_SUSPEND)
        val isExpect = MetadataFlag(IS_EXPECT_FUNCTION)
    }

    object Property {
        val isDeclaration = MetadataFlag(MEMBER_KIND, MemberKind.DECLARATION_VALUE)
        val isFakeOverride = MetadataFlag(MEMBER_KIND, MemberKind.FAKE_OVERRIDE_VALUE)
        val isDelegation = MetadataFlag(MEMBER_KIND, MemberKind.DELEGATION_VALUE)
        val isSynthesized = MetadataFlag(MEMBER_KIND, MemberKind.SYNTHESIZED_VALUE)

        val isVar = MetadataFlag(IS_VAR)
        val hasGetter = MetadataFlag(HAS_GETTER)
        val hasSetter = MetadataFlag(HAS_SETTER)
        val isConst = MetadataFlag(IS_CONST)
        val isLateinit = MetadataFlag(IS_LATEINIT)
        val hasConstant = MetadataFlag(HAS_CONSTANT)
        val isExternal = MetadataFlag(IS_EXTERNAL_PROPERTY)
        val isDelegated = MetadataFlag(IS_DELEGATED)
        val isExpect = MetadataFlag(IS_EXPECT_PROPERTY)
    }

    object PropertyAccessor {
        val isNotDefault = MetadataFlag(IS_NOT_DEFAULT)
        val isExternal = MetadataFlag(IS_EXTERNAL_ACCESSOR)
        val isInline = MetadataFlag(IS_INLINE_ACCESSOR)
    }

    object Type {
        val isNullable = MetadataFlag(0, 1, 1)
        val isSuspend = MetadataFlag(SUSPEND_TYPE.offset + 1, SUSPEND_TYPE.bitWidth, 1)
    }

    object TypeParameter {
        val isReified = MetadataFlag(0, 1, 1)
    }

    object ValueParameter {
        val declaresDefaultValue = MetadataFlag(DECLARES_DEFAULT_VALUE)
        val isCrossinline = MetadataFlag(IS_CROSSINLINE)
        val isNoinline = MetadataFlag(IS_NOINLINE)
    }
}
