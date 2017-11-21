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
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager

private class SamAdapterSyntheticConstructorsScope(
        private val storageManager: StorageManager,
        private val samResolver: SamConversionResolver,
        override val wrappedScope: ResolutionScope
) : SyntheticResolutionScope() {
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
        return getAllSamConstructors(classifier) + super.getContributedFunctions(name, location)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            val contributedDescriptors = super.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER)
            val contributedDescriptor = contributedDescriptors.singleOrNull()
            return super.getContributedDescriptors(kindFilter, nameFilter) +
                   if (contributedDescriptor is ConstructorDescriptor)
                       listOfNotNull(getSyntheticConstructor(contributedDescriptor))
                   else
                       processClassifierDescriptors(contributedDescriptors, kindFilter, nameFilter)
        }
        else {
            return super.getContributedDescriptors(kindFilter, nameFilter)
        }
    }

    private fun processClassifierDescriptors(
            contributedDescriptors: Collection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): List<DeclarationDescriptor> {
        return contributedDescriptors
                       .filterIsInstance<ClassifierDescriptor>()
                       .flatMap { getAllSamConstructors(it) } + super.getContributedDescriptors(kindFilter, nameFilter)
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
        private val samResolver: SamConversionResolver
) : SyntheticScopeProvider {
    private val makeSynthetic = storageManager.createMemoizedFunction<ResolutionScope, ResolutionScope> {
        SamAdapterSyntheticConstructorsScope(storageManager, samResolver, it)
    }

    override fun provideSyntheticScope(scope: ResolutionScope, metadata: SyntheticScopesMetadata): ResolutionScope {
        if (!metadata.needConstructors) return scope
        return makeSynthetic(scope)
    }
}