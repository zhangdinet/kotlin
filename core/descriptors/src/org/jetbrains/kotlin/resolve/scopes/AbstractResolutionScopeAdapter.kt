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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name

abstract class AbstractResolutionScopeAdapter: ResolutionScope {
    abstract val workerScope: ResolutionScope

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            workerScope.getContributedClassifier(name, location)

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> =
            workerScope.getContributedVariables(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
            workerScope.getContributedFunctions(name, location)

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
            workerScope.getContributedDescriptors(kindFilter, nameFilter)

    override fun definitelyDoesNotContainName(name: Name): Boolean = workerScope.definitelyDoesNotContainName(name)

    override fun recordLookup(name: Name, location: LookupLocation) {
        workerScope.recordLookup(name, location)
    }
}