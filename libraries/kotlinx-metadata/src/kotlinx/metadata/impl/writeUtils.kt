/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.Annotation
import kotlinx.metadata.AnnotationArgument
import kotlinx.metadata.ClassName
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.serialization.StringTable

fun Annotation.writeAnnotation(strings: StringTable): ProtoBuf.Annotation.Builder =
    ProtoBuf.Annotation.newBuilder().apply {
        id = strings.getClassNameIndex(className)
        for ((name, argument) in arguments) {
            addArgument(ProtoBuf.Annotation.Argument.newBuilder().apply {
                nameId = strings.getStringIndex(name)
                value = argument.writeAnnotationArgument(strings).build()
            })
        }
    }

private fun AnnotationArgument<*>.writeAnnotationArgument(strings: StringTable): ProtoBuf.Annotation.Argument.Value.Builder =
    ProtoBuf.Annotation.Argument.Value.newBuilder().apply {
        when (this@writeAnnotationArgument) {
            is AnnotationArgument.ByteValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
                this.intValue = value.toLong()
            }
            is AnnotationArgument.CharValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.CHAR
                this.intValue = value.toLong()
            }
            is AnnotationArgument.ShortValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
                this.intValue = value.toLong()
            }
            is AnnotationArgument.IntValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT
                this.intValue = value.toLong()
            }
            is AnnotationArgument.LongValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.LONG
                this.intValue = value
            }
            is AnnotationArgument.FloatValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.FLOAT
                this.floatValue = value
            }
            is AnnotationArgument.DoubleValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.DOUBLE
                this.doubleValue = value
            }
            is AnnotationArgument.BooleanValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN
                this.intValue = if (value) 1 else 0
            }
            is AnnotationArgument.StringValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.STRING
                this.stringValue = strings.getStringIndex(value)
            }
            is AnnotationArgument.KClassValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.CLASS
                this.classId = strings.getClassNameIndex(value)
            }
            is AnnotationArgument.EnumValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.ENUM
                this.classId = strings.getClassNameIndex(enumClassName)
                this.enumValueId = strings.getStringIndex(enumEntryName)
            }
            is AnnotationArgument.AnnotationValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION
                this.annotation = value.writeAnnotation(strings).build()
            }
            is AnnotationArgument.ArrayValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.ARRAY
                for (element in value) {
                    this.addArrayElement(element.writeAnnotationArgument(strings))
                }
            }
        }
    }

internal fun StringTable.getClassNameIndex(name: ClassName): Int =
    getQualifiedClassNameIndex(name.name, name.isLocal)
