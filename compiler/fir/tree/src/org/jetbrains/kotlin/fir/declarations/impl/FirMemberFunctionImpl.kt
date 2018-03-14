/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationKind
import org.jetbrains.kotlin.name.Name

class FirMemberFunctionImpl(
    override val session: FirSession,
    override val psi: PsiElement?,
    override val declarationKind: IrDeclarationKind,
    override val name: Name,
    override val visibility: Visibility,
    override val modality: Modality,
    override val receiverType: FirType?,
    override val returnType: FirType,
    override val body: FirBody?
) : FirNamedFunction {
    override val annotations = mutableListOf<FirAnnotationCall>()

    override val typeParameters = mutableListOf<FirTypeParameter>()

    override val valueParameters = mutableListOf<FirValueParameter>()
}