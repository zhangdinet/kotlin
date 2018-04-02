/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.isPrimary
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class SecondaryCtorLowering(val context: JsIrBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    val oldCtorToNewMap = mutableMapOf<IrConstructorSymbol, JsIrBackendContext.ConstructorsPair>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
        context.secondaryConstructorsMap.putAll(oldCtorToNewMap)
    }

    class CallsiteRedirectionTransformer(val context: JsIrBackendContext) : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement = super.visitFunction(declaration, declaration)

        override fun visitCall(expression: IrCall, ownerFunc: IrFunction?): IrElement {
            val target = expression.symbol.owner as IrFunction

            if (target is IrConstructor) {
                if (!target.descriptor.isPrimary) {
                    return redirectCall(expression, context.secondaryConstructorsMap[target.symbol]!!.stub)
                }
            }

            return expression
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, ownerFunc: IrFunction?): IrElement {
            val target = expression.symbol.owner
            if (target.descriptor.isPrimary) {
                // nothing to do here
                return expression
            }

            val fromPrimary = ownerFunc!! is IrConstructor
            val newCall = redirectCall(expression, context.secondaryConstructorsMap[target.symbol]!!.delegate)

            val readThis = if (fromPrimary) {
                IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    IrValueParameterSymbolImpl(LazyClassReceiverParameterDescriptor(target.descriptor.containingDeclaration))
                )
            } else {
                IrGetValueImpl(expression.startOffset, expression.endOffset, ownerFunc.valueParameters.last().symbol)
            }

            newCall.putValueArgument(expression.valueArgumentsCount, readThis)

            return newCall
        }

        private fun redirectCall(
            call: IrFunctionAccessExpression,
            newTarget: IrSimpleFunctionSymbol
        ): IrCallImpl {
            val newCall = IrCallImpl(call.startOffset, call.endOffset, newTarget)

            newCall.copyTypeArgumentsFrom(call)

            for (i in 0 until call.valueArgumentsCount) {
                newCall.putValueArgument(i, call.getValueArgument(i))
            }

            return newCall
        }
    }

    override fun visitFile(irFile: IrFile): IrFile {
        var i = 0
        while (i < irFile.declarations.size) {
            val declaration = irFile.declarations[i++]
            if (declaration is IrClass) {
                val constructors = lowerClass(declaration)
                irFile.declarations.addAll(i, constructors)
                i += constructors.size
            }
        }

        return irFile
    }

    private fun lowerClass(irClass: IrClass): List<IrSimpleFunction> {
        val className = irClass.name.asString()
        val oldConstructors = mutableListOf<IrConstructor>()
        val newConstructors = mutableListOf<IrSimpleFunction>()

        var i = 0

        for (declaration in irClass.declarations) {
            if (declaration is IrConstructor && !declaration.symbol.isPrimary) {
                val constructorName = "${className}_init_${i++}"
                val newDelegateConstructor = createCtorDeligate(declaration, constructorName)
                val newStubConstructor = createStubCtor(declaration, newDelegateConstructor, constructorName)

                oldCtorToNewMap[declaration.symbol] =
                        JsIrBackendContext.ConstructorsPair(newDelegateConstructor.symbol, newStubConstructor.symbol)

                oldConstructors += declaration
                newConstructors += newDelegateConstructor
                newConstructors += newStubConstructor
            }
        }

        irClass.declarations.removeAll(oldConstructors)

        return newConstructors
    }

    private class ThisUsageReplaceTransformer(val function: IrFunctionSymbol, val thisSymbol: IrValueParameterSymbol) :
        IrElementTransformerVoid() {

        override fun visitReturn(expression: IrReturn): IrExpression = IrReturnImpl(
            expression.startOffset,
            expression.endOffset,
            function,
            IrGetValueImpl(expression.startOffset, expression.endOffset, thisSymbol)
        )

        override fun visitGetValue(expression: IrGetValue): IrExpression =
            if (expression.descriptor.name.isSpecial && expression.descriptor.name.asString() == Namer.THIS_SPECIAL_NAME) IrGetValueImpl(
                expression.startOffset,
                expression.endOffset,
                thisSymbol,
                expression.origin
            ) else {
                expression
            }
    }

    private val SYNTHETIC_THIS_NAME = "\$this"

    private fun createCtorDeligate(declaration: IrConstructor, name: String): IrSimpleFunction {
        val actualName = "${name}_delegate"

        // TODO: get rid of descriptors and replace them with direct symbol creation
        val thisParamDesc = ValueParameterDescriptorImpl(
            declaration.descriptor,
            null,
            declaration.descriptor.valueParameters.size,
            Annotations.EMPTY,
            Name.identifier(SYNTHETIC_THIS_NAME),
            declaration.descriptor.returnType,
            false,
            false,
            false,
            null,
            SourceElement.NO_SOURCE
        )

        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            declaration.descriptor,
            declaration.descriptor.annotations,
            Name.identifier(actualName),
            declaration.descriptor.kind,
            declaration.descriptor.source
        ).initialize(
            null,
            declaration.descriptor.dispatchReceiverParameter,
            declaration.descriptor.typeParameters,
            declaration.descriptor.valueParameters + thisParamDesc,
            declaration.returnType,
            declaration.descriptor.modality,
            declaration.visibility
        )
        // TODO: ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


        val thisSymbol = IrValueParameterSymbolImpl(thisParamDesc)
        val functionSymbol = IrSimpleFunctionSymbolImpl(functionDescriptor)

        val thisParam = IrValueParameterImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            JsLoweredDeclarationOrigin.SECONDARY_CTOR_RECEIVER,
            thisSymbol
        )

        val constructor = IrFunctionImpl(
            declaration.startOffset, declaration.endOffset,
            declaration.origin, functionSymbol
        )

        var statements = (declaration.body as IrStatementContainer).statements.map { it.deepCopyWithSymbols() }
        val fixer = ThisUsageReplaceTransformer(functionSymbol, thisSymbol)
        for (stmt in statements) {
            stmt.transformChildrenVoid(fixer)
        }

        val retStmt = IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            functionSymbol,
            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisSymbol)
        )

        statements += retStmt
        val newBody = IrBlockBodyImpl(declaration.body!!.startOffset, declaration.body!!.endOffset, statements)

        constructor.valueParameters += declaration.valueParameters
        constructor.typeParameters += declaration.typeParameters
        constructor.parent = declaration.parent
        constructor.valueParameters += thisParam
        constructor.body = newBody

        return constructor
    }


    private fun createStubCtor(ctorOrig: IrConstructor, ctorImpl: IrSimpleFunction, name: String): IrSimpleFunction {
        //TODO: descriptor -> symbol
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            ctorOrig.descriptor,
            ctorOrig.descriptor.annotations,
            Name.identifier(name),
            ctorOrig.descriptor.kind,
            ctorOrig.descriptor.source
        ).initialize(
            null,
            ctorOrig.descriptor.dispatchReceiverParameter,
            ctorOrig.descriptor.typeParameters,
            ctorOrig.descriptor.valueParameters,
            ctorOrig.returnType,
            ctorOrig.descriptor.modality,
            ctorOrig.visibility
        )
        //TODO: ^^^^^^^^^^^^^^^^^^^^^^

        val functionSymbol = IrSimpleFunctionSymbolImpl(functionDescriptor)

        val constructor = IrFunctionImpl(
            ctorOrig.startOffset, ctorOrig.endOffset,
            ctorOrig.origin, functionSymbol
        )

        val createFunctionIntrinsic = context.objectCreate
        val irBuilder = context.createIrBuilder(functionSymbol, ctorOrig.startOffset, ctorOrig.endOffset).irBlockBody {
            val thisVar = irTemporaryVar(
                IrCallImpl(
                    startOffset,
                    endOffset,
                    ctorOrig.returnType,
                    createFunctionIntrinsic.symbol,
                    createFunctionIntrinsic.descriptor,
                    mapOf(createFunctionIntrinsic.typeParameters[0].descriptor to ctorOrig.returnType)
                )
            )
            +irReturn(
                irCall(ctorImpl.symbol).apply {
                    ctorOrig.valueParameters.forEachIndexed { index, irValueParameter ->
                        putValueArgument(index, irGet(irValueParameter.symbol))
                    }
                    putValueArgument(ctorOrig.valueParameters.size, irGet(thisVar.symbol))
                }
            )
        }

        constructor.valueParameters += ctorOrig.valueParameters
        constructor.typeParameters += ctorOrig.typeParameters
        constructor.parent = ctorOrig.parent
        constructor.body = IrBlockBodyImpl(ctorOrig.body?.startOffset!!, ctorOrig.body?.endOffset!!, irBuilder.statements)

        return constructor
    }


}