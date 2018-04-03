/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.ModuleTestCase
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.doFirResolveTestBench
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.impl.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class FirTotalKotlinResolveInIdeTest : ModuleTestCase() {


    val projectRootFile = File(".")


    override fun setUpModule() {
        super.setUpModule()

        ModuleRootModificationUtil.addContentRoot(module, VfsUtil.findFileByIoFile(projectRootFile, true)!!)

        ModuleRootModificationUtil.updateModel(module) {
            projectRootFile.walkTopDown().onEnter {
                it.name.toLowerCase() !in setOf("testdata", "resources")
            }.filter {
                it.isDirectory && (it.name in setOf("src", "test", "tests"))
            }.forEach { dir ->
                val vdir = VfsUtil.findFileByIoFile(dir, true)!!
                it.contentEntries.single().addSourceFolder(vdir, false)
            }
        }

    }

    fun createSession() = object : FirSessionBase() {
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


    fun testTotalKotlin() {
        val session = createSession()

        val firFiles = mutableListOf<FirFile>()
        val builder = RawFirBuilder(session)
        val psiManager = PsiManager.getInstance(project)

        val files = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, GlobalSearchScope.moduleScope(module))


        println("Got vfiles: ${files.size}")
        files.forEach {
            val file = psiManager.findFile(it) as? KtFile ?: return@forEach
            try {
                val firFile = builder.buildFirFile(file)
                (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
                firFiles += firFile
            } catch (e: Exception) {
                System.err.println("Error building fir for $it")
                e.printStackTrace()
            }
        }

        println("Raw fir up, files: ${firFiles.size}")

        doFirResolveTestBench(firFiles, FirTotalResolveTransformer().transformers)
    }
}