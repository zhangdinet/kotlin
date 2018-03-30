/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.io.File

val JS_IR_BACKEND_TEST_WHITELIST = listOf(
    "js/js.translator/testData/box/package/nestedPackage.kt",
    "js/js.translator/testData/box/package/deeplyNestedPackage.kt",
    "js/js.translator/testData/box/package/deeplyNestedPackageFunctionCalled.kt",
    "js/js.translator/testData/box/package/classCreatedInDeeplyNestedPackage.kt",
    "js/js.translator/testData/box/multiPackage/createClassFromOtherPackage.kt",
    "js/js.translator/testData/box/multiPackage/createClassFromOtherPackageUsingImport.kt",
    "js/js.translator/testData/box/multiPackage/nestedPackageFunctionCalledFromOtherPackage.kt",
    "js/js.translator/testData/box/expression/identifierClash/useVariableOfNameOfFunction.kt",
    "js/js.translator/testData/box/expression/invoke/internalFunctionFromSuperclass.kt",
    "js/js.translator/testData/box/expression/stringClass/stringConstant.kt",
    "js/js.translator/testData/box/expression/when/empty.kt",
    "js/js.translator/testData/box/simple/notBoolean.kt",
    "js/js.translator/testData/box/operatorOverloading/binaryDivOverload.kt",
    "js/js.translator/testData/box/operatorOverloading/notOverload.kt"
).map { File(it) }
