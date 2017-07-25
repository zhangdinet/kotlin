/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.descriptors.SyntheticElementResolveContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.Printer

private class XmlSourceElement(override val psi: PsiElement) : PsiSourceElement

internal fun genSimpleClass(packageFragmentDescriptor: PackageFragmentDescriptor): ClassDescriptor {
    val classDescriptor = ClassDescriptorImpl(packageFragmentDescriptor, Name.identifier("Test"), Modality.FINAL, ClassKind.CLASS,
            listOf(packageFragmentDescriptor.builtIns.anyType), SourceElement.NO_SOURCE, false)

    val primaryConstructor = ClassConstructorDescriptorImpl.create(classDescriptor, Annotations.EMPTY, true, SourceElement.NO_SOURCE).apply {
        initialize(emptyList(), Visibilities.PUBLIC)
    }

    val simpleFunction = SimpleFunctionDescriptorImpl.create(classDescriptor, Annotations.EMPTY, Name.identifier("foo"),
            CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE)

    simpleFunction.initialize(null, classDescriptor.thisAsReceiverParameter, emptyList(), emptyList(),
                              packageFragmentDescriptor.builtIns.stringType, Modality.FINAL, Visibilities.PUBLIC)

    val scope = object : MemberScopeImpl() {
        override fun getContributedFunctions(name: Name, location: LookupLocation) = listOf(simpleFunction)
        override fun printScopeStructure(p: Printer) { p.println(this::class.java.simpleName) }
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
            return when {
                kindFilter.accepts(simpleFunction) && nameFilter(simpleFunction.name) -> listOf(simpleFunction)
                kindFilter.accepts(primaryConstructor) && nameFilter(primaryConstructor.name) -> listOf(primaryConstructor)
                else -> emptyList()
            }
        }
    }

    classDescriptor.initialize(scope, setOf(primaryConstructor), primaryConstructor)

    primaryConstructor.returnType = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, classDescriptor, emptyList())

    return classDescriptor
}

internal fun genClearCacheFunction(packageFragmentDescriptor: PackageFragmentDescriptor, receiverType: KotlinType): SimpleFunctionDescriptor {
    val function = object : AndroidSyntheticFunction, SimpleFunctionDescriptorImpl(
            packageFragmentDescriptor,
            null,
            Annotations.EMPTY,
            Name.identifier(AndroidConst.CLEAR_FUNCTION_NAME),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE) {}

    val unitType = packageFragmentDescriptor.builtIns.unitType
    function.initialize(receiverType, null, emptyList(), emptyList(), unitType, Modality.FINAL, Visibilities.PUBLIC)
    return function
}

internal fun genPropertyForWidget(
        packageFragmentDescriptor: AndroidSyntheticPackageFragmentDescriptor,
        receiverType: KotlinType,
        resolvedWidget: ResolvedWidget,
        context: SyntheticElementResolveContext
): PropertyDescriptor {
    val sourceEl = resolvedWidget.widget.sourceElement?.let(::XmlSourceElement) ?: SourceElement.NO_SOURCE

    val classDescriptor = resolvedWidget.viewClassDescriptor
    val type = classDescriptor?.let {
        val defaultType = classDescriptor.defaultType

        if (defaultType.constructor.parameters.isEmpty()) {
            defaultType
        }
        else {
            KotlinTypeFactory.simpleNotNullType(
                    Annotations.EMPTY, classDescriptor, defaultType.constructor.parameters.map(::StarProjectionImpl))
        }
    } ?: context.view

    return genProperty(resolvedWidget.widget, receiverType, type, packageFragmentDescriptor, sourceEl, resolvedWidget.errorType)
}

internal fun genPropertyForFragment(
        packageFragmentDescriptor: AndroidSyntheticPackageFragmentDescriptor,
        receiverType: KotlinType,
        type: SimpleType,
        fragment: AndroidResource.Fragment
): PropertyDescriptor {
    val sourceElement = fragment.sourceElement?.let(::XmlSourceElement) ?: SourceElement.NO_SOURCE
    return genProperty(fragment, receiverType, type, packageFragmentDescriptor, sourceElement, null)
}

private fun genProperty(
        resource: AndroidResource,
        receiverType: KotlinType,
        type: SimpleType,
        containingDeclaration: AndroidSyntheticPackageFragmentDescriptor,
        sourceElement: SourceElement,
        errorType: String?
): PropertyDescriptor {
    val property = object : AndroidSyntheticProperty, PropertyDescriptorImpl(
            containingDeclaration,
            null,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            false,
            Name.identifier(resource.id.name),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            sourceElement,
            /* lateInit = */ false,
            /* isConst = */ false,
            /* isHeader = */ false,
            /* isImpl = */ false,
            /* isExternal = */ false,
            /* isDelegated = */ false
    ) {
        override val errorType = errorType
        override val shouldBeCached = type.shouldBeCached
        override val resource = resource
    }

    // todo support (Mutable)List
    val flexibleType = KotlinTypeFactory.flexibleType(type, type.makeNullableAsSpecified(true))
    property.setType(
            flexibleType,
            emptyList<TypeParameterDescriptor>(),
            null,
            receiverType)

    val getter = PropertyGetterDescriptorImpl(
            property,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            /* isDefault = */ false,
            /* isExternal = */ false,
            /* isInline = */ false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            /* original = */ null,
            SourceElement.NO_SOURCE
    )

    getter.initialize(null)

    property.initialize(getter, null)

    return property
}

private val SimpleType.shouldBeCached: Boolean
    get() {
        val viewClassFqName = constructor.declarationDescriptor?.fqNameUnsafe?.asString() ?: return false
        return viewClassFqName != AndroidConst.VIEWSTUB_FQNAME
    }

interface AndroidSyntheticFunction

interface AndroidSyntheticProperty {
    val resource: AndroidResource

    val errorType: String?

    // True if the View should be cached.
    // Some views (such as ViewStub) should not be cached.
    val shouldBeCached: Boolean
}

val AndroidSyntheticProperty.isErrorType: Boolean
    get() = errorType != null