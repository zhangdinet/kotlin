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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast

data class SyntheticScopesMetadata(
        val needExtensionProperties: Boolean = false,
        val needMemberFunctions: Boolean = false,
        val needStaticFunctions: Boolean = false,
        val needConstructors: Boolean = false
)

interface SyntheticScopeProvider {
    fun provideSyntheticScope(scope: ResolutionScope, metadata: SyntheticScopesMetadata): ResolutionScope
}

interface SyntheticScopes {
    val scopeProviders: Collection<SyntheticScopeProvider>

    fun provideSyntheticScope(scope: ResolutionScope, metadata: SyntheticScopesMetadata): ResolutionScope {
        var result = scope
        for (provider in scopeProviders) {
            result = provider.provideSyntheticScope(result, metadata)
        }
        return result
    }

    object Empty : SyntheticScopes {
        override val scopeProviders: Collection<SyntheticScopeProvider> = emptyList()
    }
}

fun SyntheticScopes.collectSyntheticConstructors(constructor: ConstructorDescriptor): Collection<ConstructorDescriptor> {
    val scope = object : ResolutionScope {
        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> = emptyList()
        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptyList()
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
                listOf(constructor)
    }
    val syntheticScope = provideSyntheticScope(scope, SyntheticScopesMetadata(needConstructors = true))
    return syntheticScope.getContributedDescriptors().cast()
}

interface SyntheticPropertyDescriptor: PropertyDescriptor