/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types.expressions

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

abstract class AssignedVariablesSearcher: KtTreeVisitorVoid() {

    private class DeclarationTreeNode(val declaration: KtDeclaration?) {
        val children: MutableList<DeclarationTreeNode> = ContainerUtil.newSmartList()
        val writersByName: MutableMap<Name, WritersInfo> = hashMapOf()

        fun updateInfoFromChildren() {
            for (child in children) {
                for ((name, childInfo) in child.writersByName) {
                    val writerInfo = writersByName.getOrPut(name) { WritersInfo() }
                    childInfo.assignmentWithMostEndOffset?.let {
                        writerInfo.acceptAssignment(it)
                        writerInfo.acceptInnerDeclarationWriter(child.declaration)
                    }

                    childInfo.writerInnerDeclarationWithLeastStartOffset?.let(writerInfo::acceptInnerDeclarationWriter)
                }
            }
        }
    }

    class WritersInfo {
        var writerInnerDeclarationWithLeastStartOffset: KtDeclaration? = null
            private set

        var assignmentWithMostEndOffset: KtBinaryExpression? = null
            private set

        internal fun acceptAssignment(assignment: KtBinaryExpression) {
            if (assignmentWithMostEndOffset.let { it == null || it.endOffset < assignment.endOffset }) {
                assignmentWithMostEndOffset = assignment
            }
        }

        internal fun acceptInnerDeclarationWriter(innerDeclaration: KtDeclaration?) {
            if (innerDeclaration != null &&
                writerInnerDeclarationWithLeastStartOffset.let { it == null || it.endOffset > innerDeclaration.endOffset }) {
                writerInnerDeclarationWithLeastStartOffset = innerDeclaration
            }
        }

        companion object {
            val EMPTY = WritersInfo()
        }

        fun isEmpty() = assignmentWithMostEndOffset == null
    }

    private val declarationTreeNodes: MutableMap<KtDeclaration, DeclarationTreeNode> = hashMapOf()
    private val rootDeclarationTreeNode = DeclarationTreeNode(null)
    private var currentDeclaration: KtDeclaration? = null
    private var currentDeclarationTreeNode: DeclarationTreeNode = rootDeclarationTreeNode

    open fun writersInfo(variableDescriptor: VariableDescriptor): WritersInfo {
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor) ?: return WritersInfo.EMPTY
        val node = declaration.parents.firstNotNullResult { declarationTreeNodes[it] } ?: rootDeclarationTreeNode
        return node.writersByName[variableDescriptor.name] ?: WritersInfo.EMPTY
    }

    fun hasWriters(variableDescriptor: VariableDescriptor) =
            !writersInfo(variableDescriptor).isEmpty()

    override fun visitDeclaration(declaration: KtDeclaration) {

        if (declaration is KtDeclarationWithBody || declaration is KtClassOrObject || declaration is KtAnonymousInitializer) {
            processDeclaration(declaration) {
                super.visitDeclaration(declaration)
            }
        }
        else {
            super.visitDeclaration(declaration)
        }

    }

    private inline fun processDeclaration(declaration: KtDeclaration, visitDeclaration: () -> Unit) {
        val previous = currentDeclaration
        val previousTreeNode = currentDeclarationTreeNode
        currentDeclaration = declaration
        currentDeclarationTreeNode = DeclarationTreeNode(declaration).also {
            declarationTreeNodes[declaration] = it
            previousTreeNode.children.add(it)
        }

        visitDeclaration()

        currentDeclarationTreeNode.updateInfoFromChildren()
        currentDeclaration = previous
        currentDeclarationTreeNode = previousTreeNode
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        processDeclaration(lambdaExpression.functionLiteral) {
            super.visitLambdaExpression(lambdaExpression)
        }
    }

    override fun visitBinaryExpression(binaryExpression: KtBinaryExpression) {
        if (binaryExpression.operationToken === KtTokens.EQ) {
            val left = KtPsiUtil.deparenthesize(binaryExpression.left)
            if (left is KtNameReferenceExpression) {
                currentDeclarationTreeNode.writersByName.getOrPut(left.getReferencedNameAsName()) { WritersInfo() }
                            .acceptAssignment(binaryExpression)
            }
        }
        super.visitBinaryExpression(binaryExpression)
    }
}
