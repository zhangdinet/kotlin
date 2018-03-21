/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.metadata.isSuspend
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic

object SuspendCoroutineUninterceptedOrReturnFIF: FunctionIntrinsicFactory {
    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        if (!descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(
                LanguageVersionSettingsImpl(
                    LanguageVersion.KOTLIN_1_2,
                    ApiVersion.KOTLIN_1_2
                )
            )
        ) return null
        return Intrinsic
    }
    object Intrinsic: FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val lambda = arguments.first()
            val continuation = arguments.last()
            val invocation = JsInvocation(lambda, continuation)
            invocation.isSuspend = true
            return invocation
        }
    }
}