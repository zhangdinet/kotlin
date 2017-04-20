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

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.js.resolve.MODULE_KIND
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractDiagnosticsTestWithJsStdLib : AbstractDiagnosticsTest() {
    override fun getEnvironmentConfigFiles(): EnvironmentConfigFiles = EnvironmentConfigFiles.JS_CONFIG_FILES

    override fun beforeAnalysisStarted(trace: BindingTrace, module: ModuleDescriptor, files: List<KtFile>) {
        trace.record(MODULE_KIND, module, getModuleKind(files))
    }

    private fun getModuleKind(ktFiles: Collection<KtFile>): ModuleKind {
        for (file in ktFiles) {
            val kind = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// MODULE_KIND:")
            if (kind != null) {
                return ModuleKind.valueOf(kind)
            }
        }

        return ModuleKind.PLAIN
    }

    object StdlibJsModuleInfo : LibraryModuleInfo {
        override val name: Name get() = Name.special("<kotlin-stdlib-js>")

        override fun dependencies(): List<ModuleInfo> = listOf(this)

        override val platform: TargetPlatform get() = JsPlatform

        override fun getLibraryRoots(): Collection<String> = JsConfig.JS_STDLIB
    }

    override fun getLibraryAndSdkDependency(): ModuleInfo =
            StdlibJsModuleInfo

    override fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<TestModule, List<TestFile>>): Boolean = true
}
