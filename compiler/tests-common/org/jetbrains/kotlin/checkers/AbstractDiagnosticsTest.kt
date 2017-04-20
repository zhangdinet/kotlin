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

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProjectImpl
import org.jetbrains.kotlin.analyzer.common.CommonAnalyzerFacade
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.js.resolve.JsAnalyzerFacade
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.USE_NEW_INFERENCE
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL
import org.jetbrains.kotlin.utils.keysToMap
import org.junit.Assert
import java.io.File
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.set

abstract class AbstractDiagnosticsTest : BaseDiagnosticsTest() {
    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val groupedByModule = files.groupBy(TestFile::module)

        if (groupedByModule.values.count { filesInModule -> filesInModule.any { it.fileName.endsWith(".java") } } > 1) {
            // This is not supported yet because:
            // 1) All .java files are created in the same directory and that directory is added as a JavaSourceRoot
            //    to the compiler configuration in KotlinMultiFileTestWithJava
            // 2) AllJavaSourcesInProjectScope is used below in each modules scope, making each module contain all .java files
            throw AssertionError(".java sources in more than one module are not supported yet in this test")
        }

        var lazyOperationsLog: LazyOperationsLog? = null

        val tracker = ExceptionTracker()
        val storageManager: StorageManager
        if (files.any(TestFile::checkLazyLog)) {
            lazyOperationsLog = LazyOperationsLog(HASH_SANITIZER)
            storageManager = LoggingStorageManager(
                    LockBasedStorageManager.createWithExceptionHandling(tracker),
                    lazyOperationsLog.addRecordFunction
            )
        }
        else {
            storageManager = LockBasedStorageManager.createWithExceptionHandling(tracker)
        }

        val context = SimpleGlobalContext(storageManager, tracker)

        val libraryAndSdk = getLibraryAndSdkDependency()

        val ktFilesByModule: MutableMap<TestModule, List<KtFile>> =
                groupedByModule.keys.keysToMap { testModule -> getKtFiles(groupedByModule[testModule]!!, true) }.toMutableMap()

        for (moduleInfo in ktFilesByModule.keys) {
            if (moduleInfo.platform == TargetPlatform.Common) continue

            val commonModules = moduleInfo.dependencies().filter { it.platform == TargetPlatform.Common }
            assert(commonModules.size <= 1) { "Multiple common modules in dependencies is not supported yet in this test" }

            val commonModule = commonModules.singleOrNull() ?: continue
            ktFilesByModule[moduleInfo] = ktFilesByModule[moduleInfo]!! + ktFilesByModule[commonModule]!!
        }

        val languageSettingsProvider = object : LanguageSettingsProvider {
            override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings {
                return when (moduleInfo) {
                    is TestModule -> loadLanguageVersionSettings(groupedByModule[moduleInfo]!!)
                    else -> {
                        // If there's only one module in a test (majority of tests), we use its LanguageVersionSettings for SDK as well.
                        // Otherwise we use default LanguageVersionSettings
                        groupedByModule.values.singleOrNull()?.let { loadLanguageVersionSettings(it) }
                        ?: LanguageVersionSettingsImpl.DEFAULT
                    }
                }
            }

            override fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion {
                // TODO: support JVM_TARGET directive in diagnostic tests
                return TargetPlatformVersion.NoVersion
            }
        }

        fun computeModuleScope(module: ModuleInfo): GlobalSearchScope {
            return when (module) {
                is TestModule -> {
                    GlobalSearchScope.filesScope(project, ktFilesByModule[module]!!.map { it.virtualFile })
                            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
                }
                is TestJvmBinaryModuleInfo -> {
                    GlobalSearchScope.notScope(GlobalSearchScope.union(groupedByModule.keys.map(::computeModuleScope).toTypedArray()))
                }
                libraryAndSdk -> GlobalSearchScope.EMPTY_SCOPE
                else -> error("Unknown module info: $module")
            }
        }

        val resolverForProject = ResolverForProjectImpl(
                "diagnostic tests",
                context.withProject(project),
                groupedByModule.keys + libraryAndSdk,
                analyzerFacade = { module ->
                    when (module.platform!!) {
                        JvmPlatform -> JvmAnalyzerFacade
                        JsPlatform -> JsAnalyzerFacade
                        TargetPlatform.Common -> CommonAnalyzerFacade
                        else -> error("Unknown platform: ${module.platform} (module=$module)")
                    }
                },
                modulesContent = { module ->
                    ModuleContent(ktFilesByModule[module].orEmpty(), computeModuleScope(module))
                },
                platformParameters = JvmPlatformParameters { javaClass ->
                    when (javaClass) {
                        is BinaryJavaClass -> TestJvmBinaryModuleInfo
                        is JavaClassImpl -> groupedByModule.keys.singleOrNull { module ->
                            module.platform.multiTargetPlatform != MultiTargetPlatform.Common &&
                            javaClass.psi.containingFile.virtualFile in computeModuleScope(module)
                        } ?: TestJvmBinaryModuleInfo
                        else -> error("Unknown Java class: $javaClass (${javaClass.javaClass})")
                    }
                },
                languageSettingsProvider = languageSettingsProvider,
                builtIns = { module ->
                    when (module.platform) {
                        JvmPlatform -> JvmBuiltIns(context.storageManager)
                        JsPlatform -> JsPlatform.builtIns
                        TargetPlatform.Common -> DefaultBuiltIns.Instance
                        else -> error("Unknown platform: ${module.platform}")
                    }
                },
                packagePartProviderFactory = { _, moduleContent -> environment.createPackagePartProvider(moduleContent.moduleContentScope) },
                firstDependency = null
        )

        resolverForProject.resolverForModule(libraryAndSdk)

        val modules: Map<TestModule, ModuleDescriptor> = groupedByModule.keys.keysToMap(resolverForProject::descriptorForModule)

        for (moduleInfo in resolverForProject.allModules) {
            val moduleDescriptor = resolverForProject.descriptorForModule(moduleInfo)
            val builtIns = moduleDescriptor.builtIns
            if (builtIns is JvmBuiltIns) {
                val languageVersionSettings = languageSettingsProvider.getLanguageVersionSettings(moduleInfo, project)
                builtIns.initialize(moduleDescriptor, languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
            }
        }

        val moduleBindings = HashMap<TestModule, BindingContext>()

        for (testModule in groupedByModule.keys) {
            val ktFiles = ktFilesByModule[testModule]!!

            val resolverForModule = resolverForProject.resolverForModule(testModule)
            val moduleDescriptor = resolverForProject.descriptorForModule(testModule)
            val container = resolverForModule.componentProvider
            val trace = container.get<BindingTrace>()

            beforeAnalysisStarted(trace, moduleDescriptor, ktFiles)

            container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, ktFiles)

            onAnalysisCompleted(trace, moduleDescriptor, ktFiles)

            moduleBindings[testModule] = trace.bindingContext
            checkAllResolvedCallsAreCompleted(ktFiles, trace.bindingContext)
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        var exceptionFromLazyResolveLogValidation: Throwable? = null
        if (lazyOperationsLog != null) {
            exceptionFromLazyResolveLogValidation = checkLazyResolveLog(lazyOperationsLog, testDataFile)
        }
        else {
            val lazyLogFile = getLazyLogFile(testDataFile)
            assertFalse("No lazy log expected, but found: ${lazyLogFile.absolutePath}", lazyLogFile.exists())
        }

        var exceptionFromDescriptorValidation: Throwable? = null
        try {
            val expectedFile = if (InTextDirectivesUtils.isDirectiveDefined(testDataFile.readText(), "// JAVAC_EXPECTED_FILE")
                                   && environment.configuration.getBoolean(JVMConfigurationKeys.USE_JAVAC)) {
                File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".javac.txt")
            } else {
                File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".txt")
            }
            validateAndCompareDescriptorWithFile(expectedFile, files, modules)
        }
        catch (e: Throwable) {
            exceptionFromDescriptorValidation = e
        }

        // main checks
        var ok = true

        val actualText = StringBuilder()
        for (testFile in files) {
            val module = testFile.module
            val isCommonModule = modules[module]!!.getMultiTargetPlatform() == MultiTargetPlatform.Common
            val implementingModules =
                    if (!isCommonModule) emptyList()
                    else modules.entries.filter { (testModule) -> module in testModule.dependencies() }
            val implementingModulesBindings = implementingModules.mapNotNull {
                (testModule, moduleDescriptor) ->
                val platform = moduleDescriptor.getMultiTargetPlatform()
                if (platform is MultiTargetPlatform.Specific) platform to moduleBindings[testModule]!!
                else null
            }
            ok = ok and testFile.getActualText(
                    moduleBindings[module]!!, implementingModulesBindings, actualText,
                    shouldSkipJvmSignatureDiagnostics(groupedByModule) || isCommonModule
            )
        }

        var exceptionFromDynamicCallDescriptorsValidation: Throwable? = null
        try {
            val expectedFile = File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".dynamic.txt")
            checkDynamicCallDescriptors(expectedFile, files)
        }
        catch (e: Throwable) {
            exceptionFromDynamicCallDescriptorsValidation = e
        }

        KotlinTestUtils.assertEqualsToFile(testDataFile, actualText.toString())

        assertTrue("Diagnostics mismatch. See the output above", ok)

        // now we throw a previously found error, if any
        exceptionFromDescriptorValidation?.let { throw it }
        exceptionFromLazyResolveLogValidation?.let { throw it }
        exceptionFromDynamicCallDescriptorsValidation?.let { throw it }

        performAdditionalChecksAfterDiagnostics(testDataFile, files, groupedByModule, modules, moduleBindings)
    }

    object TestJvmBinaryModuleInfo : ModuleInfo {
        override val name: Name
            get() = Name.special("<JVM binary dependencies>")

        override fun dependencies(): List<ModuleInfo> = listOf(this)

        override val platform: TargetPlatform
            get() = JvmPlatform
    }

    override fun getLibraryAndSdkDependency(): ModuleInfo =
            TestJvmBinaryModuleInfo

    protected open fun onAnalysisCompleted(trace: BindingTrace, module: ModuleDescriptor, files: List<KtFile>) {
    }

    protected open fun beforeAnalysisStarted(trace: BindingTrace, module: ModuleDescriptor, files: List<KtFile>) {
    }

    protected open fun performAdditionalChecksAfterDiagnostics(
            testDataFile: File,
            testFiles: List<TestFile>,
            moduleFiles: Map<TestModule, List<TestFile>>,
            moduleDescriptors: Map<TestModule, ModuleDescriptor>,
            moduleBindings: Map<TestModule, BindingContext>
    ) {
        // To be overridden by diagnostic-like tests.
    }

    protected open fun loadLanguageVersionSettings(module: List<TestFile>): LanguageVersionSettings {
        var result: LanguageVersionSettings? = null
        for (file in module) {
            val current = file.customLanguageVersionSettings
            if (current != null) {
                if (result != null && result != current) {
                    Assert.fail(
                            "More than one file in the module has $LANGUAGE_DIRECTIVE or $API_VERSION_DIRECTIVE directive specified. " +
                            "This is not supported. Please move all directives into one file"
                    )
                }
                result = current
            }
        }

        return result ?: BaseDiagnosticsTest.DiagnosticTestLanguageVersionSettings(
                BaseDiagnosticsTest.DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
                LanguageVersionSettingsImpl.DEFAULT.apiVersion,
                LanguageVersionSettingsImpl.DEFAULT.languageVersion
        )
    }

    private fun checkDynamicCallDescriptors(expectedFile: File, testFiles: List<TestFile>) {
        val serializer = RecursiveDescriptorComparator(RECURSIVE_ALL)

        val actualText = StringBuilder()

        for (testFile in testFiles) {
            for (descriptor in testFile.dynamicCallDescriptors) {
                val actualSerialized = serializer.serializeRecursively(descriptor)
                actualText.append(actualSerialized)
            }
        }

        if (actualText.isNotEmpty() || expectedFile.exists()) {
            KotlinTestUtils.assertEqualsToFile(expectedFile, actualText.toString())
        }
    }

    protected open fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<TestModule, List<TestFile>>): Boolean =
            groupedByModule.size > 1

    private fun checkLazyResolveLog(lazyOperationsLog: LazyOperationsLog, testDataFile: File): Throwable? =
            try {
                val expectedFile = getLazyLogFile(testDataFile)
                KotlinTestUtils.assertEqualsToFile(expectedFile, lazyOperationsLog.getText(), HASH_SANITIZER)
                null
            }
            catch (e: Throwable) {
                e
            }

    private fun getLazyLogFile(testDataFile: File): File =
            File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".lazy.log")

    private fun validateAndCompareDescriptorWithFile(
            expectedFile: File,
            testFiles: List<TestFile>,
            modules: Map<TestModule, ModuleDescriptor>
    ) {
        if (skipDescriptorsValidation()) return
        if (testFiles.any { file -> InTextDirectivesUtils.isDirectiveDefined(file.expectedText, "// SKIP_TXT") }) {
            assertFalse(".txt file should not exist if SKIP_TXT directive is used: $expectedFile", expectedFile.exists())
            return
        }

        val comparator = RecursiveDescriptorComparator(createdAffectedPackagesConfiguration(testFiles, modules.values))

        val isMultiModuleTest = modules.size != 1

        val packages =
                (testFiles.flatMap {
                    InTextDirectivesUtils.findListWithPrefixes(it.expectedText, "// RENDER_PACKAGE:").map {
                        FqName(it.trim())
                    }
                } + FqName.ROOT).toSet()

        val textByPackage = packages.keysToMap { StringBuilder() }

        val sortedModules = modules.keys.sorted()

        for ((packageName, packageText) in textByPackage.entries) {
            val module = sortedModules.iterator()
            while (module.hasNext()) {
                val moduleDescriptor = modules[module.next()]!!

                val aPackage = moduleDescriptor.getPackage(packageName)
                assertFalse(aPackage.isEmpty())

                if (isMultiModuleTest) {
                    packageText.append(String.format("// -- Module: %s --\n", moduleDescriptor.name))
                }

                val actualSerialized = comparator.serializeRecursively(aPackage)
                packageText.append(actualSerialized)

                if (isMultiModuleTest && module.hasNext()) {
                    packageText.append("\n\n")
                }
            }
        }

        val allPackagesText = textByPackage.values.joinToString("\n")

        val lineCount = StringUtil.getLineBreakCount(allPackagesText)
        assert(lineCount < 1000) {
            "Rendered descriptors of this test take up $lineCount lines. " +
            "Please ensure you don't render JRE contents to the .txt file. " +
            "Such tests are hard to maintain, take long time to execute and are subject to sudden unreviewed changes anyway."
        }

        KotlinTestUtils.assertEqualsToFile(expectedFile, allPackagesText)
    }


    protected open fun skipDescriptorsValidation(): Boolean = false

    private fun getJavaFilePackage(testFile: TestFile): Name {
        val pattern = Pattern.compile("^\\s*package [.\\w\\d]*", Pattern.MULTILINE)
        val matcher = pattern.matcher(testFile.expectedText)

        if (matcher.find()) {
            return testFile.expectedText
                    .substring(matcher.start(), matcher.end())
                    .split(" ")
                    .last()
                    .filter { !it.isWhitespace() }
                    .let { Name.identifier(it.split(".").first()) }
        }

        return SpecialNames.ROOT_PACKAGE
    }

    private fun createdAffectedPackagesConfiguration(
            testFiles: List<TestFile>,
            modules: Collection<ModuleDescriptor>
    ): RecursiveDescriptorComparator.Configuration {
        val packagesNames = (
                testFiles.filter { it.ktFile == null }
                        .map { getJavaFilePackage(it) } +
                getTopLevelPackagesFromFileList(getKtFiles(testFiles, false))
                            ).toSet()

        val stepIntoFilter = Predicate<DeclarationDescriptor> { descriptor ->
            val module = DescriptorUtils.getContainingModuleOrNull(descriptor)
            if (module !in modules) return@Predicate false

            if (descriptor is PackageViewDescriptor) {
                val fqName = descriptor.fqName
                return@Predicate fqName.isRoot || fqName.pathSegments().first() in packagesNames
            }

            true
        }

        return RECURSIVE.filterRecursion(stepIntoFilter).withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed())
    }

    private fun getTopLevelPackagesFromFileList(files: List<KtFile>): Set<Name> =
            files.mapTo(LinkedHashSet<Name>()) { file ->
                file.packageFqName.pathSegments().firstOrNull() ?: SpecialNames.ROOT_PACKAGE
            }

    private fun checkAllResolvedCallsAreCompleted(ktFiles: List<KtFile>, bindingContext: BindingContext) {
        if (ktFiles.any { file -> AnalyzingUtils.getSyntaxErrorRanges(file).isNotEmpty() }) return

        val resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)
        for ((call, resolvedCall) in resolvedCallsEntries) {
            val element = call.callElement

            val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)

            if (!USE_NEW_INFERENCE) {
                assertTrue("Resolved call for '${element.text}'$lineAndColumn is not completed",
                           (resolvedCall as MutableResolvedCall<*>).isCompleted)
            }
        }

        checkResolvedCallsInDiagnostics(bindingContext)
    }

    private fun checkResolvedCallsInDiagnostics(bindingContext: BindingContext) {
        val diagnosticsStoringResolvedCalls1 = setOf(
                OVERLOAD_RESOLUTION_AMBIGUITY, NONE_APPLICABLE, CANNOT_COMPLETE_RESOLVE, UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                ASSIGN_OPERATOR_AMBIGUITY, ITERATOR_AMBIGUITY
        )
        val diagnosticsStoringResolvedCalls2 = setOf(
                COMPONENT_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
        )

        for (diagnostic in bindingContext.diagnostics) {
            when (diagnostic.factory) {
                in diagnosticsStoringResolvedCalls1 -> assertResolvedCallsAreCompleted(
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls1).a
                )
                in diagnosticsStoringResolvedCalls2 -> assertResolvedCallsAreCompleted(
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls2).b
                )
            }
        }
    }

    private fun assertResolvedCallsAreCompleted(diagnostic: Diagnostic, resolvedCalls: Collection<ResolvedCall<*>>) {
        val element = diagnostic.psiElement
        val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)
        if (USE_NEW_INFERENCE) return

        assertTrue("Resolved calls stored in ${diagnostic.factory.name}\nfor '${element.text}'$lineAndColumn are not completed",
                   resolvedCalls.all { (it as MutableResolvedCall<*>).isCompleted })
    }

    companion object {
        private val HASH_SANITIZER = fun(s: String): String = s.replace("@(\\d)+".toRegex(), "")
    }
}
