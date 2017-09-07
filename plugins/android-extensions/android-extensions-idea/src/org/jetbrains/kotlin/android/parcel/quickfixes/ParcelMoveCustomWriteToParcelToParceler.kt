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

package org.jetbrains.kotlin.android.parcel.quickfixes

import kotlinx.android.parcel.Parceler
import org.jetbrains.kotlin.android.parcel.ANDROID_PARCELABLE_CLASS_FQNAME
import org.jetbrains.kotlin.android.parcel.ANDROID_PARCEL_CLASS_FQNAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ParcelMoveCustomWriteToParcelToParceler(function: KtFunction) : AbstractParcelableQuickFix<KtFunction>(function) {
    companion object {
        private val PARCELER_FQNAME = FqName(Parceler::class.java.name)
        private val PARCELER_WRITE_FUNCTION_NAME = Name.identifier("write")
        private val PARCELER_CREATE_FUNCTION_NAME = Name.identifier("create")

        private fun KtClass.findParcelerCompanionObject(): Pair<KtObjectDeclaration, ClassDescriptor>? {
            for (obj in companionObjects) {
                val bindingContext = obj.analyze(BodyResolveMode.PARTIAL)
                val objDescriptor = bindingContext[BindingContext.CLASS, obj] ?: continue
                for (superClassifier in objDescriptor.getAllSuperClassifiers()) {
                    val superClass = superClassifier as? ClassDescriptor ?: continue
                    if (superClass.fqNameSafe == PARCELER_FQNAME) return Pair(obj, objDescriptor)
                }
            }

            return null
        }

        private fun FunctionDescriptor.isParcelerWriteDeclaration(): Boolean {
            if (kind != CallableMemberDescriptor.Kind.DECLARATION) return false
            if (name != PARCELER_WRITE_FUNCTION_NAME) return false
            if (valueParameters.size != 2) return false
            if (valueParameters[0].type.constructor.declarationDescriptor?.fqNameSafe != ANDROID_PARCEL_CLASS_FQNAME) return false
            if (!KotlinBuiltIns.isInt(valueParameters[1].type)) return false
            return true
        }

        private fun KtNamedFunction.doesLookLikeCreateDeclaration(): Boolean {
            fun KtTypeReference.getFqName(): String? = analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, this]
                    ?.constructor?.declarationDescriptor?.fqNameSafe?.asString()

            return name == PARCELER_CREATE_FUNCTION_NAME.asString()
                   && hasModifier(KtTokens.OVERRIDE_KEYWORD)
                   && receiverTypeReference == null
                   && valueParameters.size == 1
                   && valueParameters[0].let { it.typeReference?.getFqName() == ANDROID_PARCEL_CLASS_FQNAME.asString() }
                   && typeParameters.size == 0
        }
    }

    object Factory : AbstractFactory(f@ {
        val function = findElement<KtFunction>() ?: return@f null
        fun makeFactory() = ParcelMoveCustomWriteToParcelToParceler(function)

        val (_, parcelerObjDescriptor) =
                function.containingClass()?.findParcelerCompanionObject() ?: return@f makeFactory()

        parcelerObjDescriptor.unsubstitutedMemberScope
                .getContributedFunctions(PARCELER_WRITE_FUNCTION_NAME, NoLookupLocation.FROM_IDE)
                .firstOrNull { it.isParcelerWriteDeclaration() } ?: return@f makeFactory()

        // We already have a custom implementation, and it can be hard to figure out if we can merge it
        return@f null
    })

    override fun getText() = "Move ''writeToParcel()'' to the ''Parceler'' companion object"

    override fun invoke(ktPsiFactory: KtPsiFactory, element: KtFunction) {
        val containingClass = element.containingClass() ?: return
        val parcelerObject = containingClass.findParcelerCompanionObject()?.first
                             ?: containingClass.getOrCreateCompanionObject()

        val bindingContext = parcelerObject.analyze(BodyResolveMode.PARTIAL)
        val parcelerObjectDescriptor = bindingContext[BindingContext.CLASS, parcelerObject] ?: return

        val parcelerTypeArg = containingClass.name ?: ANDROID_PARCELABLE_CLASS_FQNAME.asString()

        if (!parcelerObjectDescriptor.getAllSuperClassifiers().any { it.fqNameSafe == PARCELER_FQNAME }) {
            val entryText = PARCELER_FQNAME.asString() + "<" + parcelerTypeArg + ">"
            parcelerObject.addSuperTypeListEntry(ktPsiFactory.createSuperTypeEntry(entryText)).shortenReferences()
        }

        run addWriteMethod@ {
            val newFunction = element.copy() as KtFunction
            element.delete()

            newFunction.setName(PARCELER_WRITE_FUNCTION_NAME.asString())
            newFunction.setModifierList(ktPsiFactory.createModifierList(KtTokens.OVERRIDE_KEYWORD))
            newFunction.setReceiverTypeReference(ktPsiFactory.createType(parcelerTypeArg))
            newFunction.valueParameterList?.apply {
                val parcelParameterName = parameters.getOrNull(0)?.name ?: "parcel"
                val flagsParameterName = parameters.getOrNull(1)?.name ?: "flags"

                repeat(parameters.size) { removeParameter(0) }

                fun addParameterThenShortenReferences(text: String) {
                    addParameter(ktPsiFactory.createParameter(text))
                }

                addParameterThenShortenReferences("$parcelParameterName : ${ANDROID_PARCEL_CLASS_FQNAME.asString()}")
                addParameterThenShortenReferences("$flagsParameterName : Int")
            }

            parcelerObject.addDeclaration(newFunction).valueParameterList?.shortenReferences()
        }

        run addCreateMethod@ {
            val hasCreateMethodDeclared = parcelerObject.declarations
                    .filterIsInstance<KtNamedFunction>().any { it.doesLookLikeCreateDeclaration() }

            if (!hasCreateMethodDeclared) {
                val createFunction = ktPsiFactory.createFunction(
                        "override fun create(parcel: ${ANDROID_PARCEL_CLASS_FQNAME.asString()}): $parcelerTypeArg = TODO()")
                parcelerObject.addDeclaration(createFunction)
            }
        }

        parcelerObject.getSuperTypeList()?.shortenReferences()
    }
}