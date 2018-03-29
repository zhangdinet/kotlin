/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.utils.createValueParameter
import org.jetbrains.kotlin.ir.backend.js.utils.isPrimary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.createValueSymbol
import org.jetbrains.kotlin.ir.util.deepCopyOld
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class SecondaryCtorLowering(val context: JsIrBackendContext) : IrElementTransformerVoid(), FileLoweringPass {

    val ctorToName = mutableMapOf<IrConstructor, String>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
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
        return declaration
    }

    private fun lowerClass(irClass: IrClass): List<IrConstructor> {


        val className = irClass.name.asString()
        val ctors = mutableListOf<IrConstructor>()

        irClass.declarations.forEachIndexed { i, declaration ->
            if (declaration is IrConstructor && !declaration.symbol.isPrimary) {
                ctors += declaration


                val ctorName = "${className}_init_$i"
                val newCtor = createCtorImpl(declaration, ctorName)

//                val descriptor = SimpleFunctionDescriptorImpl.create(
//                    declaration.descriptor,
//                    declaration.descriptor.annotations,
//                    Name.identifier(ctorName),
//                    declaration.descriptor.kind,
//                    declaration.descriptor.source
//                ).initialize(
//                    null,
//                    declaration.descriptor.dispatchReceiverParameter,
//                    declaration.descriptor.typeParameters,
//                    declaration.descriptor.valueParameters,
//                    declaration.returnType,
//                    declaration.descriptor.modality,
//                    declaration.visibility
//                )
//                val constructor = IrFunctionImpl(
//                    declaration.startOffset, declaration.endOffset,
//                    declaration.origin, descriptor,
//                    declaration.body
//                )
//
//
//                constructor.valueParameters += declaration.valueParameters
//                constructor.typeParameters += declaration.typeParameters
//                constructor.parent = declaration.parent


                ctorToName[declaration] = ctorName
            }
        }


        irClass.declarations.removeAll(ctors)

        return ctors
    }


    private fun createCtorImpl(declaration: IrConstructor, name: String): IrSimpleFunction {
        val actualName = "${name}_0"

        val paramDesc = declaration.descriptor.createValueParameter(declaration.descriptor.valueParameters.size, "\$this", declaration.descriptor.returnType!!)
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
//        descriptor.valueParameters += paramDesc

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

        val retStmt = IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            declaration.returnType,
            constructor.symbol,
            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParam.symbol)
        )

        statements += retStmt

        constructor.valueParameters += declaration.valueParameters
        constructor.typeParameters += declaration.typeParameters
        constructor.parent = declaration.parent
        constructor.valueParameters += thisParam

        constructor.body = IrBlockBodyImpl(declaration.body!!.startOffset, declaration.body!!.endOffset, statements)

        return constructor
    }

    private fun createStubCtor(declaration: IrConstructor, name: String): IrSimpleFunction {

//        val objectCreation = IrVariableImpl

        val descriptor = SimpleFunctionDescriptorImpl.create(
            declaration.descriptor,
            declaration.descriptor.annotations,
            Name.identifier(name),
            declaration.descriptor.kind,
            declaration.descriptor.source
        ).initialize(
            null,
            declaration.descriptor.dispatchReceiverParameter,
            declaration.descriptor.typeParameters,
            declaration.descriptor.valueParameters,
            declaration.returnType,
            declaration.descriptor.modality,
            declaration.visibility
        )
        val constructor = IrFunctionImpl(
            declaration.startOffset, declaration.endOffset,
            declaration.origin, descriptor,
            declaration.body
        )

        constructor.valueParameters += declaration.valueParameters
        constructor.typeParameters += declaration.typeParameters
        constructor.parent = declaration.parent

        return constructor
    }


}