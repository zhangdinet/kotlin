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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.idea.util.isKotlinBinary
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.script.getScriptDefinition
import org.jetbrains.kotlin.utils.sure
import kotlin.coroutines.experimental.buildSequence

fun PsiElement.getModuleInfo(): IdeaModuleInfo =
        this.processInfos { LOG.error("Could not find correct module information.\nReason: $it") }.firstOrNull() ?: NotUnderContentRootModuleInfo

fun PsiElement.getNullableModuleInfo(): IdeaModuleInfo? =
        this.processInfos { LOG.warn("Could not find correct module information.\nReason: $it") }.firstOrNull()

fun PsiElement.getModuleInfos(): List<IdeaModuleInfo> =
        this.processInfos { LOG.warn("Could not find correct module information.\nReason: $it") }.filterNotNull().toList()

fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile): IdeaModuleInfo? {
    return processVirtualFile(project, virtualFile, treatAsLibrarySource = false).firstOrNull()
}

fun getBinaryLibrariesModuleInfos(project: Project, virtualFile: VirtualFile) = collectModuleInfosByType<BinaryModuleInfo>(project, virtualFile)
fun getLibrarySourcesModuleInfos(project: Project, virtualFile: VirtualFile) = collectModuleInfosByType<LibrarySourceInfo>(project, virtualFile)

private fun PsiElement.processInfos(onFailure: (String) -> Unit): Sequence<IdeaModuleInfo?> = buildSequence {
    (containingFile?.moduleInfo as? IdeaModuleInfo)?.let {
        yield(it)
        return@buildSequence
    }

    if (this is KtLightElement<*, *>) {
        yieldAll(processLightElement(this, onFailure))
        return@buildSequence
    }

    val containingFile = containingFile
    if (containingFile == null) {
        onFailure("Analyzing element of type ${this::class.java} with no containing file\nText:\n$text")
        return@buildSequence
    }

    val containingKtFile = (this as? KtElement)?.containingFile as? KtFile
    if (containingKtFile != null) {
        containingKtFile.analysisContext?.let {
            yieldAll(it.processInfos(onFailure))
            return@buildSequence
        }

        containingKtFile.doNotAnalyze?.let {
            onFailure("Should not analyze element: $text in file ${containingKtFile.name}\n$it")
            return@buildSequence
        }

        val explicitModuleInfo = containingKtFile.moduleInfo ?: (containingKtFile.originalFile as? KtFile)?.moduleInfo
        if (explicitModuleInfo is IdeaModuleInfo) {
            yield(explicitModuleInfo)
            return@buildSequence
        }

        if (containingKtFile is KtCodeFragment) {
            val context = containingKtFile.getContext()
            if (context == null) {
                onFailure("Analyzing code fragment of type ${containingKtFile::class.java} with no context element\nText:\n${containingKtFile.getText()}")
                return@buildSequence
            }

            yieldAll(context.processInfos(onFailure))
            return@buildSequence
        }
    }

    val virtualFile = containingFile.originalFile.virtualFile
    if (virtualFile == null) {
        onFailure("Analyzing element of type ${this::class.java} in non-physical file $containingFile of type ${containingFile::class.java}\nText:\n$text")
        return@buildSequence
    }

    yieldAll(processVirtualFile(project, virtualFile, (containingFile as? KtFile)?.isCompiled ?: false))
}

private fun processLightElement(ktLightElement: KtLightElement<*, *>, onFailure: (String) -> Unit): Sequence<IdeaModuleInfo?> = buildSequence {
    val decompiledClass = ktLightElement.getParentOfType<KtLightClassForDecompiledDeclaration>(strict = false)
    if (decompiledClass != null) {
        yieldAll(processVirtualFile(
                ktLightElement.project,
                ktLightElement.containingFile.virtualFile.sure { "Decompiled class should be build from physical file" },
                false))
    }

    val element = ktLightElement.kotlinOrigin ?: when (ktLightElement) {
        is FakeLightClassForFileOfPackage -> ktLightElement.getContainingFile()!!
        is KtLightClassForFacade -> ktLightElement.files.first()
        else -> {
            onFailure("Light element without origin is referenced by resolve:\n${ktLightElement}\n${ktLightElement.clsDelegate.text}")
            return@buildSequence
        }
    }

    yieldAll(element.processInfos(onFailure))
}

private fun processVirtualFile(
        project: Project,
        virtualFile: VirtualFile,
        treatAsLibrarySource: Boolean): Sequence<IdeaModuleInfo?> = buildSequence {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null && !module.isDisposed) {
        val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex
        if (moduleFileIndex.isInTestSourceContent(virtualFile)) {
            yield(module.testSourceInfo())
        }
        else if (moduleFileIndex.isInSourceContentWithoutInjected(virtualFile)) {
            yield(module.productionSourceInfo())
        }
    }

    projectFileIndex.getOrderEntriesForFile(virtualFile).forEach { orderEntry ->
        orderEntry.toIdeaModuleInfo(project, virtualFile, treatAsLibrarySource)?.let {
            yield(it)
        }
    }

    val scriptDefinition = getScriptDefinition(virtualFile, project)
    if (scriptDefinition != null) {
        yield(ScriptModuleInfo(project, virtualFile, scriptDefinition))
    }

    val isBinary = virtualFile.isKotlinBinary()
    val scriptConfigurationManager = ScriptDependenciesManager.getInstance(project)
    if (isBinary && virtualFile in scriptConfigurationManager.getAllScriptsClasspathScope()) {
        if (treatAsLibrarySource) {
            yield(ScriptDependenciesSourceModuleInfo(project))
        }
        else {
            yield(ScriptDependenciesModuleInfo(project, null))
        }
    }

    if (!isBinary && virtualFile in scriptConfigurationManager.getAllLibrarySourcesScope()) {
        yield(ScriptDependenciesSourceModuleInfo(project))
    }
}

private inline fun <reified T : IdeaModuleInfo> collectModuleInfosByType(project: Project, virtualFile: VirtualFile): Collection<T> {
    val orderEntries = ProjectFileIndex.SERVICE.getInstance(project).getOrderEntriesForFile(virtualFile)

    val result = linkedSetOf<T?>()
    orderEntries.forEach {
        (it.toIdeaModuleInfo(project, virtualFile, treatAsLibrarySource = false) as? T)?.let { result.add(it) }
    }

    // NOTE: non idea model infos can be obtained this way, like script related infos
    // only one though, luckily it covers existing cases
    result.add(getModuleInfoByVirtualFile(project, virtualFile) as? T)

    return result.filterNotNull()
}

private fun OrderEntry.toIdeaModuleInfo(
        project: Project,
        virtualFile: VirtualFile,
        treatAsLibrarySource: Boolean = false): IdeaModuleInfo? {
    if (!this.isValid) return null

    when (this) {
        is LibraryOrderEntry -> {
            val library = this.library ?: return null
            if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile) && !treatAsLibrarySource) {
                return LibraryInfo(project, library)
            }
            else if (ProjectRootsUtil.isLibraryFile(project, virtualFile) || treatAsLibrarySource) {
                LibrarySourceInfo(project, library)
            }
        }
        is JdkOrderEntry -> {
            val sdk = this.jdk ?: return null
            SdkInfo(project, sdk)
        }
    }

    return null
}