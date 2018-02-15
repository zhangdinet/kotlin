/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.LocalVariableResolver
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class DestructuringDeclarationResolver(
    private val fakeCallResolver: FakeCallResolver,
    private val localVariableResolver: LocalVariableResolver,
    private val typeResolver: TypeResolver
) {
    fun resolveLocalVariablesFromDestructuringDeclaration(
        scope: LexicalScope,
        destructuringDeclaration: KtDestructuringDeclaration,
        receiver: ReceiverValue?,
        initializer: KtExpression?,
        context: ExpressionTypingContext
    ): List<VariableDescriptor> {
        val result = arrayListOf<VariableDescriptor>()
        for ((componentIndex, entry) in destructuringDeclaration.entries.withIndex()) {
            val componentType = resolveInitializer(entry, receiver, initializer, context, componentIndex)
            val variableDescriptor =
                localVariableResolver.resolveLocalVariableDescriptorWithType(scope, entry, componentType, context.trace)

            result.add(variableDescriptor)
        }

        return result
    }

    fun defineLocalVariablesFromDestructuringDeclaration(
        writableScope: LexicalWritableScope,
        destructuringDeclaration: KtDestructuringDeclaration,
        receiver: ReceiverValue?,
        initializer: KtExpression?,
        context: ExpressionTypingContext
    ) = resolveLocalVariablesFromDestructuringDeclaration(
        writableScope, destructuringDeclaration, receiver, initializer, context
    ).forEach {
        ExpressionTypingUtils.checkVariableShadowing(writableScope, context.trace, it)
        writableScope.addVariableDescriptor(it)
    }

    fun resolveInitializer(
        entry: KtDestructuringDeclarationEntry,
        receiver: ReceiverValue?,
        initializer: KtExpression?,
        context: ExpressionTypingContext,
        componentIndex: Int
    ): KotlinType {
        val componentName = DataClassDescriptorResolver.createComponentName(componentIndex + 1)
        return resolveComponentFunctionAndGetType(componentName, context, entry, receiver, initializer)
    }

    private fun resolveComponentFunctionAndGetType(
        componentName: Name,
        context: ExpressionTypingContext,
        entry: KtDestructuringDeclarationEntry,
        receiver: ReceiverValue?,
        initializer: KtExpression?
    ): KotlinType {
        fun errorType() = ErrorUtils.createErrorType("$componentName() return type")

        if (receiver == null) return errorType()

        val expectedType = getExpectedTypeForComponent(context, entry)
        val newContext = context.replaceExpectedType(expectedType).replaceContextDependency(ContextDependency.INDEPENDENT)
        val results = fakeCallResolver.resolveFakeCall(
            newContext, receiver, componentName,
            entry, initializer ?: entry, FakeCallKind.COMPONENT, emptyList()
        )

        if (!results.isSingleResult) {
            return errorType()
        }

        context.trace.record(BindingContext.COMPONENT_RESOLVED_CALL, entry, results.resultingCall)

        val functionReturnType = results.resultingDescriptor.returnType
        if (functionReturnType != null && !TypeUtils.noExpectedType(expectedType)
            && !KotlinTypeChecker.DEFAULT.isSubtypeOf(functionReturnType, expectedType)) {
            context.trace.report(
                Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.on(
                    initializer ?: entry, componentName, functionReturnType, expectedType
                )
            )
        }
        return functionReturnType ?: errorType()
    }

    private fun getExpectedTypeForComponent(context: ExpressionTypingContext, entry: KtDestructuringDeclarationEntry): KotlinType {
        val entryTypeRef = entry.typeReference ?: return TypeUtils.NO_EXPECTED_TYPE
        return typeResolver.resolveType(context.scope, entryTypeRef, context.trace, true)
    }
}
