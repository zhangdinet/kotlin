/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.allopen.ide

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginModel
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginModelBuilderService
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginProjectResolverExtension

interface AllOpenModel : AnnotationBasedPluginModel

class AllOpenModelImpl(
        override val annotations: List<String>,
        override val presets: List<String>
) : AllOpenModel

class AllOpenModelBuilderService : AnnotationBasedPluginModelBuilderService<AllOpenModel>() {
    override val gradlePluginName get() = "org.jetbrains.kotlin.plugin.allopen"
    override val extensionName get() = "allOpen"
    override val modelClass get() = AllOpenModel::class.java
    override fun createModel(annotations: List<String>, presets: List<String>) = AllOpenModelImpl(annotations, presets)
}

class AllOpenProjectResolverExtension : AnnotationBasedPluginProjectResolverExtension<AllOpenModel>() {
    companion object {
        val KEY = Key<AnnotationBasedPluginModel>("AllOpenModel")
    }

    override val modelClass get() = AllOpenModel::class.java
    override val userDataKey get() = KEY
}