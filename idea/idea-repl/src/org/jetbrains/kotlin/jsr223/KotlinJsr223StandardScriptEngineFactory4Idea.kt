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

package org.jetbrains.kotlin.jsr223

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_STDLIB_JAR
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Manifest
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

@Suppress("unused") // used in javax.script.ScriptEngineFactory META-INF file
class KotlinJsr223StandardScriptEngineFactory4Idea : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmScriptEngine4Idea(
                    this,
                    scriptCompilationClasspathFromContext(KOTLIN_SCRIPT_RUNTIME_JAR, Thread.currentThread().contextClassLoader),
                    "kotlin.script.templates.standard.ScriptTemplateWithBindings",
                    { ctx, argTypes -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), argTypes ?: emptyArray()) },
                    arrayOf(Map::class)
            )
}

// TODO: some common parts with the code from script-utils, consider placing in a shared lib

private fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        }
        catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

private fun classpathFromClassloader(classLoader: ClassLoader): List<File>? =
        generateSequence(classLoader) { it.parent }.toList().flatMap { (it as? URLClassLoader)?.urLs?.mapNotNull(URL::toFile) ?: emptyList() }

// Maven runners sometimes place classpath into the manifest, so we can use it for a fallback search
private fun manifestClassPath(classLoader: ClassLoader): List<File>? =
        classLoader.getResources("META-INF/MANIFEST.MF")
                .asSequence()
                .mapNotNull { ifFailed(null) { it.openStream().use { Manifest().apply { read(it) } } } }
                .flatMap { it.mainAttributes?.getValue("Class-Path")?.splitToSequence(" ") ?: emptySequence() }
                .mapNotNull { ifFailed(null) { File(URI.create(it)) } }
                .toList()
                .let { if (it.isNotEmpty()) it else null }

private inline fun <R> ifFailed(default: R, block: () -> R) = try {
    block()
} catch (t: Throwable) {
    default
}

private fun File.matchMaybeVersionedFile(baseName: String) =
        name == baseName ||
        name == baseName.removeSuffix(".jar") || // for classes dirs
        Regex(Regex.escape(baseName.removeSuffix(".jar")) + "(-\\d.*)?\\.jar").matches(name)

private const val KOTLIN_STDLIB_JAR = KOTLIN_JAVA_STDLIB_JAR
private const val KOTLIN_SCRIPT_RUNTIME_JAR = KOTLIN_JAVA_SCRIPT_RUNTIME_JAR

private fun File.existsOrNull(): File? = existsAndCheckOrNull { true }
private inline fun File.existsAndCheckOrNull(check: (File.() -> Boolean)): File? = if (exists() && check()) this else null

private fun <T> Iterable<T>.anyOrNull(predicate: (T) -> Boolean) = if (any(predicate)) this else null

private fun contextClasspath(keyName: String, classLoader: ClassLoader): List<File>? =
        (classpathFromClassloader(classLoader)?.anyOrNull { it.matchMaybeVersionedFile(keyName) }
         ?: manifestClassPath(classLoader)?.anyOrNull { it.matchMaybeVersionedFile(keyName) }
        )?.toList()

private val validJarExtensions = setOf("jar", "zip")

private fun scriptCompilationClasspathFromContext(keyName: String, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): List<File> =
        (System.getProperty("kotlin.script.classpath")?.split(File.pathSeparator)?.map(::File)
         ?: contextClasspath(keyName, classLoader)
        ).let {
            it?.plus(KotlinJars.kotlinScriptStandardJars) ?: KotlinJars.kotlinScriptStandardJars
        }
                .mapNotNull { it?.canonicalFile }
                .distinct()
                .filter { (it.isDirectory || (it.isFile && it.extension.toLowerCase() in validJarExtensions)) && it.exists() }


private object KotlinJars {

    private val explicitCompilerCP: List<File>? by lazy {
        System.getProperty("kotlin.compiler.classpath")?.split(File.pathSeparator)?.map(::File)
        ?: System.getProperty("kotlin.compiler.jar")?.let(::File)?.existsOrNull()?.let { listOf(it) }
    }

    private fun getLib(propertyName: String, jarName: String, markerClass: KClass<*>): File? =
            System.getProperty(propertyName)?.let(::File)?.existsOrNull()
            ?: explicitCompilerCP?.firstOrNull { it.matchMaybeVersionedFile(jarName) }?.existsOrNull()
            ?: PathUtil.getResourcePathForClass(markerClass.java).existsOrNull()

    val stdlib: File? by lazy { getLib("kotlin.java.runtime.jar", KOTLIN_STDLIB_JAR, JvmStatic::class) }

    val scriptRuntime: File? by lazy { getLib("kotlin.script.runtime.jar", KOTLIN_SCRIPT_RUNTIME_JAR, ScriptTemplateWithArgs::class) }

    val kotlinScriptStandardJars get() = listOf(stdlib, scriptRuntime)

}

