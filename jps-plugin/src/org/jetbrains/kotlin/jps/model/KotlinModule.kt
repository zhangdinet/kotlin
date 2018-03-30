/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleSourceRoot
import org.jetbrains.jps.util.JpsPathUtil
import java.io.File

abstract class KotlinModule(val module: JpsModule) {
    fun addAllKotlinSourceFiles(receiver: MutableList<File>, isTests: Boolean) {
        val moduleExcludes = ContainerUtil.map(
            module.excludeRootsList.urls
        ) { url -> JpsPathUtil.urlToFile(url) }

        val compilerExcludes =
            JpsJavaExtensionService.getInstance().getOrCreateCompilerConfiguration(module.project).compilerExcludes

        for (sourceRoot in getRelevantSourceRoots(isTests)) {
            FileUtil.processFilesRecursively(
                sourceRoot.file,
                Processor { file ->
                    if (compilerExcludes.isExcluded(file)) return@Processor true

                    if (file.isFile && file.isKotlinSourceFile) {
                        receiver.add(file)
                    }
                    true
                },
                Processor { dir ->
                    ContainerUtil.find(
                        moduleExcludes
                    ) { exclude -> FileUtil.filesEqual(exclude, dir) } == null
                })
        }
    }

    private fun getRelevantSourceRoots(isTests: Boolean): Iterable<JpsModuleSourceRoot> {
        val sourceRootType = if (isTests) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
        return module.getSourceRoots<JavaSourceRootProperties>(sourceRootType)
    }
}

val File.isKotlinSourceFile: Boolean
    get() = FileUtilRt.extensionEquals(name, "kt")