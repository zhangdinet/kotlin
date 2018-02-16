package org.jetbrains.uast.test.kotlin

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.evaluation.UEvaluatorExtension
import org.jetbrains.uast.kotlin.evaluation.KotlinEvaluatorExtension
import org.jetbrains.uast.test.env.AbstractUastTest
import java.io.File

abstract class AbstractKotlinUastTest : AbstractUastTest() {
    protected companion object {
        val TEST_KOTLIN_MODEL_DIR_PATH = "plugins/uast-kotlin/testData"
        val TEST_KOTLIN_MODEL_DIR = File(TEST_KOTLIN_MODEL_DIR_PATH)
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    override fun getTestDataPath() = TEST_KOTLIN_MODEL_DIR_PATH

    override fun setUp() {
        super.setUp()
        val area = Extensions.getRootArea()
        area.registerExtensionPoint(UEvaluatorExtension.EXTENSION_POINT_NAME.name, UEvaluatorExtension::class.java.name)
        area.getExtensionPoint(UEvaluatorExtension.EXTENSION_POINT_NAME)
            .registerExtension(KotlinEvaluatorExtension())
    }

    override fun getVirtualFile(testName: String): VirtualFile {
        val testFile = File(testDataPath).listFiles { pathname -> pathname.nameWithoutExtension == testName }.first()
        return myFixture.configureByFile(PathUtil.toSystemIndependentName(testFile.path)).virtualFile
    }

    override fun tearDown() {
        Extensions.getRootArea().unregisterExtensionPoint(UEvaluatorExtension.EXTENSION_POINT_NAME.name)
        super.tearDown()
    }
}