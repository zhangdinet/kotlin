/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.name.FqName

interface FirFile : FirPackageFragment, FirDeclaration, FirAnnotationContainer {
    val packageFqName: FqName

    val imports: List<FirImport>
}