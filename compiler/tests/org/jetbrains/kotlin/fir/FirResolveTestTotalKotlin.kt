/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.impl.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

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

        doFirResolveTestBench(firFiles, totalTransformer.transformers)
    }
}