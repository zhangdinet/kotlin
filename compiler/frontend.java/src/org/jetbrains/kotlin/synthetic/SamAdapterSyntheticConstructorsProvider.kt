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
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager

val SAM_LOOKUP_NAME = Name.special("<SAM-CONSTRUCTOR>")

internal fun recordSamLookupsToClassifier(lookupTracker: LookupTracker, classifier: ClassifierDescriptor, location: LookupLocation) {
    if (classifier !is JavaClassDescriptor || classifier.kind != ClassKind.INTERFACE) return
    // TODO: We should also record SAM lookups even when the interface is not SAM
    if (!SingleAbstractMethodUtils.isSamType(classifier.defaultType)) return

    lookupTracker.record(location, classifier, SAM_LOOKUP_NAME)
}

private class SamAdapterSyntheticConstructorsScope(
        private val storageManager: StorageManager,
        private val samResolver: SamConversionResolver,
        private val lookupTracker: LookupTracker,
        override val workerScope: ResolutionScope
) : AbstractResolutionScopeAdapter() {
    private val samConstructorForClassifier =
            storageManager.createMemoizedFunction<JavaClassDescriptor, SamConstructorDescriptor> { classifier ->
                SingleAbstractMethodUtils.createSamConstructorFunction(classifier.containingDeclaration, classifier, samResolver)
            }

    private val samConstructorForJavaConstructor =
            storageManager.createMemoizedFunction<JavaClassConstructorDescriptor, ClassConstructorDescriptor> { constructor ->
                SingleAbstractMethodUtils.createSamAdapterConstructor(constructor, samResolver) as ClassConstructorDescriptor
            }

    private val samConstructorForTypeAliasConstructor =
            storageManager.createMemoizedFunctionWithNullableValues<
                    Pair<ClassConstructorDescriptor, TypeAliasDescriptor>,
                    TypeAliasConstructorDescriptor> { (constructor, typeAliasDescriptor) ->
                TypeAliasConstructorDescriptorImpl.createIfAvailable(storageManager, typeAliasDescriptor, constructor)
            }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        val classifier = getContributedClassifier(name, location) ?: return super.getContributedFunctions(name, location)
        recordSamLookupsToClassifier(classifier, location)
        return getAllSamConstructors(classifier) + super.getContributedFunctions(name, location)
    }

    private fun recordSamLookupsToClassifier(classifier: ClassifierDescriptor, location: LookupLocation) {
        recordSamLookupsToClassifier(lookupTracker, classifier, location)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            val contributedDescriptors = workerScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER)
            val contributedDescriptor = contributedDescriptors.singleOrNull()
            if (contributedDescriptor is ConstructorDescriptor)
                return super.getContributedDescriptors(kindFilter, nameFilter) + listOfNotNull(getSyntheticConstructor(contributedDescriptor))
            else
                return super.getContributedDescriptors(kindFilter, nameFilter) + processClassifierDescriptors(contributedDescriptors)
        }
        else {
            return super.getContributedDescriptors(kindFilter, nameFilter)
        }
    }

    private fun processClassifierDescriptors(
            contributedDescriptors: Collection<DeclarationDescriptor>
    ): List<DeclarationDescriptor> {
        return contributedDescriptors
                       .filterIsInstance<ClassifierDescriptor>()
                       .flatMap { getAllSamConstructors(it) }
    }

    private fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? {
        when (constructor) {
            is JavaClassConstructorDescriptor -> return createJavaSamAdapterConstructor(constructor)
            is TypeAliasConstructorDescriptor -> {
                val underlyingConstructor = constructor.underlyingConstructorDescriptor as? JavaClassConstructorDescriptor ?: return null
                val underlyingSamConstructor = createJavaSamAdapterConstructor(underlyingConstructor) ?: return null

                return samConstructorForTypeAliasConstructor(Pair(underlyingSamConstructor, constructor.typeAliasDescriptor))
            }
            else -> return null
        }
    }

    private fun createJavaSamAdapterConstructor(constructor: JavaClassConstructorDescriptor): ClassConstructorDescriptor? {
        if (!SingleAbstractMethodUtils.isSamAdapterNecessary(constructor)) return null
        return samConstructorForJavaConstructor(constructor)
    }

    private fun getAllSamConstructors(classifier: ClassifierDescriptor): List<FunctionDescriptor> =
            getSamAdaptersFromConstructors(classifier) + listOfNotNull(getSamConstructor(classifier))

    private fun getSamAdaptersFromConstructors(classifier: ClassifierDescriptor): List<FunctionDescriptor> {
        if (classifier !is JavaClassDescriptor) return emptyList()

        return arrayListOf<FunctionDescriptor>().apply {
            for (constructor in classifier.constructors) {
                val samConstructor = getSyntheticConstructor(constructor) ?: continue
                add(samConstructor)
            }
        }
    }

    private fun getSamConstructor(classifier: ClassifierDescriptor): SamConstructorDescriptor? {
        if (classifier is TypeAliasDescriptor) {
            return getTypeAliasSamConstructor(classifier)
        }

        if (classifier !is LazyJavaClassDescriptor || classifier.defaultFunctionTypeForSamInterface == null) return null
        return samConstructorForClassifier(classifier)
    }

    private fun getTypeAliasSamConstructor(classifier: TypeAliasDescriptor): SamConstructorDescriptor? {
        val classDescriptor = classifier.classDescriptor ?: return null
        if (classDescriptor !is LazyJavaClassDescriptor || classDescriptor.defaultFunctionTypeForSamInterface == null) return null

        return SingleAbstractMethodUtils.createTypeAliasSamConstructorFunction(
                classifier, samConstructorForClassifier(classDescriptor), samResolver)
    }
}

class SamAdapterSyntheticConstructorsProvider(
        private val storageManager: StorageManager,
        private val samResolver: SamConversionResolver,
        private val lookupTracker: LookupTracker
) : SyntheticScopeProvider {
    private val makeSynthetic = storageManager.createMemoizedFunction<ResolutionScope, ResolutionScope> {
        SamAdapterSyntheticConstructorsScope(storageManager, samResolver, lookupTracker, it)
    }

    override fun provideSyntheticScope(scope: ResolutionScope, requirements: SyntheticScopesRequirements): ResolutionScope {
        if (!requirements.needConstructors) return scope
        return makeSynthetic(scope)
    }
}