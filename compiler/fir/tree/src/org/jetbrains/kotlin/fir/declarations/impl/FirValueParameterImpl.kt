/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationKind
import org.jetbrains.kotlin.name.Name

class FirValueParameterImpl(
    override val session: FirSession,
    override val psi: PsiElement?,
    isProperty: Boolean,
    override val name: Name,
    override val returnType: FirType,
    override val defaultValue: FirExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean
) : FirValueParameter {
    override val declarationKind = if (isProperty) IrDeclarationKind.PROPERTY else IrDeclarationKind.VALUE_PARAMETER

    override val annotations = mutableListOf<FirAnnotationCall>()
}