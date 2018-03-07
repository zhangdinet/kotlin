/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

fun coroutinesPackageFqName(languageFeatureSettings: LanguageVersionSettings) =
    if (languageFeatureSettings.supportsFeature(LanguageFeature.ReleaseCoroutines))
        DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE
    else
        DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL

fun coroutinesIntrinsicsPackageFqName(languageFeatureSettings: LanguageVersionSettings) =
    coroutinesPackageFqName(languageFeatureSettings).child(Name.identifier("intrinsics"))

fun continuationInterfaceFqName(languageFeatureSettings: LanguageVersionSettings) =
    coroutinesPackageFqName(languageFeatureSettings).child(Name.identifier("Continuation"))

fun restrictsSuspensionFqName(languageFeatureSettings: LanguageVersionSettings) =
    coroutinesPackageFqName(languageFeatureSettings).child(Name.identifier("RestrictsSuspension"))

fun FqName.isBuiltInCoroutineContext(languageFeatureSettings: LanguageVersionSettings) =
    this == coroutinesPackageFqName(languageFeatureSettings).child(Name.identifier("coroutineContext"))