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
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
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
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.properties.Delegates

fun canBePropertyAccessor(identifier: String): Boolean {
    return identifier.startsWith("get") || identifier.startsWith("is") || identifier.startsWith("set")
}

interface SyntheticJavaPropertyDescriptor : SyntheticPropertyDescriptor {
    val getMethod: FunctionDescriptor
    val setMethod: FunctionDescriptor?

    companion object {
        // TODO: Find a way to remove this
        fun findByGetterOrSetter(getterOrSetter: FunctionDescriptor, syntheticScopes: SyntheticScopes): SyntheticJavaPropertyDescriptor? {
            val name = getterOrSetter.name
            if (name.isSpecial) return null
            val identifier = name.identifier
            if (!canBePropertyAccessor(identifier)) return null  // optimization

            val classDescriptorOwner = getterOrSetter.containingDeclaration as? ClassDescriptor ?: return null
            val originalGetterOrSetter = getterOrSetter.original

            val scope = syntheticScopes.provideSyntheticScope(
                    classDescriptorOwner.defaultType.memberScope,
                    SyntheticScopesRequirements(needExtensionProperties = true)
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

open class JavaSyntheticPropertiesScope(
        storageManager: StorageManager,
        private val lookupTracker: LookupTracker,
        override val workerScope: ResolutionScope,
        private val provider: JavaSyntheticPropertiesProvider
) : AbstractResolutionScopeAdapter() {
    private val properties = storageManager.createMemoizedFunction<Name, SyntheticPropertyHolder> {
        doGetProperty(it)
    }
    protected val descriptors = storageManager.createLazyValue {
        doGetDescriptors()
    }
    lateinit private var classDescriptor: ClassDescriptor

    private fun getSyntheticPropertyAndRecordLookups(name: Name, location: LookupLocation): SyntheticJavaPropertyDescriptor? {
        val (descriptor, lookedNames) = properties(name)

        if (!this::classDescriptor.isInitialized) return descriptor

        if (location !is NoLookupLocation) {
            lookedNames.forEach { lookupTracker.record(location, classDescriptor, it) }
        }

        return descriptor
    }

    private fun doGetDescriptors(): List<VariableDescriptor> =
            super.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, MemberScope.ALL_NAME_FILTER)
                    .filterIsInstance<FunctionDescriptor>()
                    .flatMap {
                        val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(it.name) ?: return@flatMap emptyList<VariableDescriptor>()
                        getContributedVariables(propertyName, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
                    }

    private fun doGetProperty(name: Name): SyntheticPropertyHolder {
        fun result(descriptor: SyntheticJavaPropertyDescriptor?, getterNames: List<Name>, setterName: Name? = null): SyntheticPropertyHolder {
            if (lookupTracker === LookupTracker.DO_NOTHING) {
                return if (descriptor == null) SyntheticPropertyHolder.EMPTY else SyntheticPropertyHolder(descriptor, emptyList())
            }

            val names = ArrayList<Name>(getterNames.size + (setterName?.let { 1 } ?: 0))

            names.addAll(getterNames)
            names.addIfNotNull(setterName)

            return SyntheticPropertyHolder(descriptor, names)
        }
        if (name.isSpecial) return SyntheticPropertyHolder.EMPTY
        val identifier = name.identifier
        if (identifier.isEmpty()) return SyntheticPropertyHolder.EMPTY
        val firstChar = identifier[0]
        if (!firstChar.isJavaIdentifierStart() || firstChar in 'A'..'Z') return SyntheticPropertyHolder.EMPTY

        val possibleGetMethodNames = possibleGetMethodNames(name)
        val getter = possibleGetMethodNames
                             .flatMap { super.getContributedFunctions(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE) }
                             .singleOrNull { it.hasJavaOriginInHierarchy() && isGoodGetMethod(it) }
                     ?: return result(null, possibleGetMethodNames)
        if (getter.containingDeclaration !is ClassDescriptor) return SyntheticPropertyHolder.EMPTY

        if (!this::classDescriptor.isInitialized) {
            classDescriptor = getter.containingDeclaration as ClassDescriptor
        }

        val setMethodName = setMethodName(getter.name)
        val setter = super.getContributedFunctions(setMethodName, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
                .singleOrNull { isGoodSetMethod(it, getter) }

        val type = getter.returnType!!
        val descriptor = MyPropertyDescriptor.create(getter.containingDeclaration as ClassDescriptor, getter, setter, name, type)
        return result(descriptor, possibleGetMethodNames, setMethodName)
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
            if (descriptor.findOverridden {
                val baseProperty = SyntheticJavaPropertyDescriptor.findByGetterOrSetter(it, provider)
                baseProperty?.getMethod?.name == getMethod.name
            } == null) return false
        }

        return parameter.varargElementType == null
               && descriptor.typeParameters.isEmpty()
               && descriptor.visibility.isVisibleOutside()
    }

    private fun FunctionDescriptor.findOverridden(condition: (FunctionDescriptor) -> Boolean): FunctionDescriptor? {
        for (descriptor in overriddenDescriptors) {
            if (condition(descriptor)) return descriptor
            descriptor.findOverridden(condition)?.let { return it }
        }
        return null
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
        return listOfNotNull(getSyntheticPropertyAndRecordLookups(name, location)) + workerScope.getContributedVariables(name, location)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) return workerScope.getContributedDescriptors(kindFilter, nameFilter)
        return filterDescriptors(kindFilter, nameFilter) + workerScope.getContributedDescriptors(kindFilter, nameFilter)
    }

    private fun filterDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) descriptors().filter { nameFilter(it.name) }
            else emptyList()

    private data class SyntheticPropertyHolder(val descriptor: SyntheticJavaPropertyDescriptor?, val lookedNames: List<Name>) {
        companion object {
            val EMPTY = SyntheticPropertyHolder(null, emptyList())
        }
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

private class JavaSyntheticPropertiesMemberScopeForSubstitution(
        override val workerScope: MemberScope,
        private val propertiesScope: JavaSyntheticPropertiesScope
) :
        AbstractMemberScopeAdapter() {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return propertiesScope.getContributedVariables(name, location).filterIsInstance<PropertyDescriptor>()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return propertiesScope.getContributedFunctions(name, location).filterIsInstance<SimpleFunctionDescriptor>()
    }

    override fun getVariableNames(): Set<Name> {
        return propertiesScope.getContributedDescriptors(DescriptorKindFilter.VARIABLES, nameFilter = MemberScope.ALL_NAME_FILTER).map { it.name }.toSet()
    }
}

class JavaSyntheticPropertiesProvider(
        private val storageManager: StorageManager,
        private val lookupTracker: LookupTracker
) : SyntheticScopeProvider {
    private val makeSynthetic = storageManager.createMemoizedFunction<ResolutionScope, JavaSyntheticPropertiesScope> {
        JavaSyntheticPropertiesScope(storageManager, lookupTracker, it, this)
    }

    override fun provideSyntheticScope(scope: ResolutionScope, requirements: SyntheticScopesRequirements): ResolutionScope {
        if (!requirements.needExtensionProperties) return scope
        // Instead of decorating substituted scope, substitute decorated scope
        if (scope is SubstitutingScope) {
            val unsubstitutedScope = scope.workerScope
            val synthetic = JavaSyntheticPropertiesMemberScopeForSubstitution(unsubstitutedScope, makeSynthetic(unsubstitutedScope))
            return SubstitutingScope(synthetic, scope.givenSubstitutor)
        }
        return makeSynthetic(scope)
    }
}

