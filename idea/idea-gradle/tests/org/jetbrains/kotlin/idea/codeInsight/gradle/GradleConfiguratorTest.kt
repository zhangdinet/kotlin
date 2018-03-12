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

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test
import java.io.File

class GradleConfiguratorTest : GradleImportingTestCase() {
    private val testDir = PluginTestCaseBase.getTestDataPathBase() + "/gradle/configurator/"

    @Test
    fun testProjectWithModule() {
        configureByFiles()
        importProject()

        runInEdtAndWait {
            runWriteAction {
                // Create not configured build.gradle for project
                myProject.baseDir.createChildData(null, "build.gradle")

                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val moduleGroup = module.toModuleGroup()
                // We have a Kotlin runtime in build.gradle but not in the classpath, so it doesn't make sense
                // to suggest configuring it
                assertEquals(ConfigureKotlinStatus.BROKEN, findGradleModuleConfigurator().getStatus(moduleGroup))
                // Don't offer the JS configurator if the JVM configuration exists but is broken
                assertEquals(ConfigureKotlinStatus.BROKEN, findJsGradleModuleConfigurator().getStatus(moduleGroup))
            }
        }
    }

    @Test
    fun testConfigure10() {
        val files = configureByFiles()

        importProject()

        runInEdtAndWait {
            runWriteAction {
                val module = myTestFixture.module
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(module), "1.0.6", collector)

                checkFiles(files)
            }
        }
    }

    @TargetVersions("3.5")
    @Test
    fun testConfigureGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            runWriteAction {
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(myTestFixture.module), "1.1.2", collector)

                importProject()
                checkFiles(files)
                checkForDiagnostics(files)
            }
        }
    }

    @TargetVersions("4.5")
    @Test
    fun testConfigureGSK45() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            runWriteAction {
                val configurator = findGradleModuleConfigurator()
                val collector = createConfigureKotlinNotificationCollector(myProject)
                configurator.configureWithVersion(myProject, listOf(myTestFixture.module), "1.1.2", collector)

                checkFiles(files)
                checkForDiagnostics(files)
            }
        }
    }

    @Test
    fun testListNonConfiguredModules() {
        configureByFiles()

        importProject()

        runReadAction {
            val configurator = findGradleModuleConfigurator()

            val moduleNames = getCanBeConfiguredModulesWithKotlinFiles(myProject).map { it.name }
            assertSameElements(moduleNames, "app")

            val moduleNamesFromConfigurator = getCanBeConfiguredModules(myProject, configurator).map { it.name }
            assertSameElements(moduleNamesFromConfigurator, "app")

            val moduleNamesWithKotlinFiles = getCanBeConfiguredModulesWithKotlinFiles(myProject, configurator).map { it.name }
            assertSameElements(moduleNamesWithKotlinFiles, "app")
        }
    }

    @Test
    fun testListNonConfiguredModulesConfigured() {
        configureByFiles()

        importProject()

        runReadAction {
            assertEmpty(getCanBeConfiguredModulesWithKotlinFiles(myProject))
        }
    }

    @Test
    fun testListNonConfiguredModulesConfiguredOnlyTest() {
        configureByFiles()

        importProject()

        runReadAction {
            assertEmpty(getCanBeConfiguredModulesWithKotlinFiles(myProject))
        }
    }

    @Test
    fun testAddNonKotlinLibraryGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    myTestFixture.module,
                    DependencyScope.COMPILE,
                    object : ExternalLibraryDescriptor("org.a.b", "lib", "1.0.0", "1.0.0") {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })
            }

            checkFiles(files)
            checkForDiagnostics(files)
        }
    }

    @Test
    fun testAddLibraryGSKWithKotlinVersion() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                val stdLibVersion = KotlinWithGradleConfigurator.getKotlinStdlibVersion(myTestFixture.module)
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    myTestFixture.module,
                    DependencyScope.COMPILE,
                    object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", stdLibVersion, stdLibVersion) {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })
            }

            checkFiles(files)
            checkForDiagnostics(files)
        }
    }

    @Test
    fun testAddTestLibraryGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    myTestFixture.module,
                    DependencyScope.TEST,
                    object : ExternalLibraryDescriptor("junit", "junit", "4.12", "4.12") {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })

                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    myTestFixture.module,
                    DependencyScope.TEST,
                    object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-test", "1.1.2", "1.1.2") {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })
            }

            checkFiles(files)
            checkForDiagnostics(files)
        }
    }

    @Test
    fun testAddLibraryGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    myTestFixture.module,
                    DependencyScope.COMPILE,
                    object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.0.0", "1.0.0") {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })
            }

            checkFiles(files)
            checkForDiagnostics(files)
        }
    }

    @Test
    fun testAddCoroutinesSupport() {
        val files = configureByFiles()

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            checkFiles(files)
        }
    }

    @Test
    fun testAddCoroutinesSupportGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            checkFiles(files)
            checkForDiagnostics(files)
        }
    }

    @Test
    fun testChangeCoroutinesSupport() {
        val files = configureByFiles()

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            checkFiles(files)
        }
    }

    @Test
    fun testChangeCoroutinesSupportGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeCoroutineConfiguration(myTestFixture.module, "enable")
            }

            checkFiles(files)
        }
    }

    @Test
    fun testAddLanguageVersion() {
        val files = configureByFiles()

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            checkFiles(files)
        }
    }

    @Test
    fun testAddLanguageVersionGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            checkFiles(files)
            checkForDiagnostics(files)
        }
    }

    @Test
    fun testChangeLanguageVersion() {
        val files = configureByFiles()

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            checkFiles(files)
        }
    }

    @Test
    fun testChangeLanguageVersionGSK() {
        val files = configureByFiles()

        importProject()
        checkForDiagnostics(files)

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeLanguageVersion(myTestFixture.module, "1.1", null, false)
            }

            checkFiles(files)
        }
    }

    @Test
    fun testAddLibrary() {
        val files = configureByFiles()

        importProject()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    myTestFixture.module,
                    DependencyScope.COMPILE,
                    object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.0.0", "1.0.0") {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })
            }

            checkFiles(files)
        }
    }

    private fun checkForDiagnostics(files: List<VirtualFile>) {
        for (scriptFile in files) {
            val psiFile = runReadAction { PsiManager.getInstance(myProject).findFile(scriptFile) as? KtFile } ?: continue
            runInEdtAndWait {
                ScriptDependenciesManager.updateScriptDependenciesSynchronously(scriptFile, myProject)
                val bindingContext = psiFile.analyzeWithContent()
                val errors = bindingContext.diagnostics.filter { it.severity == Severity.ERROR }
                assert(errors.isEmpty()) {
                    errors.joinToString(
                        separator = "\n",
                        prefix = "Compilation error for ${scriptFile.path}:\n${psiFile.text}\n"
                    ) {
                        DefaultErrorMessages.render(it)
                    }
                }
            }
        }
    }


    private fun configureByFiles(): List<VirtualFile> {
        val rootDir = rootDir()
        assert(rootDir.exists()) { "Directory ${rootDir.path} doesn't exist"}

        return rootDir.walk().mapNotNull {
            when {
                it.isDirectory -> null
                !it.name.endsWith(SUFFIX) -> {
                    createProjectSubFile(it.path.substringAfter(rootDir.path + File.separator), it.readText())
                }
                else -> null
            }
        }.toList()
    }

    private fun checkFiles(files: List<VirtualFile>) {
        FileDocumentManager.getInstance().saveAllDocuments()

        files.filter { it.name == GradleConstants.DEFAULT_SCRIPT_NAME || it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME }
            .forEach {
                val actualText = LoadTextUtil.loadText(it).toString()
                KotlinTestUtils.assertEqualsToFile(File(rootDir(), it.name + SUFFIX), actualText)
            }
    }

    private fun rootDir() = File(testDir, getTestName(true).substringBefore("_"))

    private fun findGradleModuleConfigurator() = Extensions.findExtension(
        KotlinProjectConfigurator.EP_NAME,
        KotlinGradleModuleConfigurator::class.java
    )

    private fun findJsGradleModuleConfigurator() = Extensions.findExtension(
        KotlinProjectConfigurator.EP_NAME,
        KotlinJsGradleModuleConfigurator::class.java
    )

    companion object {
        private val SUFFIX = ".after"
    }
}
