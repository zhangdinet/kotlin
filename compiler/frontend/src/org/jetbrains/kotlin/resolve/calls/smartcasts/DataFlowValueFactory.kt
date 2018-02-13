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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableNothing
import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.before
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.AssignedVariablesSearcher.Writer
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor
import org.jetbrains.kotlin.types.isError

/**
 * This class is intended to create data flow values for different kind of expressions.
 * Then data flow values serve as keys to obtain data flow information for these expressions.
 */
interface DataFlowValueFactory {
    fun createDataFlowValue(
        expression: KtExpression,
        type: KotlinType,
        resolutionContext: ResolutionContext<*>
    ): DataFlowValue

    fun createDataFlowValue(
        expression: KtExpression,
        type: KotlinType,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue

    fun createDataFlowValueForStableReceiver(receiver: ReceiverValue): DataFlowValue

    fun createDataFlowValue(
        receiverValue: ReceiverValue,
        resolutionContext: ResolutionContext<*>
    ): DataFlowValue

    fun createDataFlowValue(
        receiverValue: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue

    fun createDataFlowValueForProperty(
        property: KtProperty,
        variableDescriptor: VariableDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue

}

class DataFlowValueFactoryImpl : DataFlowValueFactory {

    // Receivers
    override fun createDataFlowValue(
        receiverValue: ReceiverValue,
        resolutionContext: ResolutionContext<*>
    ) = createDataFlowValue(receiverValue, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    override fun createDataFlowValue(
        receiverValue: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ) = when (receiverValue) {
        is TransientReceiver, is ImplicitReceiver -> createDataFlowValueForStableReceiver(receiverValue)
        is ExpressionReceiver -> createDataFlowValue(
            receiverValue.expression,
            receiverValue.getType(),
            bindingContext,
            containingDeclarationOrModule
        )
        else -> throw UnsupportedOperationException("Unsupported receiver value: " + receiverValue::class.java.name)
    }

    override fun createDataFlowValueForStableReceiver(receiver: ReceiverValue) =
        DataFlowValue(IdentifierInfo.Receiver(receiver), receiver.type)


    // Property
    override fun createDataFlowValueForProperty(
        property: KtProperty,
        variableDescriptor: VariableDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue {
        val identifierInfo = IdentifierInfo.Variable(
            variableDescriptor,
            variableKind(variableDescriptor, usageContainingModule, bindingContext, property),
            bindingContext[BOUND_INITIALIZER_VALUE, variableDescriptor]
        )
        return DataFlowValue(identifierInfo, variableDescriptor.type)
    }


    // Expressions
    override fun createDataFlowValue(
        expression: KtExpression,
        type: KotlinType,
        resolutionContext: ResolutionContext<*>
    ) = createDataFlowValue(expression, type, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    override fun createDataFlowValue(
        expression: KtExpression,
        type: KotlinType,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue {
        return when {
            expression is KtConstantExpression && expression.node.elementType === KtNodeTypes.NULL ->
                DataFlowValue.nullValue(containingDeclarationOrModule.builtIns)

            type.isError -> DataFlowValue.ERROR

            isNullableNothing(type) ->
                DataFlowValue.nullValue(containingDeclarationOrModule.builtIns) // 'null' is the only inhabitant of 'Nothing?'

            // In most cases type of `E!!`-expression is strictly not nullable and we could get proper Nullability
            // by calling `getImmanentNullability` (as it happens below).
            //
            // But there are some problem with types built on type parameters, e.g.
            // fun <T : Any?> foo(x: T) = x!!.hashCode() // there no way in type system to denote that `x!!` is not nullable
            ExpressionTypingUtils.isExclExclExpression(KtPsiUtil.deparenthesize(expression)) ->
                DataFlowValue(IdentifierInfo.Expression(expression), type, Nullability.NOT_NULL)

            isComplexExpression(expression) ->
                DataFlowValue(IdentifierInfo.Expression(expression, stableComplex = true), type)

            else -> {
                val result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule)
                DataFlowValue(
                    if (result === IdentifierInfo.NO) IdentifierInfo.Expression(expression) else result, type
                )
            }
        }
    }

    private fun isComplexExpression(expression: KtExpression): Boolean = when (expression) {
        is KtBlockExpression, is KtIfExpression, is KtWhenExpression -> true
        is KtBinaryExpression -> expression.operationToken === KtTokens.ELVIS
        is KtParenthesizedExpression -> {
            val deparenthesized = KtPsiUtil.deparenthesize(expression)
            deparenthesized != null && isComplexExpression(deparenthesized)
        }
        else -> false
    }
}
