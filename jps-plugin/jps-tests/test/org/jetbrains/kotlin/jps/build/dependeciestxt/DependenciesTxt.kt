/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build.dependeciestxt

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.jps.build.dependeciestxt.generated.DependenciesTxtLexer
import org.jetbrains.kotlin.jps.build.dependeciestxt.generated.DependenciesTxtParser
import org.jetbrains.kotlin.jps.build.dependeciestxt.generated.DependenciesTxtParser.*
import java.io.File

/**
 * Dependencies description file.
 * See [README.md] for more details.
 */
data class DependenciesTxt(val modules: List<Module>, val dependencies: List<Dependency>) {
    data class Module(
        val name: String,
        val targetPlatform: TargetPlatform
    ) {
        lateinit var jpsModule: JpsModule
    }

    data class Dependency(
        val from: Module,
        val to: Module,
        val scope: JpsJavaDependencyScope,
        val expectedBy: Boolean,
        val exported: Boolean
    )

    enum class TargetPlatform {
        COMMON, JS, JVM
    }
}

class DependenciesTxtBuilder {
    val modules = mutableMapOf<String, ModuleRef>()
    val dependencies = mutableListOf<DependencyBuilder>()

    /**
     * Reference to module which can be defined later
     */
    class ModuleRef(name: String) {
        var defined: Boolean = false
        var actual: DependenciesTxt.Module = DependenciesTxt.Module(name, DependenciesTxt.TargetPlatform.JVM)
    }

    /**
     * Temporary object for resolving references to modules.
     */
    data class DependencyBuilder(
        val from: ModuleRef,
        val to: ModuleRef,
        val scope: JpsJavaDependencyScope,
        val expectedBy: Boolean,
        val exported: Boolean
    ) {
        fun build() = DependenciesTxt.Dependency(from.actual, to.actual, scope, expectedBy, exported)
    }

    fun readFile(file: File): DependenciesTxt {
        val lexer = DependenciesTxtLexer(CharStreams.fromPath(file.toPath()))
        val parser = DependenciesTxtParser(CommonTokenStream(lexer))

        parser.file().def().forEach { def ->
            val moduleDef = def.moduleDef()
            val dependencyDef = def.dependencyDef()

            when {
                moduleDef != null -> newModule(moduleDef)
                dependencyDef != null -> newDependency(dependencyDef)
            }
        }

        return DependenciesTxt(
            modules.map { it.value.actual },
            dependencies.map { it.build() }
        )
    }

    private fun moduleRef(name: String) =
        modules.getOrPut(name) { ModuleRef(name) }

    private fun moduleRef(refNode: ModuleRefContext) =
        moduleRef(refNode.ID().text)

    fun newModule(def: ModuleDefContext): DependenciesTxt.Module {
        var platform = DependenciesTxt.TargetPlatform.JVM
        def.attrs().accept { key, value ->
            if (value == null) {
                when (key) {
                    "common" -> platform = DependenciesTxt.TargetPlatform.COMMON
                    "jvm" -> platform = DependenciesTxt.TargetPlatform.JVM
                    "js" -> platform = DependenciesTxt.TargetPlatform.JS
                    else -> error("Unknown module flag `$key`")
                }
            } else error("Unknown module property `$key`")
        }

        return DependenciesTxt.Module(def.ID().text, platform).also {
            val moduleRef = moduleRef(it.name)
            check(!moduleRef.defined) { "Module `${it.name}` already defined" }
            moduleRef.defined = true
            moduleRef.actual = it
        }
    }

    fun newDependency(def: DependencyDefContext): DependencyBuilder? {
        val from = def.moduleRef(0)
        val to = def.moduleRef(1)

        if (to == null) {
            // `x -> ` should just create undefined module `x`
            moduleRef(from)

            check(def.attrs() == null) {
                "Attributes are not allowed for `x -> ` like dependencies. Please use `x [attrs...]` syntax for module attributes."
            }

            return null
        } else {
            var exported = false
            var scope = JpsJavaDependencyScope.COMPILE
            var expectedBy = false

            def.attrs()?.accept { key, value ->
                if (value == null) {
                    when (key) {
                        "exported" -> exported = true
                        "compile" -> scope = JpsJavaDependencyScope.COMPILE
                        "test" -> scope = JpsJavaDependencyScope.TEST
                        "runtime" -> scope = JpsJavaDependencyScope.RUNTIME
                        "provided" -> scope = JpsJavaDependencyScope.PROVIDED
                        "expectedBy" -> expectedBy = true
                        else -> error("Unknown dependency flag `$key`")
                    }
                } else error("Unknown dependency property `$key`")
            }

            return DependencyBuilder(
                from = moduleRef(from),
                to = moduleRef(to),
                scope = scope,
                expectedBy = expectedBy,
                exported = exported
            ).also {
                dependencies.add(it)
            }
        }
    }

    private fun AttrsContext.accept(visit: (key: String, value: String?) -> Unit) {
        attr().forEach {
            val flagRef = it.attrFlagRef()
            val keyValue = it.attrKeyValue()

            when {
                flagRef != null -> visit(flagRef.ID().text, null)
                keyValue != null -> visit(keyValue.attrKeyRef().ID().text, keyValue.attrValueRef().ID().text)
            }
        }
    }
}