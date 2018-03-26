/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime

@TestDataPath("\$PROJECT_ROOT")
class FirResolveTestTotalKotlin : AbstractFirResolveWithSessionTestCase() {

    override fun createEnvironment(): KotlinCoreEnvironment {

        val configurationKind = ConfigurationKind.JDK_NO_RUNTIME
        val testJdkKind = TestJdkKind.MOCK_JDK


        val javaFiles = File(".").walkTopDown().filter { file ->
            (!file.isDirectory) && !(file.path.contains("testData") || file.path.contains("resources"))
                    && (file.extension == "java")
        }.toList()

        val configuration = KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, emptyList(), javaFiles)

        val env = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        return env
    }

    override fun createSession(): FirSession {
        return object : FirSessionBase() {
            init {
                val firProvider = FirProviderImpl(this)
                registerComponent(FirProvider::class, firProvider)
                registerComponent(
                    FirSymbolProvider::class,
                    FirCompositeSymbolProvider(listOf(firProvider, JavaSymbolProvider(project), FirLibrarySymbolProviderImpl(this)))
                )
                registerComponent(FirQualifierResolver::class, FirQualifierResolverImpl(this))
                registerComponent(FirTypeResolver::class, FirTypeResolverImpl())
            }
        }
    }


    fun testTotalKotlin() {

        val testDataPath = "."
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val allFiles = root.walkTopDown().filter { file ->
            (!file.isDirectory) && !(file.path.contains("testData") || file.path.contains("resources"))
                    && (file.extension == "kt")
        }


        val ktFiles = allFiles.map {
            val text = KotlinTestUtils.doLoadFile(it)
            KotlinTestUtils.createFile(it.path, text, project)
        }


        val session = createSession()
        val builder = RawFirBuilder(session)

        val totalTransformer = FirTotalResolveTransformer()
        val firFiles = ktFiles.map {
            val firFile = builder.buildFirFile(it)
            (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
            firFile
        }.toList()


        println("Raw FIR up, files: ${firFiles.size}")

        val timePerTransformer = mutableMapOf<KClass<*>, Long>()
        val counterPerTransformer = mutableMapOf<KClass<*>, Long>()
        val totalLength = 0
        var resolvedTypes = 0
        var errorTypes = 0
        var unresolvedTypes = 0


        try {
            for (transformer in totalTransformer.transformers) {
                for (file in firFiles) {
                    val time = measureNanoTime {
                        try {
                            transformer.transformFile(file, null)
                        } catch (e: Exception) {
                            println("Fail in file: ${(file.psi as KtFile).virtualFilePath}")
                            throw e
                        }
                    }
                    timePerTransformer.merge(transformer::class, time) { a, b -> a + b }
                    counterPerTransformer.merge(transformer::class, 1) { a, b -> a + b }
                    //totalLength += StringBuilder().apply { FirRenderer(this).visitFile(file) }.length
                }
            }


            println("SUCCESS!")
        } finally {

            firFiles.forEach {
                it.accept(object : FirVisitorVoid() {
                    override fun visitElement(element: FirElement) {
                        element.acceptChildren(this)
                    }

                    override fun visitType(type: FirType) {
                        unresolvedTypes++
                    }

                    override fun visitResolvedType(resolvedType: FirResolvedType) {
                        resolvedTypes++
                        val type = resolvedType.type
                        if (type is ConeKotlinErrorType || type is ConeClassErrorType) {
                            errorTypes++
                        }
                    }
                })
            }

            println("TOTAL LENGTH: $totalLength")
            println("UNRESOLVED TYPES: $unresolvedTypes")
            println("RESOLVED TYPES: $resolvedTypes")
            println("GOOD TYPES: ${resolvedTypes - errorTypes}")
            println("ERROR TYPES: $errorTypes")

            timePerTransformer.forEach { (transformer, time) ->
                val counter = counterPerTransformer[transformer]!!
                println("${transformer.simpleName}, TIME: ${time * 1e-6} ms, TIME PER FILE: ${(time / counter) * 1e-6} ms, FILES: $counter")
            }
        }
    }
}