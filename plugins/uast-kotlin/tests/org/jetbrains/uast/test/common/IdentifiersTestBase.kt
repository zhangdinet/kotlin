/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*
import org.jetbrains.uast.evaluation.UEvaluationContext
import org.jetbrains.uast.evaluation.UEvaluatorExtension
import org.jetbrains.uast.evaluation.analyzeAll
import org.jetbrains.uast.test.env.assertEqualsToFile
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

interface IdentifiersTestBase {
    fun getIdentifiersFile(testName: String): File

    private fun UFile.asLogValues(): String {
        val builder = StringBuilder()
        (this.psi as KtFile).accept(object : PsiElementVisitor(){
            override fun visitElement(element: PsiElement) {
                val uIdentifier = element.toUElementOfType<UIdentifier>()
                if(uIdentifier != null){
                    builder.appendln(uIdentifier.asLogString())
                }
                element.acceptChildren(this)


            }
        })
        return builder.toString()
    }

    fun check(testName: String, file: UFile) {
        val valuesFile = getIdentifiersFile(testName)

        assertEqualsToFile("Identifiers", valuesFile, file.asLogValues())
    }

}