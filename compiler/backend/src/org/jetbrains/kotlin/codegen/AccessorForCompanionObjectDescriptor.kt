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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name

class AccessorForCompanionObjectDescriptor(
        containingDeclaration: DeclarationDescriptor,
        val companionObjectDescriptor: ClassDescriptor
) :
        SimpleFunctionDescriptorImpl(
                containingDeclaration,
                null,
                Annotations.EMPTY,
                Name.identifier("access\$co\$" + companionObjectDescriptor.name.asString()),
                CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
        ),
        AccessorForMemberDescriptor<ClassDescriptor>
{
    init {
        initialize(
                null, null, emptyList(), emptyList(),
                companionObjectDescriptor.defaultType,
                Modality.FINAL,
                Visibilities.LOCAL // TODO really?
        )
    }

    override fun getAccessedDescriptor(): ClassDescriptor = companionObjectDescriptor

    override fun getSuperCallTarget(): ClassDescriptor? = null
}