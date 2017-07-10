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

package org.jetbrains.kotlin.android

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractAndroidExtensionsInspectionTest : KotlinAndroidTestCase() {

    override fun setUp() {
        super.setUp()
        (myFixture as CodeInsightTestFixtureImpl).setVirtualFileFilter { false }
        ConfigLibraryUtil.configureKotlinRuntime(myModule)
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntime(myModule)
        super.tearDown()
    }

    fun doTest(path: String) {
        val fileText = File(path).readText()
        val mainInspectionClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INSPECTION_CLASS: ") ?: error("Empty class name")

        val inspectionClassNames = mutableListOf(mainInspectionClassName)
        for (i in 2..100) {
            val className = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INSPECTION_CLASS$i: ") ?: break
            inspectionClassNames += className
        }

        myFixture.enableInspections(*inspectionClassNames.map { className ->
            val inspectionClass = Class.forName(className)
            inspectionClass.newInstance() as InspectionProfileEntry
        }.toTypedArray())

        copyResourceDirectoryForTest(path)
        myFixture.copyFileToProject("${File(path).parent}/R.java", "gen/com/myapp/R.java")

        val virtualFile = myFixture.copyFileToProject(path, "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)
        myFixture.checkHighlighting(true, false, false)
    }
}