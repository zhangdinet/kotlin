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

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object MissingBuiltInDeclarationChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.isFlagEnabled(AnalysisFlags.suppressMissingBuiltinsError)) return

        diagnosticFor(resolvedCall.resultingDescriptor, reportOn)?.let(context.trace::report)
    }

    private fun diagnosticFor(descriptor: DeclarationDescriptor, reportOn: PsiElement): Diagnostic? {
        val containingClassOrPackage = DescriptorUtils.getParentOfType(descriptor, ClassOrPackageFragmentDescriptor::class.java)

        if (containingClassOrPackage is ClassDescriptor) {
            val containingPackage = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java)
            if ((containingPackage as? BuiltInsPackageFragment)?.isFallback == true) {
                return Errors.MISSING_BUILT_IN_DECLARATION.on(reportOn, containingClassOrPackage.fqNameSafe)
            }
        }
        else if ((containingClassOrPackage as? BuiltInsPackageFragment)?.isFallback == true) {
            return Errors.MISSING_BUILT_IN_DECLARATION.on(reportOn, descriptor.fqNameSafe)
        }

        return null
    }

    object ClassifierUsage : ClassifierUsageChecker {
        override fun check(
                targetDescriptor: ClassifierDescriptor,
                trace: BindingTrace,
                element: PsiElement,
                languageVersionSettings: LanguageVersionSettings
        ) {
            if (languageVersionSettings.isFlagEnabled(AnalysisFlags.suppressMissingBuiltinsError)) return

            diagnosticFor(targetDescriptor, element)?.let(trace::report)
        }
    }
}
