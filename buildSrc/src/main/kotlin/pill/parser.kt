@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.internal.artifacts.ResolvableDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import java.io.File

data class PProject(
    val name: String,
    val rootDirectory: File,
    val modules: List<PModule>,
    val libraries: List<PLibrary>
)

data class PModule(
    val name: String,
    val rootDirectory: File,
    val moduleFile: File,
    val contentRoots: List<PContentRoot>,
    val orderRoots: List<POrderRoot>,
    val group: String? = null
)

data class PContentRoot(
    val path: File,
    val sourceRoots: List<PSourceRoot>,
    val excludedDirectories: List<File>
)

data class PSourceRoot(
    val path: File,
    val kind: Kind
) {
    enum class Kind { PRODUCTION, TEST, RESOURCES, TEST_RESOURCES }
}

data class POrderRoot(
    val dependency: PDependency,
    val scope: Scope,
    val isExported: Boolean
) {
    enum class Scope { COMPILE, TEST, RUNTIME, PROVIDED }
}

sealed class PDependency {
    data class Module(val name: String) : PDependency()
    data class Library(val name: String) : PDependency()
    data class ModuleLibrary(val library: PLibrary) : PDependency()
    data class LinkedLibrary(val library: PLibrary) : PDependency()
}

data class PLibrary(
    val name: String,
    val classes: List<File>,
    val javadoc: List<File> = emptyList(),
    val sources: List<File> = emptyList(),
    val jarDirectories: List<File> = emptyList(),
    val annotations: List<File> = emptyList()
)

fun parse(project: Project): PProject {
    val modules = project.allprojects
        .filter { it.plugins.hasPlugin(JpsCompatiblePlugin::class.java) }
        .map { parseModule(it) }

    return PProject("Kotlin", project.rootProject.projectDir, modules, emptyList())
}

private val CONFIGURATION_MAPPING = mapOf(
    listOf("compile") to POrderRoot.Scope.COMPILE,
    listOf("compileOnly") to POrderRoot.Scope.PROVIDED,
    listOf("runtime") to POrderRoot.Scope.RUNTIME,
    listOf("testCompile", "testRuntime") to POrderRoot.Scope.TEST
)

private val SOURCE_SET_MAPPING = mapOf(
    "main" to PSourceRoot.Kind.PRODUCTION,
    "test" to PSourceRoot.Kind.TEST
)

private fun parseModule(project: Project): PModule {
    return PModule(
        project.name,
        project.projectDir,
        File(project.projectDir, project.name + ".iml"),
        parseContentRoots(project),
        parseDependencies(project)
    )
}

private fun parseContentRoots(project: Project): List<PContentRoot> {
    val excludedDirs = (project.plugins.findPlugin(IdeaPlugin::class.java)
        ?.model?.module?.excludeDirs?.toList()
            ?: emptyList()) + project.buildDir

    val (mainRoots, otherRoots) = parseSourceRoots(project).partition { it.path.startsWith(project.projectDir) }

    val mainContentRoot = PContentRoot(project.projectDir, mainRoots, excludedDirs)

    val otherContentRoots = otherRoots.map {
        val contentRootPath = if (it.path.name in listOf("src", "test", "tests")) it.path.parentFile else it.path
        PContentRoot(contentRootPath, listOf(it), emptyList())
    }

    return listOf(mainContentRoot) + otherContentRoots
}

private fun parseSourceRoots(project: Project): List<PSourceRoot> {
    if (!project.plugins.hasPlugin(JavaPlugin::class.java)) {
        return emptyList()
    }

    val sourceRoots = mutableListOf<PSourceRoot>()

    project.configure<JavaPluginConvention> {
        for ((sourceSetName, kind) in SOURCE_SET_MAPPING) {
            val sourceSet = sourceSets.findByName(sourceSetName) ?: continue

            fun Any.getKotlin(): SourceDirectorySet {
                val kotlinMethod = javaClass.getMethod("getKotlin")
                val oldIsAccessible = kotlinMethod.isAccessible
                try {
                    kotlinMethod.isAccessible = true
                    return kotlinMethod(this) as SourceDirectorySet
                } finally {
                    kotlinMethod.isAccessible = oldIsAccessible
                }
            }

            val kotlinSourceDirectories = (sourceSet as HasConvention).convention.plugins
                    .getValue("kotlin").getKotlin().srcDirs

            val directories = (sourceSet.java.sourceDirectories.files + kotlinSourceDirectories).toList()
                .filter { it.exists() }
                .takeIf { it.isNotEmpty() }
                    ?: continue

            for (resourceRoot in sourceSet.resources.sourceDirectories.files) {
                if (!resourceRoot.exists() || resourceRoot in directories) {
                    continue
                }

                val resourceRootKind = when (kind) {
                    PSourceRoot.Kind.PRODUCTION -> PSourceRoot.Kind.RESOURCES
                    PSourceRoot.Kind.TEST -> PSourceRoot.Kind.TEST_RESOURCES
                    else -> error("Invalid source root kind $kind")
                }

                sourceRoots += PSourceRoot(resourceRoot, resourceRootKind)
            }

            for (directory in directories) {
                sourceRoots += PSourceRoot(directory, kind)
            }
        }
    }

    return sourceRoots
}

private fun parseDependencies(project: Project): List<POrderRoot> {
    with(project.configurations) {
        listOf("testCompileOnly")
            .mapNotNull { findByName(it) }
            .forEach {
                if (it.dependencies.isNotEmpty()) {
                    error("$project: JPS model does not support ${it.name} configuration")
                }
            }

        val orderRoots = mutableListOf<POrderRoot>()

        for ((configurationNames, scope) in CONFIGURATION_MAPPING) {
            for (configurationName in configurationNames) {
                val configuration = findByName(configurationName)?.also { it.resolve() } ?: continue
                val isExported = scope == POrderRoot.Scope.COMPILE || scope == POrderRoot.Scope.TEST

                for (dependency in configuration.dependencies) {
                    if (dependency is ProjectDependency) {
                        orderRoots += POrderRoot(PDependency.Module(dependency.dependencyProject.name), scope, isExported)
                    } else if (dependency is SelfResolvingDependency) {
                        val library = PLibrary(
                            dependency.name,
                            classes = dependency.resolve().toList(),
                            jarDirectories = emptyList(),
                            javadoc = emptyList(),
                            sources = emptyList(),
                            annotations = emptyList()
                        )

                        orderRoots += POrderRoot(PDependency.ModuleLibrary(library), scope, isExported)
                    } else if (dependency is ExternalModuleDependency) {
                        val library = PLibrary(
                            dependency.name,
                            classes = findArtifacts(project, configuration, dependency),
                            javadoc = emptyList(),
                            sources = emptyList(),
                            jarDirectories = emptyList(),
                            annotations = emptyList()
                        )

                        orderRoots += POrderRoot(PDependency.ModuleLibrary(library), scope, isExported)
                    } else {
                        error("$project: Do not know how to resolve dependency $dependency")
                    }
                }
            }
        }

        return orderRoots
    }
}

private fun findArtifacts(project: Project, configuration: Configuration, dependency: ExternalModuleDependency): List<File> {
    fun ResolvedDependency.isNeedle() = with (module.id) {
        name == dependency.name && group == dependency.group && version == dependency.version
    }

    val resolvedDependency = configuration.resolvedConfiguration.firstLevelModuleDependencies
        .singleOrNull { it.isNeedle() }
            ?: error("$project: Can not find $dependency in resolved configuration")

    try {
        return resolvedDependency.allModuleArtifacts.map { it.file }
    } catch (e: StackOverflowError) {
        error("$project: Cycle in ${dependency.name}")
    }
}