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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object ResolutionResultsStorage {
    private var rootDirectory: Directory = Directory(Paths.get(""))

    fun saveResolutionResult(resolvedCall: ResolvedCall<*>) {
        val results = ResolutionResults.create(resolvedCall) ?: return
        val absolutePath = results.path

        var currentDirectory = rootDirectory
        for (i in 0 until absolutePath.nameCount - 1) {
            currentDirectory = currentDirectory.getSubdirectory(absolutePath.getName(i))
        }

        currentDirectory.addResult(absolutePath.fileName, results)
    }

    fun dumpResults(dumpDestination: String, includeTypeInference: Boolean) {
        if (rootDirectory.directories.isEmpty() && rootDirectory.filesToResults.isEmpty()) {
            return
        }

        val destination = Paths.get(dumpDestination)
        dumpSubTree(rootDirectory, destination, includeTypeInference)

        rootDirectory = Directory(Paths.get(""))
    }

    private fun dumpSubTree(directory: Directory, base: Path, includeTypeInference: Boolean) {
        val thisDirAbsolutePath = base.resolve(directory.path)
        Files.createDirectories(thisDirAbsolutePath)

        // dump subdirs
        directory.directories.forEach { dumpSubTree(it.value, thisDirAbsolutePath, includeTypeInference) }

        // dump files
        directory.filesToResults.forEach { (fileName, results) ->
            val absolutePathToFile = thisDirAbsolutePath.resolve(fileName)
            val sortedResults = results.toList().sortedBy { it.callElement.startOffset }

            Files.deleteIfExists(absolutePathToFile)

            Files.createFile(absolutePathToFile).toFile().printWriter().use { writer ->
                sortedResults.forEach { it.dumpToFile(writer, includeTypeInference) }
            }
        }
    }

    private class ResolutionResults(
            val path: Path,
            val lineNumber: Int,
            val offsetInLine: Int,
            val callElement: KtElement,
            val resolvedCall: ResolvedCall<*>,
            val resolvedTo: DeclarationDescriptor
    ) {
        override fun hashCode(): Int = callElement.startOffset

        override fun equals(other: Any?): Boolean {
            if (other !is ResolutionResults) return false
            return callElement.startOffset == other.callElement.startOffset
        }

        companion object {
            fun create(resolvedCall: ResolvedCall<*>): ResolutionResults? {
                val callElement = resolvedCall.call.callElement

                val file = callElement.containingKtFile
                val virtualFile = file.virtualFile ?: return null
                val path = Paths.get(virtualFile.path)

                val document = file.viewProvider.document

                val lineNumber = document!!.getLineNumber(callElement.startOffset)
                val offsetInLine = callElement.startOffset - document.getLineStartOffset(lineNumber)

                return ResolutionResults(path, lineNumber, offsetInLine, resolvedCall.call.callElement, resolvedCall, resolvedCall.resultingDescriptor)
            }
        }

        fun dumpToFile(writer: PrintWriter, includeTypeInference: Boolean) {
            writer.appendln(
                    "'${resolvedCall.call.calleeExpression?.text ?: resolvedCall.call.callElement.text}' @ [${lineNumber + 1}:${offsetInLine + 1}] ==> ${resolvedTo.render()}"
            )
            if (includeTypeInference && resolvedCall.typeArguments.isNotEmpty()) {
                writer.appendln("Inferred types:")

                resolvedCall.typeArguments.forEach {
                    writer.appendln("    ${DEBUG_WITHOUT_DEFINED_IN_RENDERER.render(it.key)} -> ${it.value}")
                }
            }
            writer.appendln()
        }

        private fun DeclarationDescriptor.render(): String {
            return try {
                DEBUG_WITH_SHORT_NAMES_RENDERER.render(this) +
                "[" + this.javaClass.simpleName + "]"
            } catch (e: Throwable) {
                // DescriptionRenderer may throw if this is not yet completely initialized
                // It is very inconvenient while debugging
                this.javaClass.simpleName + " " + this.name
            }
        }

        // Like debug, but without fqns
        private val DEBUG_WITH_SHORT_NAMES_RENDERER = DescriptorRenderer.withOptions {
            debugMode = true
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            modifiers = DescriptorRendererModifier.ALL
        }

        private val DEBUG_WITHOUT_DEFINED_IN_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            debugMode = true
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            modifiers = DescriptorRendererModifier.ALL
        }
    }

    private data class Directory(val path: Path) {
        val filesToResults: HashMap<Path, MutableSet<ResolutionResults>> = hashMapOf()
        val directories: HashMap<Path, Directory> = hashMapOf()

        fun getSubdirectory(path: Path): Directory = directories[path] ?: Directory(path).also { directories[path] = it }

        fun addResult(path: Path, resolutionResults: ResolutionResults) {
            val oldSet = filesToResults[path]
            if (oldSet != null) {
                oldSet.add(resolutionResults)
            } else {
                filesToResults[path] = hashSetOf(resolutionResults)
            }
        }
    }
}
