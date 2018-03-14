/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.declarations.FirValueParameter

interface FirFunctionType : FirTypeWithNullability {
    val receiverType: FirType?

    // May be it should inherit FirFunction?
    val valueParameters: List<FirValueParameter>

    val returnType: FirType
}