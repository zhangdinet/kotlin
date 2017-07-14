/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.InputStream

class IDEVirtualFileFinder(private val scope: GlobalSearchScope) : VirtualFileFinder() {
    override fun findMetadata(classId: ClassId): InputStream? =
            findVirtualFileWithHeader(classId.asSingleFqName(), KotlinMetadataFileIndex.KEY)?.inputStream

    override fun hasMetadataPackage(fqName: FqName): Boolean = KotlinMetadataFilePackageIndex.hasSomethingInPackage(fqName, scope)

    override fun findBuiltInsData(packageFqName: FqName): InputStream? =
            findVirtualFileWithHeader(packageFqName, KotlinBuiltInsMetadataIndex.KEY)?.inputStream

    init {
        if (scope != GlobalSearchScope.EMPTY_SCOPE && scope.project == null) {
            LOG.warn("Scope with null project $scope")
        }
    }

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
            findVirtualFileWithHeader(classId.asSingleFqName(), KotlinClassFileIndex.KEY)

    private fun findVirtualFileWithHeader(fqName: FqName, key: ID<FqName, Void>): VirtualFile? {
        val files = FileBasedIndex.getInstance().getContainingFiles<FqName, Void>(key, fqName, scope)
        if (files.size > 1) {
            LOG.warn("There are ${files.size} files with same fqName: $fqName found.")
        }
        return files.firstOrNull()
    }

    companion object {
        private val LOG = Logger.getInstance(IDEVirtualFileFinder::class.java)
    }
}
