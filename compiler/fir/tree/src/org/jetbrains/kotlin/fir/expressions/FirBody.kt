/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

// Should we have FirBlockBody / FirExpressionBody?
// Is it FirExpression or just FirElement?
interface FirBody : FirExpression {
    val statements: List<FirStatement>
}