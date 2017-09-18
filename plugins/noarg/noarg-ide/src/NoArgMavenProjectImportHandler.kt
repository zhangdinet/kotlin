/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.noarg.ide

import org.jetbrains.kotlin.annotation.plugin.ide.AbstractMavenImportHandler
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor.Companion.INVOKE_INITIALIZERS_OPTION
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor.Companion.ANNOTATION_OPTION
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor.Companion.SUPPORTED_PRESETS

class NoArgMavenProjectImportHandler : AbstractMavenImportHandler() {
    private companion object {
        val MAVEN_PLUGIN_NAME = "no-arg"
    }

    override val compilerPluginId = NoArgCommandLineProcessor.PLUGIN_ID
    override val pluginName = "noarg"
    override val annotationOptionName = NoArgCommandLineProcessor.ANNOTATION_OPTION.name
    override val mavenPluginArtifactName = "kotlin-maven-noarg"
    override val pluginJarFileFromIdea = PathUtil.kotlinPathsForIdeaPlugin.noArgPluginJarPath

    private fun getOptions(rawOptions: List<String>, option: CliOption): List<String> {
        return rawOptions.mapNotNull { text ->
            val prefix = MAVEN_PLUGIN_NAME + ":" + option.name + "="
            if (!text.startsWith(prefix)) return@mapNotNull null
            text.substring(prefix.length)
        }
    }

    private fun isNoArgEnabled(enabledCompilerPlugins: List<String>): Boolean {
        return "no-arg" in enabledCompilerPlugins || "jpa" in enabledCompilerPlugins
    }

    override fun parseAdditionalPluginOptions(enabledCompilerPlugins: List<String>, pluginOptions: List<String>): List<Pair<String, String>> {
        if (!isNoArgEnabled(enabledCompilerPlugins)) return emptyList()

        return mutableListOf<Pair<String, String>>().apply {
            getOptions(pluginOptions, INVOKE_INITIALIZERS_OPTION).lastOrNull()?.let { value ->
                add(Pair(INVOKE_INITIALIZERS_OPTION.name, value))
            }
        }
    }

    override fun getAnnotations(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<String>? {
        if (!isNoArgEnabled(enabledCompilerPlugins)) return null

        val annotations = mutableListOf<String>()
        for ((presetName, presetAnnotations) in SUPPORTED_PRESETS) {
            if (presetName in enabledCompilerPlugins) {
                annotations.addAll(presetAnnotations)
            }
        }

        annotations.addAll(getOptions(compilerPluginOptions, ANNOTATION_OPTION))

        return annotations
    }
}
