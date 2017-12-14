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
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager

private class SamAdapterSyntheticStaticFunctionsCache(
        storageManager: StorageManager,
        private val samResolver: SamConversionResolver
) {
    val functions = storageManager.createMemoizedFunctionWithNullableValues<FunctionDescriptor, FunctionDescriptor> { wrapFunction(it) }

    private fun wrapFunction(function: FunctionDescriptor): FunctionDescriptor? {
        if (function !is JavaMethodDescriptor) return null
        if (function.dispatchReceiverParameter != null) return null // consider only statics
        if (!SingleAbstractMethodUtils.isSamAdapterNecessary(function)) return null

        return SingleAbstractMethodUtils.createSamAdapterFunction(function, samResolver)
    }
}

private class SamAdapterSyntheticStaticFunctionsScope(
        private val cache: SamAdapterSyntheticStaticFunctionsCache,
        private val lookupTracker: LookupTracker,
        override val workerScope: ResolutionScope
) : AbstractResolutionScopeAdapter() {
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        val original = super.getContributedFunctions(name, location)
        val synthetics = original.mapNotNull { cache.functions(it) }
        if (synthetics.isEmpty()) return original
        recordLookups(synthetics, location)
        return synthetics + original
    }

    private fun recordLookups(functions: Collection<FunctionDescriptor>, location: LookupLocation) {
        functions.forEach { recordSamLookupsForParameters(it.original, location) }
    }

    private fun recordSamLookupsForParameters(function: FunctionDescriptor, location: LookupLocation) {
        for (valueParameter in function.valueParameters) {
            recordSamLookupsToClassifier(lookupTracker, valueParameter.type.constructor.declarationDescriptor ?: continue, location)
        }
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val original = super.getContributedDescriptors(kindFilter, nameFilter)
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) return original
        return original.filterIsInstance<FunctionDescriptor>().mapNotNull { cache.functions(it) } + original
    }
}

class SamAdapterSyntheticStaticFunctionsProvider(
        storageManager: StorageManager,
        samResolver: SamConversionResolver,
        private val lookupTracker: LookupTracker
) : SyntheticScopeProvider {
    private val cache = SamAdapterSyntheticStaticFunctionsCache(storageManager, samResolver)
    override fun provideSyntheticScope(scope: ResolutionScope, requirements: SyntheticScopesRequirements): ResolutionScope {
        if (!requirements.needStaticFunctions) return scope
        return SamAdapterSyntheticStaticFunctionsScope(cache, lookupTracker, scope)
    }
}