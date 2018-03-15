/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.rangeLoops

import org.jetbrains.kotlin.codegen.optimization.boxing.isMethodInsnWith
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.insertBeforeWithInstructionAdapter
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

class RangeLoopOptimizationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        Transformer(internalClassName, methodNode).run()
    }

    private class Transformer(private val internalClassName: String, private val methodNode: MethodNode) {
        fun run() {
            // If there are nextInt or nextLong method calls on specialized iterators,
            // most likely this method contains optimizable loops over Int/Long ranges.
            // Otherwise we can just skip all bytecode analysis and rewriting.
            if (!hasNextIntOrNextLongMethodCalls()) return

            val frames = analyze(
                internalClassName, methodNode,
                object : OptimizationBasicInterpreter() {
                    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? =
                        if (insn.opcode == Opcodes.CHECKCAST)
                            value
                        else
                            super.unaryOperation(insn, value)
                }
            )
            val insns = methodNode.instructions.toArray()
            val transforms = ArrayList<() -> Unit>()
            for (i in insns.indices) {
                // We are looking for the following pattern of code (range loop header):
                //
                // #1       INVOKEINTERFACE java/lang/Iterable.iterator ()Ljava/util/Iterator;
                // #2       ASTORE v
                // #3   LNext
                // #4       ALOAD v
                // #5       INVOKEINTERFACE java/util/Iterator.hasNext ()Z
                // #6       IFEQ LEnd
                // #7       ALOAD v
                // #8       CHECKCAST kotlin/collections/{IntIterator, LongIterator}
                // #9       INVOKEVIRTUAL kotlin/collections/{IntIterator.nextInt ()I, LongIterator.nextLong ()J}
                // #10      {ISTORE w, LSTORE w}
                //
                // At #1, stack top SHOULD be a range value (of type 'Lkotlin/ranges/IntRange;' or 'Lkotlin/ranges/LongRange;').
                // At #2, variable store 'ASTORE v' SHOULD NOT be visible in debugger.

                // #1       INVOKEINTERFACE java/lang/Iterable.iterator ()Ljava/util/Iterator;
                val iteratorInsn = insns[i]
                if (!iteratorInsn.isIterator()) continue
                val rangeType = frames[i]?.top()?.type ?: continue
                val elementType = rangeType.getRangeElementType() ?: continue

                // #2       ASTORE v
                val aStoreInsn = iteratorInsn.next as? VarInsnNode ?: continue
                if (aStoreInsn.opcode != Opcodes.ASTORE) continue
                if (aStoreInsn.isVisibleInDebugger()) continue

                // #3   L
                val lNextInsn = aStoreInsn.next as? LabelNode ?: continue
                val lNext = lNextInsn.label

                // #4       ALOAD v
                val aLoad1Insn = lNextInsn.next as? VarInsnNode ?: continue
                if (aLoad1Insn.`var` != aStoreInsn.`var`) continue

                // #5       INVOKEINTERFACE java/util/Iterator.hasNext ()Z
                val hasNextInsn = aLoad1Insn.next ?: continue
                if (!hasNextInsn.isHasNext()) continue

                // #6       IFEQ L4
                val ifEqInsn = hasNextInsn.next as? JumpInsnNode ?: continue
                if (ifEqInsn.opcode != Opcodes.IFEQ) continue
                val lEnd = ifEqInsn.label.label

                // #7       ALOAD v
                val aLoad2Insn = ifEqInsn.next as? VarInsnNode ?: continue
                if (aLoad2Insn.`var` != aStoreInsn.`var`) continue

                // #8       CHECKCAST kotlin/collections/{IntIterator, LongIterator}
                val checkCastInsn = aLoad2Insn.next as? TypeInsnNode ?: continue
                if (checkCastInsn.opcode != Opcodes.CHECKCAST) continue
                if (checkCastInsn.desc != "kotlin/collections/IntIterator" && checkCastInsn.desc != "kotlin/collections/LongIterator")
                    continue

                // #9       INVOKEVIRTUAL kotlin/collections/{IntIterator.nextInt ()I, LongIterator.nextLong ()J}
                val nextValueInsn = checkCastInsn.next as? MethodInsnNode ?: continue
                if (!nextValueInsn.isNextInt() && !nextValueInsn.isNextLong()) continue

                // #10      {ISTORE w, LSTORE w}
                val storeLoopVarInsn = nextValueInsn.next as? VarInsnNode ?: continue
                if (storeLoopVarInsn.opcode != Opcodes.ISTORE && storeLoopVarInsn.opcode != Opcodes.LSTORE) continue
                val vLoop = storeLoopVarInsn.`var`


                // Allocate locals for counter and upper bound. If we run out of locals, bail out.
                val vNext = allocateLocal(elementType) ?: break
                val vLast = allocateLocal(elementType) ?: break

                // If we are here, we have found a piece of code that is a range-based loop,
                // and can transform it to a counter-based loop without virtual method calls on each iteration.
                //
                //      CHECKCAST <Range>
                //      DUP
                //      INVOKEVIRTUAL <Range>.getFirst()
                //      tSTORE vNext
                //      INVOKEVIRTUAL <Range>.getLast()
                //      tSTORE vLast
                //      tLOAD vNext
                //      tLOAD vLast
                //      ifcmpgt<t> LEnd
                //      tINC vLast
                //  LNext
                //      tLOAD vNext
                //      tLOAD vLast
                //      ifcmpeq<t> LEnd
                //      tLOAD vNext
                //      tSTORE vLoop
                //      tINC vNext

                transforms.add {
                    methodNode.insertBeforeWithInstructionAdapter(lNextInsn) {
                        checkcast(rangeType)
                        dup()
                        getRangeElement("getFirst", elementType)
                        store(vNext, elementType)
                        getRangeElement("getLast", elementType)
                        store(vLast, elementType)
                        load(vNext, elementType)
                        load(vLast, elementType)
                        ifcmpgt(lEnd, elementType)
                        inc(vLast, elementType)
                    }

                    methodNode.insertBeforeWithInstructionAdapter(lNextInsn.next) {
                        load(vNext, elementType)
                        load(vLast, elementType)
                        ifcmpeq(lEnd, elementType)
                        load(vNext, elementType)
                        store(vLoop, elementType)
                        inc(vNext, elementType)
                    }

                    methodNode.instructions.removeRange(iteratorInsn, aStoreInsn)
                    methodNode.instructions.removeRange(aLoad1Insn, storeLoopVarInsn)
                }
            }

            // Perform transformations
            transforms.forEach { it() }
        }

        private fun InsnList.removeRange(startInclusive: AbstractInsnNode, endInclusive: AbstractInsnNode) {
            var node = startInclusive
            while (true) {
                val next = node.next ?: throw AssertionError("Range is not continuous")
                remove(node)
                if (node == endInclusive) return
                node = next
            }
        }

        private fun allocateLocal(type: Type): Int? {
            val slot = methodNode.maxLocals
            methodNode.maxLocals += type.size
            return if (methodNode.maxLocals <= Short.MAX_VALUE) slot else null
        }

        private fun InstructionAdapter.getRangeElement(methodName: String, elementType: Type) {
            when (elementType) {
                Type.INT_TYPE -> invokevirtual("kotlin/ranges/IntRange", methodName, "()I", false)
                Type.LONG_TYPE -> invokevirtual("kotlin/ranges/LongRange", methodName, "()J", false)
                else -> throw AssertionError("I or J expected: $elementType")
            }
        }

        private fun InstructionAdapter.ifcmpgt(label: Label, elementType: Type) {
            when (elementType) {
                Type.INT_TYPE -> ificmpgt(label)
                Type.LONG_TYPE -> {
                    lcmp()
                    ifgt(label)
                }
                else -> throw AssertionError("I or J expected: $elementType")
            }
        }

        private fun InstructionAdapter.ifcmpeq(label: Label, elementType: Type) {
            when (elementType) {
                Type.INT_TYPE -> ificmpeq(label)
                Type.LONG_TYPE -> {
                    lcmp()
                    ifeq(label)
                }
                else -> throw AssertionError("I or J expected: $elementType")
            }
        }

        private fun InstructionAdapter.inc(v: Int, elementType: Type) {
            when (elementType) {
                Type.INT_TYPE -> iinc(v, 1)
                Type.LONG_TYPE -> {
                    load(v, elementType)
                    lconst(1)
                    add(elementType)
                    store(v, elementType)
                }
                else -> throw AssertionError("I or J expected: $elementType")
            }
        }

        private fun VarInsnNode.isVisibleInDebugger(): Boolean {
            val aStoreIndex = indexOf()
            return methodNode.localVariables.any {
                it.index == `var` &&
                        it.start.indexOf() <= aStoreIndex + 1 &&
                        it.end.indexOf() >= aStoreIndex
            }
        }

        private fun AbstractInsnNode.indexOf() = methodNode.instructions.indexOf(this)

        private fun hasNextIntOrNextLongMethodCalls() =
            methodNode.instructions.toArray().any {
                it.isMethodInsnWith(Opcodes.INVOKEVIRTUAL) {
                    isNextInt() || isNextLong()
                }
            }

        private fun AbstractInsnNode.isIterator() =
            isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
                owner == "java/lang/Iterable" && name == "iterator" && desc == "()Ljava/util/Iterator;"
            }

        private fun AbstractInsnNode.isHasNext() =
            isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
                owner == "java/util/Iterator" && name == "hasNext" && desc == "()Z"
            }

        private fun MethodInsnNode.isNextLong() =
            owner == "kotlin/collections/LongIterator" && name == "nextLong" && desc == "()J"

        private fun MethodInsnNode.isNextInt() =
            owner == "kotlin/collections/IntIterator" && name == "nextInt" && desc == "()I"

        private fun Type.getRangeElementType() =
            when (descriptor) {
                "Lkotlin/ranges/IntRange;" -> Type.INT_TYPE
                "Lkotlin/ranges/LongRange;" -> Type.LONG_TYPE
                else -> null
            }
    }
}