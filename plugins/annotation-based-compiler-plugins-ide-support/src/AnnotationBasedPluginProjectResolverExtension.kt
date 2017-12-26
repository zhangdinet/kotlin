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

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.util.Key
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import java.io.Serializable
import java.lang.Exception

interface AnnotationBasedPluginModel : Serializable {
    val annotations: List<String>
    val presets: List<String>
}

@Suppress("unused")
abstract class AnnotationBasedPluginProjectResolverExtension<T : AnnotationBasedPluginModel> : AbstractProjectResolverExtension() {
    private companion object {
        private val LOG = Logger.getInstance(AnnotationBasedPluginProjectResolverExtension::class.java)
    }

    abstract val modelClass: Class<T>
    abstract val userDataKey: Key<AnnotationBasedPluginModel>

    override fun getExtraProjectModelClasses() = setOf(modelClass)

    override fun getToolingExtensionsClasses() = setOf(
            modelClass,
            AnnotationBasedPluginProjectResolverExtension::class.java,
            Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val model = resolverCtx.getExtraProject(gradleModule, modelClass)

        if (model != null) {
            ideModule.putUserData(userDataKey, model)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}

abstract class AnnotationBasedPluginModelBuilderService<T : AnnotationBasedPluginModel> : AbstractKotlinGradleModelBuilder() {
    abstract val gradlePluginName: String
    abstract val extensionName: String

    abstract val modelClass: Class<T>
    abstract fun createModel(annotations: List<String>, presets: List<String>): T

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
                .withDescription("Unable to build $gradlePluginName plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == modelClass.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val plugin: Plugin<*>? = project.plugins.findPlugin(gradlePluginName)
        val extension: Any? = project.extensions.findByName(extensionName)

        val annotations = mutableListOf<String>()
        val presets = mutableListOf<String>()

        if (plugin != null && extension != null) {
            annotations += extension.getList("myAnnotations")
            presets += extension.getList("myPresets")
        }

        return createModel(annotations, presets)
    }

    private fun Any.getList(fieldName: String, clazz: Class<*> = this.javaClass): List<String> {
        val field = clazz.declaredFields.firstOrNull { it.name == fieldName }
                    ?: return getList(fieldName, clazz.superclass ?: return emptyList())

        val oldIsAccessible = field.isAccessible
        try {
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return field.get(this) as? List<String> ?: emptyList()
        } finally {
            field.isAccessible = oldIsAccessible
        }
    }
}