/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.ClassVisitor
import kotlinx.metadata.Flags
import kotlinx.metadata.FunctionVisitor
import kotlinx.metadata.jvm.KotlinClassFile
import kotlinx.metadata.jvm.KotlinMetadata
import kotlinx.metadata.jvm.jvmSignature
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
