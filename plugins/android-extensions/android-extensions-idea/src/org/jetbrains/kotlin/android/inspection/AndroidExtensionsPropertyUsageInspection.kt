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

package org.jetbrains.kotlin.android.inspection

import com.android.resources.ResourceType
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.getReferredResourceOrManifestField
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class AndroidExtensionsPropertyUsageInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            private val androidFacet = AndroidFacet.getInstance(session.file)
            private val relatedLayouts: ConcurrentMap<KtClass, Set<XmlFile>> = ConcurrentHashMap()

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                androidFacet ?: return
                val context = expression.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return
                propertyDescriptor as? AndroidSyntheticProperty ?: return
                val layoutFile = (propertyDescriptor.source as? PsiSourceElement)?.psi?.containingFile as? XmlFile ?: return
                val receiverClass = resolvedCall.getReceiverClass() ?: return

                val relatedLayouts = receiverClass.getRelatedLayoutsFromCache(androidFacet)

                // Don't report anything if no related layouts found, probably alternative way of layout inflating is used
                if (relatedLayouts.isEmpty()) {
                    return
                }

                if (layoutFile !in relatedLayouts) {
                    holder.registerProblem(
                            expression,
                            "Usage of Android Extensions property from unrelated layout ${layoutFile.name}",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }

            private fun KtClass.getRelatedLayoutsFromCache(androidFacet: AndroidFacet): Set<XmlFile> = synchronized(this) {
                relatedLayouts.computeIfAbsent(this) { it.getRelatedLayouts(androidFacet) }
            }
        }
    }

    private fun ResolvedCall<*>.getReceiverClass(): KtClass? =
            extensionReceiver?.type?.constructor?.declarationDescriptor?.source?.getPsi() as? KtClass


    private fun KtClass.getRelatedLayouts(androidFacet: AndroidFacet): Set<XmlFile> {
        val layouts = mutableSetOf<XmlFile>()
        accept(object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)
                element.acceptChildren(this)
            }

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                super.visitReferenceExpression(expression)

                val resClassName = ResourceType.LAYOUT.getName()
                val info = (expression as? KtSimpleNameExpression)?.let {
                    getReferredResourceOrManifestField(androidFacet, it, resClassName, true)
                }

                if (info == null || info.isFromManifest) {
                    return
                }

                val files = androidFacet
                        .localResourceManager
                        .findResourceFiles(resClassName, info.fieldName, false)
                        .filterIsInstance<XmlFile>()

                layouts.addAll(files)
                layouts.addAll(files.getIncludedLayouts(androidFacet))
            }
        })

        return layouts
    }

    private fun List<XmlFile>.getIncludedLayouts(androidFacet: AndroidFacet, processedLayouts: Set<XmlFile> = setOf()): List<XmlFile> {
        if (isEmpty()) {
            return emptyList()
        }

        val includedLayouts = filter { it !in processedLayouts }.flatMap { it.getIncludedLayouts(androidFacet) }
        return includedLayouts + includedLayouts.getIncludedLayouts(androidFacet, processedLayouts + this)
    }

    private fun XmlFile.getIncludedLayouts(androidFacet: AndroidFacet): Set<XmlFile> {
        val result = mutableSetOf<XmlFile>()
        rootTag?.acceptChildren(object : XmlElementVisitor() {
            override fun visitXmlElement(element: XmlElement) {
                super.visitXmlElement(element)
                element.acceptChildren(this)
            }

            override fun visitXmlTag(tag: XmlTag) {
                super.visitXmlTag(tag)
                if (tag.name != "include") return
                val layout = tag.getAttribute("layout")?.value ?: return
                val (resClassName, fieldName) =
                        layout.removePrefix("@").split('/').takeIf { it.size == 2 } ?: return

                val files = androidFacet
                        .localResourceManager
                        .findResourcesByFieldName(resClassName, fieldName)
                        .filterIsInstance<XmlFile>()

                result.addAll(files)
            }
        })

        return result
    }
}
