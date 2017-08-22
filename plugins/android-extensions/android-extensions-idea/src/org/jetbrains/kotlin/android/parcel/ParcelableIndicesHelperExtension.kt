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

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.idea.androidExtensionsIsExperimental
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor
import org.jetbrains.kotlin.types.KotlinType

class ParcelableIndicesHelperExtension : KotlinIndicesHelperExtension {
    private companion object {
        val CREATOR_NAME = Name.identifier(ParcelableSyntheticComponent.ComponentKind.CREATOR.jvmName)
    }

    override fun appendExtensionCallables(
            consumer: MutableList<in CallableDescriptor>,
            moduleDescriptor: ModuleDescriptor,
            receiverTypes: Collection<KotlinType>,
            nameFilter: (String) -> Boolean
    ) {
        if (!nameFilter(CREATOR_NAME.asString())) return

        val moduleInfo = moduleDescriptor.getCapability(ModuleInfo.Capability) ?: return

        val isExperimental by lazy { moduleInfo.androidExtensionsIsExperimental }

        for (receiverType in receiverTypes) {
            val descriptor = receiverType.constructor.declarationDescriptor as? ClassDescriptor ?: continue
            if (descriptor !is SyntheticClassOrObjectDescriptor || !descriptor.isCompanionObject) continue
            (descriptor.containingDeclaration as? ClassDescriptor)?.takeIf { it.isParcelize } ?: continue
            if (!isExperimental) continue

            consumer += descriptor.unsubstitutedMemberScope.getContributedVariables(CREATOR_NAME, NoLookupLocation.FROM_IDE)
        }
    }
}