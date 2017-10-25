/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.analyze

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.PredefinedAnnotation.*
import org.jetbrains.kotlin.resolve.diagnostics.SuppressStringProvider

class SuppressDiagnosticsForNativeDeclarations : SuppressStringProvider {
    private val stringsToSuppress =
            listOf(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY,
                   Errors.NON_MEMBER_FUNCTION_NO_BODY,
                   Errors.UNINITIALIZED_VARIABLE,
                   Errors.MUST_BE_INITIALIZED,
                   Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT,
                   Errors.UNUSED_PARAMETER
            ).map { it.name.toLowerCase() }

    private val expectedFqNames = setOf(NATIVE.fqName, NATIVE_INVOKE.fqName, NATIVE_GETTER.fqName, NATIVE_SETTER.fqName)

    override fun get(annotationDescriptor: AnnotationDescriptor): List<String> =
            if (annotationDescriptor.fqName in expectedFqNames) stringsToSuppress else emptyList()
}
