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
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class SecondaryCtorLowering(val context: JsIrBackendContext) : IrElementTransformerVoid(), FileLoweringPass {


    data class Constructors(val delegate: IrSimpleFunction, val stub: IrSimpleFunction)

    val ctorToName = mutableMapOf<IrConstructor, Constructors>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    inner class CallsiteFixer : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement = super.visitFunction(declaration, declaration)

        override fun visitCall(expression: IrCall, ownerFunc: IrFunction?): IrElement {
            val target = expression.symbol.owner as IrFunction

            if (target is IrConstructor) {
                if (!target.descriptor.isPrimary) {
                    return redirectCall(expression, ctorToName[target]!!.stub)
                }
            }

            return expression
        }

        private fun redirectCall(
            call: IrFunctionAccessExpression,
            newTarget: IrSimpleFunction
        ): IrCallImpl {
            val newCall = IrCallImpl(
                call.startOffset, call.endOffset,
                newTarget.symbol, newTarget.descriptor
            )

            newCall.copyTypeArgumentsFrom(call)

            for (i in 0 until call.valueArgumentsCount) {
                newCall.putValueArgument(i, call.getValueArgument(i))
            }

            return newCall
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, ownerFunc: IrFunction?): IrElement {
            val target = expression.symbol.owner
            if (target.descriptor.isPrimary) {
                // nothing to do here
                return expression
            }

            val fromPrimary = ownerFunc!! is IrConstructor
            val newCall = redirectCall(expression, ctorToName[target]!!.delegate)

            val readThis = if (fromPrimary) {
                IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    LazyClassReceiverParameterDescriptor(target.descriptor.containingDeclaration)
                )
            } else {
                IrGetValueImpl(expression.startOffset, expression.endOffset, ownerFunc.valueParameters.last().symbol)
            }

            newCall.putValueArgument(expression.valueArgumentsCount, readThis)

            return newCall
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        var i = 0
        while (i < declaration.declarations.size) {
            val decl = declaration.declarations[i++]
            if (decl is IrClass) {
                val ctors = lowerClass(decl)
                declaration.declarations.addAll(i, ctors)
                i += ctors.size
            }
        }

        declaration.transformChildren(CallsiteFixer(), null)

        return declaration
    }

    private fun lowerClass(irClass: IrClass): List<IrSimpleFunction> {


        val className = irClass.name.asString()
        val ctors = mutableListOf<IrConstructor>()
        val newCtors = mutableListOf<IrSimpleFunction>()

        irClass.declarations.forEachIndexed { i, declaration ->
            if (declaration is IrConstructor && !declaration.symbol.isPrimary) {
                ctors += declaration


                val ctorName = "${className}_init_$i"
                val newCtorDelegate = createCtorDeligate(declaration, ctorName)
                val newCtorStub = createStubCtor(declaration, newCtorDelegate, ctorName)

                ctorToName[declaration] = Constructors(newCtorDelegate, newCtorStub)
                newCtors += newCtorDelegate
                newCtors += newCtorStub
            }
        }

        irClass.declarations.removeAll(ctors)

        return newCtors
    }

    inner class RetAndThisReplacer(val function: IrFunctionSymbol, val thisParam: IrValueParameter) : IrElementTransformerVoid() {

        override fun visitReturn(expression: IrReturn): IrExpression = IrReturnImpl(
            expression.startOffset,
            expression.endOffset,
            function,
            IrGetValueImpl(
                expression.startOffset,
                expression.endOffset,
                thisParam.symbol
            )
        )

        override fun visitGetValue(expression: IrGetValue): IrExpression =
            if (expression.descriptor.name.isSpecial && expression.descriptor.name.asString() == Namer.THIS_SPECIAL_NAME) IrGetValueImpl(
                expression.startOffset,
                expression.endOffset,
                thisParam.symbol,
                expression.origin
            ) else {
                expression
            }

    }

    private val synthetic_this = "\$this"

    private fun createCtorDeligate(declaration: IrConstructor, name: String): IrSimpleFunction {
        val actualName = "${name}_delegate"

        val paramDesc = ValueParameterDescriptorImpl(
            declaration.descriptor,
            null,
            declaration.descriptor.valueParameters.size,
            Annotations.EMPTY,
            Name.identifier(synthetic_this),
            declaration.descriptor.returnType,
            false,
            false,
            false,
            null,
            SourceElement.NO_SOURCE
        )

        val descriptor = SimpleFunctionDescriptorImpl.create(
            declaration.descriptor,
            declaration.descriptor.annotations,
            Name.identifier(actualName),
            declaration.descriptor.kind,
            declaration.descriptor.source
        ).initialize(
            null,
            declaration.descriptor.dispatchReceiverParameter,
            declaration.descriptor.typeParameters,
            declaration.descriptor.valueParameters + paramDesc,
            declaration.returnType,
            declaration.descriptor.modality,
            declaration.visibility
        )
        val thisParam = IrValueParameterImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            JsLoweredDeclarationOrigin.SECONDARY_CTOR_RECEIVER,
            paramDesc
        )


        val constructor = IrFunctionImpl(
            declaration.startOffset, declaration.endOffset,
            declaration.origin, descriptor
        )

        var statements = (declaration.body as IrStatementContainer).statements.map { it.deepCopyWithSymbols() }
        val fixer = RetAndThisReplacer(constructor.symbol, thisParam)
        for (stmt in statements) {
            stmt.transformChildrenVoid(fixer)
        }

        val retStmt = IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            constructor.symbol,
            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParam.symbol)
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
        val descriptor = SimpleFunctionDescriptorImpl.create(
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


        val constructor = IrFunctionImpl(
            ctorOrig.startOffset, ctorOrig.endOffset,
            ctorOrig.origin, descriptor
        )

        val createFunctionIntrinsic = context.objectCreate
        val irBuilder = context.createIrBuilder(constructor.symbol, ctorOrig.startOffset, ctorOrig.endOffset).irBlockBody {
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