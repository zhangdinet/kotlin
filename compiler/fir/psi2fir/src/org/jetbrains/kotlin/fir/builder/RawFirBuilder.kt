/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.types.FirUserType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationKind
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifierType
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

class RawFirBuilder(val session: FirSession) {

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
    }

    private val KtModifierListOwner.visibility: Visibility
        get() {
            val modifierType = visibilityModifierType()
            return when (modifierType) {
                KtTokens.PUBLIC_KEYWORD -> Visibilities.PUBLIC
                KtTokens.PROTECTED_KEYWORD -> Visibilities.PROTECTED
                KtTokens.INTERNAL_KEYWORD -> Visibilities.INTERNAL
                KtTokens.PRIVATE_KEYWORD -> Visibilities.PRIVATE
                else -> Visibilities.UNKNOWN
            }
        }

    private val KtDeclaration.modality: Modality
        get() {
            val modifierType = modalityModifierType()
            return when (modifierType) {
                KtTokens.FINAL_KEYWORD -> Modality.FINAL
                KtTokens.SEALED_KEYWORD -> Modality.SEALED
                KtTokens.ABSTRACT_KEYWORD -> Modality.ABSTRACT
                KtTokens.OPEN_KEYWORD -> Modality.OPEN
                else -> Modality.FINAL // FIX ME
            }
        }

    private inner class Visitor : KtVisitor<FirElement, Unit>() {
        @Suppress("UNCHECKED_CAST")
        private fun <R : FirElement> KtElement?.convertSafe(): R? =
            this?.accept(this@Visitor, Unit) as? R

        @Suppress("UNCHECKED_CAST")
        private fun <R : FirElement> KtElement.convert(): R =
            this.accept(this@Visitor, Unit) as R

        private fun KtTypeReference?.toFirOrImplicitType(): FirType =
            convertSafe<FirUserType>() ?: FirImplicitTypeImpl(session, this)

        private fun KtTypeReference?.toFirOrErrorType(): FirType =
            convertSafe<FirUserType>() ?: FirErrorTypeImpl(session, this, false)

        private fun KtExpression?.toFirBody(): FirBody? =
            null

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            return super.visitKtFile(file, data) as FirFile
        }

        override fun visitNamedFunction(function: KtNamedFunction, data: Unit): FirElement {
            function.bodyExpression
            val firFunction = FirMemberFunctionImpl(
                session,
                function,
                IrDeclarationKind.FUNCTION,
                function.nameAsSafeName,
                function.visibility,
                function.modality,
                function.receiverTypeReference.convertSafe(),
                function.typeReference.toFirOrImplicitType(),
                function.bodyExpression.toFirBody()
            )
            for (annotationEntry in function.annotationEntries) {
                firFunction.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            for (typeParameter in function.typeParameters) {
                firFunction.typeParameters += typeParameter.convert<FirTypeParameter>()
            }
            for (valueParameter in function.valueParameters) {
                firFunction.valueParameters += valueParameter.convert<FirValueParameter>()
            }
            return firFunction
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): FirElement {
            val typeElement = typeReference.typeElement
            val isNullable = typeElement is KtNullableType

            fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
                if (this is KtNullableType) this.innerType.unwrapNullable() else this

            val unwrappedElement = typeElement.unwrapNullable()
            val firType = when (unwrappedElement) {
                is KtDynamicType -> FirDynamicTypeImpl(session, typeReference, isNullable)
                is KtUserType -> {
                    val referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        FirUserTypeImpl(
                            session, typeReference, isNullable,
                            referenceExpression.getReferencedNameAsName()
                        )
                    } else {
                        FirErrorTypeImpl(session, typeReference, isNullable)
                    }
                }
                is KtFunctionType -> {
                    val functionType = FirFunctionTypeImpl(
                        session,
                        typeReference,
                        isNullable,
                        unwrappedElement.receiverTypeReference.convertSafe(),
                        unwrappedElement.returnTypeReference.toFirOrImplicitType()
                    )
                    for (valueParameter in unwrappedElement.parameters) {
                        functionType.valueParameters += valueParameter.convert<FirValueParameter>()
                    }
                    functionType
                }
                null -> FirErrorTypeImpl(session, typeReference, isNullable)
                else -> throw AssertionError("Unexpected type element: ${unwrappedElement.text}")
            }

            for (annotationEntry in typeReference.annotationEntries) {
                firType.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            if (typeElement == null || firType !is FirUserTypeImpl) return firType
            for (typeArgument in typeElement.typeArgumentsAsTypes) {
                firType.arguments += typeArgument.convert<FirTypeProjection>()
            }
            return firType
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit): FirElement {
            return FirAnnotationCallImpl(
                session,
                annotationEntry,
                annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget(),
                annotationEntry.typeReference.toFirOrErrorType()
            )
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit): FirElement {
            val firTypeParameter = FirTypeParameterImpl(
                session,
                parameter,
                parameter.nameAsSafeName,
                parameter.variance
            )
            for (annotationEntry in parameter.annotationEntries) {
                firTypeParameter.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            val extendsBound = parameter.extendsBound
            // TODO: handle where, here or (preferable) in parent
            if (extendsBound != null) {
                firTypeParameter.bounds += extendsBound.convert<FirType>()
            }
            return firTypeParameter
        }

        override fun visitParameter(parameter: KtParameter, data: Unit): FirElement {
            val firValueParameter = FirValueParameterImpl(
                session,
                parameter,
                parameter.hasValOrVar(),
                parameter.nameAsSafeName,
                parameter.typeReference.toFirOrErrorType(),
                parameter.defaultValue?.convert(),
                isCrossinline = parameter.hasModifier(KtTokens.CROSSINLINE_KEYWORD),
                isNoinline = parameter.hasModifier(KtTokens.NOINLINE_KEYWORD),
                isVararg = parameter.isVarArg
            )
            for (annotationEntry in parameter.annotationEntries) {
                firValueParameter.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            return firValueParameter
        }

        override fun visitExpression(expression: KtExpression, data: Unit): FirElement {
            return FirExpressionStub(session, expression)
        }
    }
}