/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirDelegatedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirDelegatedTypeImpl(
    session: FirSession,
    psi: PsiElement?,
    override val delegate: FirExpression?
) : FirAbstractAnnotatedType(session, psi, isNullable = false), FirDelegatedType {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirDelegatedType>.accept(visitor, data)
    }
}