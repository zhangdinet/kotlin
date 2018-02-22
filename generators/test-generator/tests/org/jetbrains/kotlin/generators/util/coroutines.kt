/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.generators.tests.generator.MethodModel
import org.jetbrains.kotlin.generators.tests.generator.SimpleTestMethodModel
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

class CoroutinesTestModel(
    rootDir: File,
    file: File,
    doTestMethodName: String,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean,
    private val isLanguageVersion1_3: Boolean
) : SimpleTestMethodModel(
    rootDir,
    file,
    doTestMethodName,
    filenamePattern,
    checkFilenameStartsLowerCase,
    targetBackend,
    skipIgnored
) {
    override val name: String
        get() = super.name + if (isLanguageVersion1_3) "_1_3" else "_1_2"
}

fun isCommonCoroutineTest(file: File): Boolean {
    return file.readLines().any { it.startsWith("// COMMON_COROUTINES_TEST") }
}

fun createCommonCoroutinesTestMethodModels(
    rootDir: File,
    file: File,
    doTestMethodName: String,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean
): Collection<MethodModel> {
    return listOf(
        CoroutinesTestModel(
            rootDir,
            file,
            doTestMethodName,
            filenamePattern,
            checkFilenameStartsLowerCase,
            targetBackend,
            skipIgnored,
            true
        ),
        CoroutinesTestModel(
            rootDir,
            file,
            doTestMethodName,
            filenamePattern,
            checkFilenameStartsLowerCase,
            targetBackend,
            skipIgnored,
            false
        )
    )
}