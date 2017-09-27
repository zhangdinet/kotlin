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

@file:Suppress("unused") // could be used externally in javax.script.ScriptEngineFactory META-INF file

package org.jetbrains.kotlin.script.jsr223

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.script.util.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileNotFoundException
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class KotlinJsr223JvmLocalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmLocalScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    scriptCompilationClasspathFromContext("kotlin-script-util.jar"),
                    KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                    { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
                    arrayOf(Bindings::class)
            )
}

class KotlinJsr223JvmDaemonLocalEvalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmDaemonCompileScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    KotlinJars.compilerCP,
                    scriptCompilationClasspathFromContext("kotlin-script-util.jar"),
                    KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                    { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
                    arrayOf(Bindings::class)
            )
}

private const val KOTLIN_COMPILER_JAR = "kotlin-compiler.jar"
private const val KOTLIN_COMPILER_EMBEDDABLE_JAR = "kotlin-compiler-embeddable.jar"
private const val KOTLIN_STDLIB_JAR = "kotlin-stdlib.jar"
private const val KOTLIN_REFLECT_JAR = "kotlin-reflect.jar"
private const val KOTLIN_SCRIPT_RUNTIME_JAR = "kotlin-script-runtime.jar"

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

    val compilerCP: List<File> by lazy {
        val kotlinCompilerJars = listOf(KOTLIN_COMPILER_JAR, KOTLIN_COMPILER_EMBEDDABLE_JAR)
        val kotlinLibsJars = listOf(KOTLIN_STDLIB_JAR, KOTLIN_REFLECT_JAR, KOTLIN_SCRIPT_RUNTIME_JAR)
        val kotlinBaseJars = kotlinCompilerJars + kotlinLibsJars

        val cp = explicitCompilerCP
            // search classpath from context classloader and `java.class.path` property
            ?: (classpathFromClass(Thread.currentThread().contextClassLoader, K2JVMCompiler::class)
                ?: contextClasspath(KOTLIN_COMPILER_JAR, Thread.currentThread().contextClassLoader)
                ?: classpathFromClasspathProperty()
               )?.filter { f -> kotlinBaseJars.any { f.matchMaybeVersionedFile(it) } }?.takeIf { it.isNotEmpty() }
        // if autodetected, additionaly check for presense of the compiler jars
        if (cp == null || (explicitCompilerCP == null && cp.none { f -> kotlinCompilerJars.any { f.matchMaybeVersionedFile(it) } })) {
            throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.classpath property to proper location")
        }
        cp!!
    }

    private fun getLib(propertyName: String, jarName: String, markerClass: KClass<*>): File? =
            System.getProperty(propertyName)?.let(::File)?.existsOrNull()
            ?: explicitCompilerCP?.firstOrNull { it.matchMaybeVersionedFile(jarName) }?.existsOrNull()
            ?: PathUtil.getResourcePathForClass(markerClass.java).existsOrNull()

    val stdlib: File? by lazy { getLib("kotlin.java.runtime.jar", KOTLIN_STDLIB_JAR, JvmStatic::class) }

    val scriptRuntime: File? by lazy { getLib("kotlin.script.runtime.jar", KOTLIN_SCRIPT_RUNTIME_JAR, ScriptTemplateWithArgs::class) }

    val kotlinScriptStandardJars get() = listOf(stdlib, scriptRuntime)

}

