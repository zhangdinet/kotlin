package org.jetbrains.kotlin.pill

import java.io.File

object KotlinSpecific {
    private val distLibraries = listOf(
        "kotlin-stdlib",
        "kotlin-stdlib-js",
        "kotlin-test-junit",
        "kotlin-test-js",
        "kotlin-stdlib-jdk7",
        "kotlin-stdlib-jdk8",
        "kotlin-script-runtime",
        "kotlin-reflect",
        "kotlin-runtime",
        "kotlin-annotations-jvm"
    )

    private val explicitLibraries = listOf(
        "gradle-plugin" to "ideaSDK/plugins/gradle/lib",
        "android-jps-plugin" to "ideaSDK/plugins/android/lib/jps",
        "android-plugin" to "ideaSDK/plugins/android/lib",
        "junit-plugin" to "ideaSDK/plugins/junit/lib",
        "intellijlang-plugin" to "ideaSDK/plugins/IntelliLang/lib",
        "java-i18n-plugin" to "ideaSDK/plugins/java-i18n/lib",
        "testng-plugin" to "ideaSDK/plugins/testng/lib",
        "properties-plugin" to "ideaSDK/plugins/properties/lib",
        "coverage-plugin" to "ideaSDK/plugins/coverage/lib",
        "copyright-plugin" to "ideaSDK/plugins/copyright/lib",
        "groovy-plugin" to "ideaSDK/plugins/Groovy/lib",
        "gradle-plugin" to "ideaSDK/plugins/gradle/lib",
        "maven-plugin" to "ideaSDK/plugins/maven/lib",
        "java-decompiler-plugin" to "ideaSDK/plugins/java-decompiler/lib",
        "intellij-core" to "ideaSDK/core",
        "idea-full" to "ideaSDK/lib",
        "idea-jps-plugin-test" to "ideaSDK/jps/test",
        "idea-jps-plugin" to "ideaSDK/jps",
        "kotlin-coroutines-jdk8" to "dependencies/kotlinx-coroutines-jdk8.jar"
    )

    private val otherSpecialDependencies = listOf(
        "kotlin-test-jvm",
        "protobuf-lite"
    )

    fun getProjectLibraries(project: PProject): List<PLibrary> {
        fun distJar(name: String) = File(project.rootDirectory, "dist/kotlinc/lib/$name.jar")
        fun projectFile(path: String) = File(project.rootDirectory, path)

        var libraries = distLibraries
            .map { PLibrary(
                it,
                classes = listOf(distJar(it)),
                sources = listOf(distJar("$it-sources"))
            ) }

        libraries += PLibrary(
            "kotlin-test-jvm",
            classes = listOf(distJar("kotlin-test")),
            sources = listOf(distJar("kotlin-test-sources"))
        )

        libraries += PLibrary(
            "protobuf-lite",
            classes = listOf(projectFile("custom-dependencies/protobuf-lite/build/jars/protobuf-2.6.1-relocated.jar"))
        )

        // Add explicit dependencies
        for ((name, relativePath) in explicitLibraries) {
            val file = File(project.rootDirectory, relativePath)

            libraries += if (file.isDirectory) {
                PLibrary(name, classes = emptyList(), jarDirectories = listOf(file))
            } else {
                PLibrary(name, classes = listOf(file))
            }
        }

        // Convert module-local dependencies to project dependencies
        val moduleLibraries = project.modules
            .flatMap { it.orderRoots }
            .map { it.dependency }
            .filterIsInstance<PDependency.LinkedLibrary>()
            .map { it.library }
            .distinct()

        outer@ for (lib in moduleLibraries) {
            val name = lib.renderName()
            if (libraries.any { it.name == name }) {
                continue@outer
            }

            libraries += lib.copy(name = name)
        }

        return libraries
    }

    fun postProcess(project: PProject): PProject {
        return project.copy(modules = project.modules.map { module ->
            module.copy(
                orderRoots = module.orderRoots
                    .mapNotNull { root ->
                        root.copy(dependency = postProcess(project, root.dependency) ?: return@mapNotNull null)
                    }
                    .distinctBy { it.dependency }
            )
        })
    }

    private fun postProcess(project: PProject, dependency: PDependency): PDependency? {
        if (dependency is PDependency.ModuleLibrary) {
            val classPaths = dependency.library.classes + dependency.library.jarDirectories

            for (lib in explicitLibraries) {
                val libraryPath = File(project.rootDirectory, lib.second)
                if (classPaths.all { it.startsWith(libraryPath) }) {
                    return PDependency.Library(lib.first)
                }
            }

            if (classPaths.any { it.startsWith(project.rootDirectory) }) {
                return PDependency.LinkedLibrary(dependency.library)
            }
        }

        if (dependency is PDependency.Module) {
            if (dependency.name == "kotlin-compiler" || dependency.name == "kotlin-stdlib-js") {
                return null
            }

            if (dependency.name == "kotlin-reflect-api") {
                return PDependency.Library("kotlin-reflect")
            }

            if (dependency.name in distLibraries || dependency.name in otherSpecialDependencies) {
                return PDependency.Library(dependency.name)
            }
        }

        return dependency
    }
}