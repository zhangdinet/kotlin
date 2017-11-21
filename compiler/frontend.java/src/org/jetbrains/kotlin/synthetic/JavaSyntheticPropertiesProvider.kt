/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.synthetic

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import java.util.*
import kotlin.properties.Delegates

fun canBePropertyAccessor(identifier: String): Boolean {
    return identifier.startsWith("get") || identifier.startsWith("is") || identifier.startsWith("set")
}

interface SyntheticJavaPropertyDescriptor : SyntheticPropertyDescriptor {
    val getMethod: FunctionDescriptor
    val setMethod: FunctionDescriptor?

    companion object {
        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, syntheticScopes: SyntheticScopes): SyntheticJavaPropertyDescriptor? {
            val name = getterOrSetter.name
            if (name.isSpecial) return null
            val identifier = name.identifier
            if (!canBePropertyAccessor(identifier)) return null  // optimization

            val classDescriptorOwner = getterOrSetter.containingDeclaration as? ClassDescriptor ?: return null
            val originalGetterOrSetter = getterOrSetter.original

            val scope = syntheticScopes.provideSyntheticScope(
                    classDescriptorOwner.defaultType.memberScope,
                    SyntheticScopesMetadata(needExtensionProperties = true)
            )
            return scope.getContributedDescriptors(DescriptorKindFilter.VARIABLES).filterIsInstance<SyntheticJavaPropertyDescriptor>()
                    .firstOrNull { originalGetterOrSetter == it.getMethod || originalGetterOrSetter == it.setMethod }
        }

        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, syntheticScopeProvider: SyntheticScopeProvider) =
                findByGetterOrSetter(getterOrSetter,
                                     object : SyntheticScopes {
                                         override val scopeProviders: Collection<SyntheticScopeProvider> = listOf(syntheticScopeProvider)
                                     })

        fun propertyNameByGetMethodName(methodName: Name): Name?
                = org.jetbrains.kotlin.load.java.propertyNameByGetMethodName(methodName)

        fun propertyNameBySetMethodName(methodName: Name, withIsPrefix: Boolean): Name?
                = org.jetbrains.kotlin.load.java.propertyNameBySetMethodName(methodName, withIsPrefix)
    }
}

class JavaSyntheticPropertiesScope(
        storageManager: StorageManager,
        override val wrappedScope: ResolutionScope
) : SyntheticResolutionScope() {
    private val variables = storageManager.createMemoizedFunction<Name, Collection<VariableDescriptor>> {
        super.getContributedVariables(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE) + listOfNotNull(doGetProperty(it))
    }
    private val descriptors = storageManager.createLazyValue {
        doGetDescriptors()
    }

    private fun doGetOwnerClass(type: KotlinType) = type.constructor.declarationDescriptor as? ClassDescriptor

    private fun doGetDescriptors(): List<VariableDescriptor> =
            super.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, MemberScope.ALL_NAME_FILTER)
                    .filterIsInstance<FunctionDescriptor>()
                    .mapNotNull {
                        val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(it.name) ?: return@mapNotNull null
                        getContributedVariables(propertyName, NoLookupLocation.FROM_SYNTHETIC_SCOPE).singleOrNull()
                    }

    private fun doGetProperty(name: Name): PropertyDescriptor? {
        if (name.isSpecial) return null
        val identifier = name.identifier
        if (identifier.isEmpty()) return null
        val firstChar = identifier[0]
        if (!firstChar.isJavaIdentifierStart() || firstChar in 'A'..'Z') return null

        val possibleGetters = possibleGetMethodNames(name)
        val getter = possibleGetters
                             .flatMap { super.getContributedFunctions(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE) }
                             .singleOrNull { it.hasJavaOriginInHierarchy() && isGoodGetMethod(it) } ?: return null
        if (getter.containingDeclaration !is ClassDescriptor) return null

        val setterName = setMethodName(getter.name)
        val setter = super.getContributedFunctions(setterName, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
                .singleOrNull { isGoodSetMethod(it, getter) }

        val type = getter.returnType!!
        return MyPropertyDescriptor.create(getter.containingDeclaration as ClassDescriptor, getter, setter, name, type)
    }

    private fun possibleGetMethodNames(propertyName: Name): List<Name> {
        val result = ArrayList<Name>(3)
        val identifier = propertyName.identifier

        if (JvmAbi.startsWithIsPrefix(identifier)) {
            result.add(propertyName)
        }

        val capitalize1 = identifier.capitalizeAsciiOnly()
        val capitalize2 = identifier.capitalizeFirstWord(asciiOnly = true)
        result.add(Name.identifier("get" + capitalize1))
        if (capitalize2 != capitalize1) {
            result.add(Name.identifier("get" + capitalize2))
        }

        return result
                .filter { SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(it) == propertyName } // don't accept "uRL" for "getURL" etc
    }

    private fun isGoodGetMethod(descriptor: FunctionDescriptor): Boolean {
        val returnType = descriptor.returnType ?: return false
        if (returnType.isUnit()) return false

        return descriptor.valueParameters.isEmpty()
               && descriptor.typeParameters.isEmpty()
               && descriptor.visibility.isVisibleOutside()
    }

    private fun isGoodSetMethod(descriptor: FunctionDescriptor, getMethod: FunctionDescriptor): Boolean {
        val propertyType = getMethod.returnType ?: return false
        val parameter = descriptor.valueParameters.singleOrNull() ?: return false
        if (!TypeUtils.equalTypes(parameter.type, propertyType)) {
            if (!propertyType.isSubtypeOf(parameter.type)) return false
        }

        return parameter.varargElementType == null
               && descriptor.typeParameters.isEmpty()
               && descriptor.visibility.isVisibleOutside()
    }

    private fun setMethodName(getMethodName: Name): Name {
        val identifier = getMethodName.identifier
        val prefix = when {
            identifier.startsWith("get") -> "get"
            identifier.startsWith("is") -> "is"
            else -> throw IllegalArgumentException()
        }
        return Name.identifier("set" + identifier.removePrefix(prefix))
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        recordLookup(name, location)
        return variables(name)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return super.getContributedDescriptors(kindFilter, nameFilter) +
               if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) descriptors()
               else emptyList()
    }

    private class MyPropertyDescriptor(
            containingDeclaration: DeclarationDescriptor,
            original: PropertyDescriptor?,
            annotations: Annotations,
            modality: Modality,
            visibility: Visibility,
            isVar: Boolean,
            name: Name,
            kind: CallableMemberDescriptor.Kind,
            source: SourceElement
    ) : SyntheticJavaPropertyDescriptor, PropertyDescriptorImpl(
            containingDeclaration, original, annotations, modality, visibility, isVar, name, kind, source,
            /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false,
            /* isDelegated = */ false
    ) {

        override var getMethod: FunctionDescriptor by Delegates.notNull()
            private set

        override var setMethod: FunctionDescriptor? = null
            private set

        companion object {
            fun create(ownerClass: ClassDescriptor, getMethod: FunctionDescriptor, setMethod: FunctionDescriptor?, name: Name, type: KotlinType): MyPropertyDescriptor {
                val visibility = syntheticVisibility(getMethod, isUsedForExtension = true)
                val descriptor = MyPropertyDescriptor(DescriptorUtils.getContainingModule(ownerClass),
                                                      null,
                                                      Annotations.EMPTY,
                                                      Modality.FINAL,
                                                      visibility,
                                                      setMethod != null,
                                                      name,
                                                      CallableMemberDescriptor.Kind.SYNTHESIZED,
                                                      SourceElement.NO_SOURCE)
                descriptor.getMethod = getMethod
                descriptor.setMethod = setMethod

                val classTypeParams = ownerClass.typeConstructor.parameters
                val typeParameters = ArrayList<TypeParameterDescriptor>(classTypeParams.size)
                val typeSubstitutor = DescriptorSubstitutor.substituteTypeParameters(classTypeParams, TypeSubstitution.EMPTY, descriptor, typeParameters)

                val propertyType = typeSubstitutor.safeSubstitute(type, Variance.INVARIANT)
                val receiverType = typeSubstitutor.safeSubstitute(ownerClass.defaultType, Variance.INVARIANT)
                descriptor.setType(propertyType, typeParameters, null, receiverType)

                val getter = PropertyGetterDescriptorImpl(descriptor,
                                                          getMethod.annotations,
                                                          Modality.FINAL,
                                                          visibility,
                                                          false,
                                                          getMethod.isExternal,
                                                          false,
                                                          CallableMemberDescriptor.Kind.SYNTHESIZED,
                                                          null,
                                                          SourceElement.NO_SOURCE)
                getter.initialize(null)

                val setter = if (setMethod != null)
                    PropertySetterDescriptorImpl(descriptor,
                                                 setMethod.annotations,
                                                 Modality.FINAL,
                                                 syntheticVisibility(setMethod, isUsedForExtension = true),
                                                 false,
                                                 setMethod.isExternal,
                                                 false,
                                                 CallableMemberDescriptor.Kind.SYNTHESIZED,
                                                 null,
                                                 SourceElement.NO_SOURCE)
                else
                    null
                setter?.initializeDefault()

                descriptor.initialize(getter, setter)

                return descriptor
            }
        }

        override fun createSubstitutedCopy(
                newOwner: DeclarationDescriptor,
                newModality: Modality,
                newVisibility: Visibility,
                original: PropertyDescriptor?,
                kind: CallableMemberDescriptor.Kind,
                newName: Name
        ): PropertyDescriptorImpl {
            return MyPropertyDescriptor(newOwner, this, annotations, newModality, newVisibility, isVar, newName, kind, source).apply {
                getMethod = this@MyPropertyDescriptor.getMethod
                setMethod = this@MyPropertyDescriptor.setMethod
            }
        }

        override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor? {
            val descriptor = super.substitute(substitutor) as MyPropertyDescriptor? ?: return null
            if (descriptor == this) return descriptor

            val classTypeParameters = (getMethod.containingDeclaration as ClassDescriptor).typeConstructor.parameters
            val substitutionMap = HashMap<TypeConstructor, TypeProjection>()
            for ((typeParameter, classTypeParameter) in typeParameters.zip(classTypeParameters)) {
                val typeProjection = substitutor.substitution[typeParameter.defaultType] ?: continue
                substitutionMap[classTypeParameter.typeConstructor] = typeProjection

            }
            val classParametersSubstitutor = TypeConstructorSubstitution.createByConstructorsMap(
                    substitutionMap,
                    approximateCapturedTypes = true
            ).buildSubstitutor()

            descriptor.getMethod = getMethod.substitute(classParametersSubstitutor) ?: return null
            descriptor.setMethod = setMethod?.substitute(classParametersSubstitutor)
            return descriptor
        }
    }
}

class JavaSyntheticPropertiesProvider(private val storageManager: StorageManager) : SyntheticScopeProvider {
    private val makeSynthetic = storageManager.createMemoizedFunction<ResolutionScope, ResolutionScope> {
        JavaSyntheticPropertiesScope(storageManager, it)
    }

    override fun provideSyntheticScope(scope: ResolutionScope, metadata: SyntheticScopesMetadata): ResolutionScope {
        if (!metadata.needExtensionProperties) return scope
        return makeSynthetic(scope)
    }
}

