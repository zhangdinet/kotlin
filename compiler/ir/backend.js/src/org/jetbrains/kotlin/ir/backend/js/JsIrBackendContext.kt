/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.Variance

class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment,
    val symbolTable: SymbolTable
) : CommonBackendContext {
    override val builtIns = module.builtIns
    override val sharedVariablesManager = JsSharedVariablesManager(builtIns)

    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // TODO
        ReflectionTypes(module, FqName("kotlin.reflect"))
    }

    override val ir: Ir<CommonBackendContext> = object : Ir<CommonBackendContext>(this, irModuleFragment) {
        override val symbols: Symbols<CommonBackendContext> = object : Symbols<CommonBackendContext>(this@JsIrBackendContext, symbolTable) {

            override fun calc(initializer: () -> IrClassSymbol): IrClassSymbol {
                return object : IrClassSymbol {
                    override val owner: IrClass get() = TODO("not implemented")
                    override val isBound: Boolean get() = TODO("not implemented")
                    override fun bind(owner: IrClass) = TODO("not implemented")
                    override val descriptor: ClassDescriptor get() = TODO("not implemented")
                }
            }

            override val areEqual
                get () = TODO("not implemented")

            override val ThrowNullPointerException
                get () = TODO("not implemented")

            override val ThrowNoWhenBranchMatchedException
                get () = TODO("not implemented")

            override val ThrowTypeCastException
                get () = TODO("not implemented")

            override val ThrowUninitializedPropertyAccessException = symbolTable.referenceSimpleFunction(
                irBuiltIns.defineOperator(
                    "throwUninitializedPropertyAccessException",
                    builtIns.nothingType,
                    listOf(builtIns.stringType)
                ).descriptor
            )

            override val stringBuilder
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coroutineImpl: IrClassSymbol
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    val objectCreate: IrSimpleFunction = defineObjectCreateIntrinsic()

    private val OBJECT_CREATE_INTRINSIC_NAME = "Object.create"
    private val stubBuilder = DeclarationStubGenerator(symbolTable, JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB)
    private fun defineObjectCreateIntrinsic(): IrSimpleFunction {

        val typeParam = TypeParameterDescriptorImpl.createWithDefaultBound(
            builtIns.any,
            Annotations.EMPTY,
            true,
            Variance.INVARIANT,
            Name.identifier("T"),
            0
        )

        val returnType = KotlinTypeFactory.simpleType(Annotations.EMPTY, typeParam.typeConstructor, emptyList(), false)

        val desc = SimpleFunctionDescriptorImpl.create(
            module,
            Annotations.EMPTY,
            Name.identifier(OBJECT_CREATE_INTRINSIC_NAME),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            initialize(null, null, listOf(typeParam), emptyList(), returnType, Modality.FINAL, Visibilities.PUBLIC)
            isInline = true
        }

        return stubBuilder.generateFunctionStub(desc)
    }

    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        return find(memberScope, Name.identifier(className))
    }

    private fun find(memberScope: MemberScope, name: Name): ClassDescriptor {
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    }

    override fun getInternalClass(name: String): ClassDescriptor {
        return find(module.getPackage(FqName("kotlin.js")).memberScope, name)
    }

    override fun getClass(fqName: FqName): ClassDescriptor {
        return find(module.getPackage(fqName.parent()).memberScope, fqName.shortName())
    }

    override fun getInternalFunctions(name: String): List<FunctionDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun log(message: () -> String) {
        /*TODO*/
        print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }

}