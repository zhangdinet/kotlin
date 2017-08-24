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

package org.jetbrains.kotlin.codegen

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.errors.CompilationError
import com.android.tools.r8.errors.MainDexError
import com.android.tools.r8.graph.*
import com.android.tools.r8.ir.conversion.IRConverter
import com.android.tools.r8.utils.*
import com.android.tools.r8.utils.FileUtils.DEFAULT_DEX_FILENAME
import org.junit.Assert
import java.io.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.regex.Pattern

object D8Checker {

    val RUN_DX_CHECKER = true
    private val STACK_TRACE_PATTERN = Pattern.compile("[\\s]+at .*")

    @JvmStatic
    fun check(outputFiles: ClassFileFactory) {
        for (file in outputFiles.getClassFiles()) {
//            try {
                val bytes = file.asByteArray()
                checkFileWithDx(bytes, file.relativePath)
//            }
//            catch (e: Throwable) {
//                Assert.fail(generateExceptionMessage(e))
//            }

        }
    }

    private fun checkFileWithDx(bytes: ByteArray, relativePath: String) {
        try {
            val command = D8Command.builder().
                    setMode(com.android.tools.r8.CompilationMode.RELEASE).build()

            val programClasses = mutableListOf<DexProgramClass>()
            //TODO
            val options = InternalOptions(DexItemFactory())
            options.skipMinification = true
            options.inlineAccessors = false
            options.outline.enabled = false

            val timing = Timing("Timer")
            val builder = DexApplication.Builder(options.itemFactory, timing)
            try {
                val classReader = JarClassFileReader(
                        JarApplicationReader(options),
                        ClassKind.PROGRAM.bridgeConsumer(Consumer<DexProgramClass> { programClasses.add(it) }))
                classReader.read(DEFAULT_DEX_FILENAME, ClassKind.PROGRAM, ByteArrayInputStream(bytes))

                for (clazz in programClasses) {
                    builder.addProgramClass(clazz.asProgramClass())
                }
            }
            catch (e: IOException) {
                throw CompilationError("Failed to load class: " + relativePath, e)
            }


            var application = builder.build()
            val converter = IRConverter(timing, application, AppInfo(application), options, null/* CfgPrinter()*/)
            val executor = Executors.newSingleThreadExecutor()
            application = converter.convertToDex(executor)

            options.printWarnings()
        }
        catch (mainDexError: MainDexError) {
            throw CompilationError(mainDexError.getMessageForD8())
        }
        catch (e: ExecutionException) {
            if (e.cause is CompilationError) {
                throw e.cause as CompilationError
            }
            throw RuntimeException(e.message, e.cause)
        }

    }

    private fun generateExceptionMessage(e: Throwable): String {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            e.printStackTrace(printWriter)
            val stackTrace = writer.toString()
            val matcher = STACK_TRACE_PATTERN.matcher(stackTrace)
            return matcher.replaceAll("")
        }
    }
}
