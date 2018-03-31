/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.impl.ResolveScopeManager
import org.jetbrains.kotlin.psi.*

class KtLightPsiArrayInitializerMemberValue(
    val ktOrigin: KtElement,
    val lightParent: PsiElement,
    val arguments: List<KtStringTemplateExpression>
) : PsiElement by ktOrigin, PsiArrayInitializerMemberValue {
    override fun getInitializers(): Array<PsiAnnotationMemberValue> {
        return arguments
            .map { KtLightPsiLiteral(it, this) }
            .toTypedArray()
    }

    override fun getParent(): PsiElement = lightParent
}

class KtLightPsiLiteral(
    val ktOrigin: KtStringTemplateExpression,
    val lightParent: PsiElement
) : PsiElement by ktOrigin, PsiLiteralExpression {
    override fun getValue(): Any? {
        return (ktOrigin.entries.single() as KtLiteralStringTemplateEntry).text
    }

    override fun getType(): PsiType? {
        val manager = manager
        val resolveScope = ResolveScopeManager.getElementResolveScope(this)
        return PsiType.getJavaLangString(manager, resolveScope)
    }

    override fun getParent(): PsiElement = lightParent
}