/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

fun TODO(element: IrElement): Nothing = TODO(element::class.java.simpleName + " is not supported yet here")

fun CallableMemberDescriptor.createValueParameter(index: Int, name: String, type: KotlinType): ValueParameterDescriptor =
    ValueParameterDescriptorImpl(
        this, null,
        index,
        Annotations.EMPTY,
        Name.identifier(name),
        type,
        false, false, false, null, SourceElement.NO_SOURCE
    )