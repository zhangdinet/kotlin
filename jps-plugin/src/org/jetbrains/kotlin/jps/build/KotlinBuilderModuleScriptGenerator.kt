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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.ProjectBuildException
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.jps.model.PlatformModuleBuildTarget
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.alwaysNull
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.util.*

object KotlinBuilderModuleScriptGenerator {

    // TODO used reflection to be compatible with IDEA from both 143 and 144 branches,
    // TODO switch to directly using when "since-build" will be >= 144.3357.4
    internal val getRelatedProductionModule: (JpsModule) -> JpsModule? = run {
        val klass =
                try {
                    Class.forName("org.jetbrains.jps.model.module.JpsTestModuleProperties")
                } catch (e: ClassNotFoundException) {
                    return@run alwaysNull()
                }


        val getTestModulePropertiesMethod: Method
        val getProductionModuleMethod: Method

        try {
            getTestModulePropertiesMethod = JpsJavaExtensionService::class.java.getDeclaredMethod("getTestModuleProperties", JpsModule::class.java)
            getProductionModuleMethod = klass.getDeclaredMethod("getProductionModule")
        }
        catch (e: NoSuchMethodException) {
            return@run alwaysNull()
        }

        return@run { module ->
            getTestModulePropertiesMethod(JpsJavaExtensionService.getInstance(), module)?.let {
                getProductionModuleMethod(it) as JpsModule?
            }
        }
    }

    fun generateModuleDescription(
            context: CompileContext,
            chunk: ModuleChunk,
            sourceFiles: MultiMap<ModuleBuildTarget, File>, // ignored for non-incremental compilation
            hasRemovedFiles: Boolean
    ): File? {
        val builder = KotlinModuleXmlBuilder()

        var noSources = true

        val outputDirs = HashSet<File>()
        for (target in chunk.targets) {
            outputDirs.add(getOutputDirSafe(target))
        }

        val logger = context.loggingManager.projectBuilderLogger
        for (target in chunk.targets) {
            val outputDir = getOutputDirSafe(target)
            val platformModule = PlatformModuleBuildTarget(target)
            val moduleSources = platformModule.findSources(sourceFiles)

            if (moduleSources.isNotEmpty() || hasRemovedFiles) {
                noSources = false

                if (logger.isEnabled) {
                    logger.logCompiledFiles(moduleSources, KotlinBuilder.KOTLIN_BUILDER_NAME, "Compiling files:")
                }
            }

            val targetType = target.targetType
            assert(targetType is JavaModuleBuildTargetType)
            val targetId = TargetId(target)
            builder.addModule(
                targetId.name,
                outputDir.absolutePath,
                moduleSources,
                platformModule.findSourceRoots(context),
                platformModule.findClassPathRoots(),
                platformModule.findModularJdkRoot(),
                targetId.type,
                (targetType as JavaModuleBuildTargetType).isTests,
                // this excludes the output directories from the class path, to be removed for true incremental compilation
                outputDirs,
                platformModule.additionalOutputDirsWhereInternalsAreVisible
            )
        }

        if (noSources) return null

        val scriptFile = createTempFileForModuleDesc(chunk)
        FileUtil.writeToFile(scriptFile, builder.asText().toString())
        return scriptFile
    }

    private fun createTempFileForModuleDesc(chunk: ModuleChunk): File {
        val readableSuffix = buildString {
            append(StringUtil.sanitizeJavaIdentifier(chunk.representativeTarget().module.name))
            if (chunk.containsTests()) {
                append("-test")
            }
        }
        val dir = System.getProperty("kotlin.jps.dir.for.module.files")?.let { File(it) }?.takeIf { it.isDirectory }
        return try {
            File.createTempFile("kjps", readableSuffix + ".script.xml", dir)
        }
        catch (e: IOException) {
            // sometimes files cannot be created, because file name is too long (Windows, Mac OS)
            // see https://bugs.openjdk.java.net/browse/JDK-8148023
            try {
                File.createTempFile("kjps", ".script.xml", dir)
            }
            catch (e: IOException) {
                val message = buildString {
                    append("Could not create module file when building chunk $chunk")
                    if (dir != null) {
                        append(" in dir $dir")
                    }
                }
                throw RuntimeException(message, e)
            }
        }
    }

    fun getOutputDirSafe(target: ModuleBuildTarget): File =
            target.outputDir ?: throw ProjectBuildException("No output directory found for " + target)

    fun getProductionModulesWhichInternalsAreVisible(from: ModuleBuildTarget): List<JpsModule> {
        if (!from.isTests) return emptyList()

        val result = SmartList<JpsModule>(from.module)
        result.addIfNotNull(getRelatedProductionModule(from.module))

        return result.filter { it.hasProductionSourceRoot }
    }

}
