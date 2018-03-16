/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.ClassName
import kotlinx.metadata.ClassVisitor
import kotlinx.metadata.Flags
import kotlinx.metadata.FunctionVisitor
import kotlinx.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLClassLoader
import kotlin.reflect.full.primaryConstructor
import org.jetbrains.org.objectweb.asm.AnnotationVisitor as AsmAnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter as AsmClassWriter

class MetadataSmokeTest {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    private fun Class<*>.readMetadata(): KotlinMetadata {
        return getAnnotation(Metadata::class.java).run {
            KotlinMetadata(k, mv, bv, d1, d2, xs, pn, xi)
        }
    }

    @Test
    fun listInlineFunctions() {
        @Suppress("unused")
        class L {
            val x: Int inline get() = 42
            inline fun foo(f: () -> String) = f()
            fun bar() {}
        }

        val inlineFunctions = mutableListOf<String>()

        val klass = KotlinClassFile.read(L::class.java.readMetadata()) as KotlinClassFile.Class
        klass.accept(object : ClassVisitor() {
            override fun visitFunction(flags: Int, name: String, ext: FunctionVisitor.Extensions): FunctionVisitor? = null.also {
                val desc = ext.jvmSignature
                if (Flags.Function.isInline(flags) && desc != null) {
                    inlineFunctions += desc
                }
            }
        })

        assertEquals(
            listOf("foo(Lkotlin/jvm/functions/Function0;)Ljava/lang/String;"),
            inlineFunctions
        )
    }

    @Test
    fun produceKotlinClassFile() {
        // First, produce a KotlinMetadata instance with the kotlinx-metadata API, for the following class:
        //     class Hello {
        //         fun hello(): String = "Hello, world!"
        //     }

        val metadata = KotlinClassFile.Class.Writer().run {
            visit(Flags(Flags.isPublic), ClassName("Hello"))
            visitConstructor(
                Flags(Flags.isPublic, Flags.Constructor.isPrimary),
                JvmConstructorExtensions("<init>()V")
            )!!.visitEnd()
            visitFunction(
                Flags(Flags.isPublic, Flags.Function.isDeclaration),
                "hello",
                JvmFunctionExtensions("hello()Ljava/lang/String;")
            )!!.run {
                visitReturnType(0, JvmTypeExtensions(emptyList(), false))!!.run {
                    visitClass(ClassName("kotlin/String"))
                    visitEnd()
                }
                visitEnd()
            }
            visitEnd()

            write().metadata
        }

        // Then, produce the bytecode of a .class file with ASM

        val bytes = AsmClassWriter(0).run {
            visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER, "Hello", null, "java/lang/Object", null)

            // Use the created KotlinMetadata instance to write @kotlin.Metadata annotation on the class file
            visitAnnotation("Lkotlin/Metadata;", true).run {
                visit("mv", metadata.metadataVersion)
                visit("bv", metadata.bytecodeVersion)
                visit("k", metadata.kind)
                visitArray("d1").run {
                    metadata.data1.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d2").run {
                    metadata.data2.forEach { visit(null, it) }
                    visitEnd()
                }
                visitEnd()
            }

            visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "hello", "()Ljava/lang/String;", null, null).run {
                visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false).visitEnd()
                visitCode()
                visitLdcInsn("Hello, world!")
                visitInsn(Opcodes.ARETURN)
                visitMaxs(1, 1)
                visitEnd()
            }
            visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).run {
                visitCode()
                visitVarInsn(Opcodes.ALOAD, 0)
                visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                visitInsn(Opcodes.RETURN)
                visitMaxs(1, 1)
                visitEnd()
            }
            visitEnd()

            toByteArray()
        }

        // Finally, load the generated class, create its instance and invoke the method via Kotlin reflection.
        // Kotlin reflection loads the metadata and builds a mapping from Kotlin symbols to JVM, so if the call succeeds,
        // we can be sure that the metadata is consistent

        val classLoader = object : URLClassLoader(emptyArray()) {
            override fun findClass(name: String): Class<*> =
                if (name == "Hello") defineClass(name, bytes, 0, bytes.size) else super.findClass(name)
        }

        val kClass = classLoader.loadClass("Hello").kotlin
        val hello = kClass.primaryConstructor!!.call()
        val result = kClass.members.single { it.name == "hello" }.call(hello) as String

        assertEquals("Hello, world!", result)
    }
}
