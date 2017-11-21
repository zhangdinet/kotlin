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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager

private class SamAdapterSyntheticStaticFunctionsScope(
        storageManager: StorageManager,
        private val samResolver: SamConversionResolver,
        override val wrappedScope: ResolutionScope
) : SyntheticResolutionScope() {
    val functions = storageManager.createMemoizedFunction<Name, List<FunctionDescriptor>> {
        super.getContributedFunctions(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE) + doGetFunctions(it)
    }

    val descriptors = storageManager.createLazyValue {
        doGetDescriptors()
    }

    private fun doGetFunctions(name: Name) =
            super.getContributedFunctions(name, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
                    .mapNotNull { wrapFunction(it) }

    private fun wrapFunction(function: DeclarationDescriptor): FunctionDescriptor? {
        if (function !is JavaMethodDescriptor) return null
        if (function.dispatchReceiverParameter != null) return null // consider only statics
        if (!SingleAbstractMethodUtils.isSamAdapterNecessary(function)) return null

        return SingleAbstractMethodUtils.createSamAdapterFunction(function, samResolver)
    }

    private fun doGetDescriptors(): List<FunctionDescriptor> =
            super.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, MemberScope.ALL_NAME_FILTER)
                    .mapNotNull { wrapFunction(it) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = functions(name)

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return super.getContributedDescriptors(kindFilter, nameFilter) +
               if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) descriptors()
               else emptyList()
    }
}

class SamAdapterSyntheticStaticFunctionsProvider(
        private val storageManager: StorageManager,
        private val samResolver: SamConversionResolver
) : SyntheticScopeProvider {
    private val makeSynthetic = storageManager.createMemoizedFunction<ResolutionScope, ResolutionScope> {
        SamAdapterSyntheticStaticFunctionsScope(storageManager, samResolver, it)
    }

    override fun provideSyntheticScope(scope: ResolutionScope, metadata: SyntheticScopesMetadata): ResolutionScope {
        if (!metadata.needStaticFunctions) return scope
        return makeSynthetic(scope)
    }
}