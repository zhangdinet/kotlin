/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*

class InsertBeforeMethodVisitor(
    private val methodNode: MethodNode,
    private val insertBeforeNode: AbstractInsnNode
) : MethodVisitor(Opcodes.ASM6, methodNode) {

    private fun add(node: AbstractInsnNode) {
        methodNode.instructions.insertBefore(insertBeforeNode, node)
    }

    override fun visitFrame(type: Int, nLocal: Int, local: Array<Any>?, nStack: Int, stack: Array<Any>?) {
        add(
            FrameNode(
                type, nLocal,
                if (local == null) null else getLabelNodes(local),
                nStack,
                if (stack == null) null else getLabelNodes(stack)
            )
        )
    }

    override fun visitInsn(opcode: Int) {
        add(InsnNode(opcode))
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        add(IntInsnNode(opcode, operand))
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        add(VarInsnNode(opcode, `var`))
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        add(TypeInsnNode(opcode, type))
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        add(FieldInsnNode(opcode, owner, name, desc))
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
        add(MethodInsnNode(opcode, owner, name, desc, itf))
    }

    override fun visitInvokeDynamicInsn(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any) {
        add(InvokeDynamicInsnNode(name, desc, bsm, *bsmArgs))
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        add(JumpInsnNode(opcode, getLabelNode(label)))
    }

    override fun visitLabel(label: Label) {
        add(getLabelNode(label))
    }

    override fun visitLdcInsn(cst: Any) {
        add(LdcInsnNode(cst))
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        add(IincInsnNode(`var`, increment))
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
        add(TableSwitchInsnNode(min, max, getLabelNode(dflt), *getLabelNodes(labels)))
    }

    override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<Label>) {
        add(LookupSwitchInsnNode(getLabelNode(dflt), keys, getLabelNodes(labels)))
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        add(MultiANewArrayInsnNode(desc, dims))
    }

    override fun visitInsnAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor {
        var insn = insertBeforeNode.previous
                ?: throw IllegalStateException("No instructions before insertion location")
        while (insn.opcode == -1) {
            insn = insn.previous
        }

        return TypeAnnotationNode(typeRef, typePath, desc).also {
            addInsnAnnotation(visible, insn, it)
        }
    }

    private fun addInsnAnnotation(visible: Boolean, insn: AbstractInsnNode, an: TypeAnnotationNode) {
        if (visible) {
            if (insn.visibleTypeAnnotations == null) {
                insn.visibleTypeAnnotations = ArrayList<TypeAnnotationNode>(1)
            }
            insn.visibleTypeAnnotations.add(an)
        } else {
            if (insn.invisibleTypeAnnotations == null) {
                insn.invisibleTypeAnnotations = ArrayList<TypeAnnotationNode>(1)
            }
            insn.invisibleTypeAnnotations.add(an)
        }
    }

    private fun getLabelNode(label: Label): LabelNode {
        if (label.info !is LabelNode) {
            label.info = LabelNode(label)
        }
        return label.info as LabelNode
    }

    private fun getLabelNodes(labels: Array<out Label>) =
        Array(labels.size) { getLabelNode(labels[it]) }

    private fun getLabelNodes(labels: Array<out Any>) =
        Array(labels.size) {
            val x = labels[it]
            if (x is Label) getLabelNode(x) else x
        }
}


inline fun MethodNode.insertBeforeWithInstructionAdapter(insertBeforeNode: AbstractInsnNode, builder: InstructionAdapter.() -> Unit) {
    InstructionAdapter(InsertBeforeMethodVisitor(this, insertBeforeNode)).builder()
}