/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.intellij.util.io.URLUtil
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsSdkDependency
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.jps.build.KotlinBuilderModuleScriptGenerator
import org.jetbrains.kotlin.jps.targetPlatform
import java.io.File

class PlatformModuleBuildTarget(val target: ModuleBuildTarget) {
    val module: KotlinPlatformModule = KotlinPlatformModule(target.module)
    val exceptedBy = mutableListOf<KotlinCommonModule>()

    init {
        findExpectedBy()
    }

    private fun findExpectedBy() {
        target.allDependencies.modules.forEach {
            val targetPlatform = it.targetPlatform
            if (targetPlatform == TargetPlatformKind.Common) {
                exceptedBy.add(KotlinCommonModule(it))
            }
        }
    }

    val additionalOutputDirsWhereInternalsAreVisible: List<File>
        get() {
            return KotlinBuilderModuleScriptGenerator.getProductionModulesWhichInternalsAreVisible(target).mapNotNullTo(SmartList<File>()) {
                JpsJavaExtensionService.getInstance().getOutputDirectory(it, false)
            }
        }

    /**
     * @param changedSourceFiles ignored for non-incremental compilation
     */
    fun findSources(changedSourceFiles: MultiMap<ModuleBuildTarget, File>) =
        if (IncrementalCompilation.isEnabled()) changedSourceFiles.get(target).toList()
        else {
            mutableListOf<File>().also { result ->
                module.addAllKotlinSourceFiles(result, target.isTests)
                exceptedBy.forEach {
                    it.addAllKotlinSourceFilesWithDependenciesRecursivly(result, target.isTests)
                }
            }
        }

    fun findSourceRoots(context: CompileContext): List<JvmSourceRoot> {
        val roots = context.projectDescriptor.buildRootIndex.getTargetRoots(target, context)
        val result = ContainerUtil.newArrayList<JvmSourceRoot>()
        for (root in roots) {
            val file = root.rootFile
            val prefix = root.packagePrefix
            if (file.exists()) {
                result.add(JvmSourceRoot(file, if (prefix.isEmpty()) null else prefix))
            }
        }
        return result
    }

    fun findClassPathRoots(): Collection<File> {
        return target.allDependencies.classes().roots.filter { file ->
            if (!file.exists()) {
                val extension = file.extension

                // Don't filter out files, we want to report warnings about absence through the common place
                if (!(extension == "class" || extension == "jar")) {
                    return@filter false
                }
            }

            true
        }
    }

    fun findModularJdkRoot(): File? {
        // List of paths to JRE modules in the following format:
        // jrt:///Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home!/java.base
        val urls = JpsJavaExtensionService.dependencies(target.module)
            .satisfying { dependency -> dependency is JpsSdkDependency }
            .classes().urls

        val url = urls.firstOrNull { it.startsWith(StandardFileSystems.JRT_PROTOCOL_PREFIX) } ?: return null

        return File(url.substringAfter(StandardFileSystems.JRT_PROTOCOL_PREFIX).substringBeforeLast(URLUtil.JAR_SEPARATOR))
    }
}

val ModuleBuildTarget.allDependencies: JpsJavaDependenciesEnumerator
    get() {
        return JpsJavaExtensionService.dependencies(module).recursively().exportedOnly()
            .includedIn(JpsJavaClasspathKind.compile(isTests))
    }