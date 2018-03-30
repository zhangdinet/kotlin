/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import org.jetbrains.jps.model.module.JpsModule
import java.io.File

class KotlinCommonModule(module: JpsModule) : KotlinModule(module) {
    val dependencies = mutableListOf<KotlinCommonModule>()

    init {
        // todo: dependencies
    }

    fun addAllKotlinSourceFilesWithDependenciesRecursivly(result: MutableList<File>, isTests: Boolean) {
        addAllKotlinSourceFiles(result, isTests)
        dependencies.forEach {
            it.addAllKotlinSourceFilesWithDependenciesRecursivly(result, isTests)
        }
    }
}