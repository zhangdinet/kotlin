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

data class SyntheticScopesRequirements(
        val needExtensionProperties: Boolean = false,
        val needMemberFunctions: Boolean = false,
        val needStaticFunctions: Boolean = false,
        val needConstructors: Boolean = false
)

interface SyntheticScopeProvider {
    fun provideSyntheticScope(scope: ResolutionScope, requirements: SyntheticScopesRequirements): ResolutionScope
}

interface SyntheticScopes {
    val scopeProviders: Collection<SyntheticScopeProvider>

    fun provideSyntheticScope(scope: ResolutionScope, requirements: SyntheticScopesRequirements): ResolutionScope =
            scopeProviders.fold(scope) { prevScope, provider->
                provider.provideSyntheticScope(prevScope, requirements)
            }

    object Empty : SyntheticScopes {
        override val scopeProviders: Collection<SyntheticScopeProvider> = emptyList()
    }
}

// TODO: Find a way to remove it
fun SyntheticScopes.collectSyntheticConstructors(constructor: ConstructorDescriptor): Collection<ConstructorDescriptor> {
    val scope = object : ResolutionScope.Empty() {
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
                listOf(constructor)
    }
    val syntheticScope = provideSyntheticScope(scope, SyntheticScopesRequirements(needConstructors = true))
    return syntheticScope.getContributedDescriptors().filterIsInstance<ConstructorDescriptor>()
}

interface SyntheticPropertyDescriptor: PropertyDescriptor