package com.example.casdocplugin

import com.intellij.codeInspection.*
import com.intellij.patterns.PsiJavaPatterns.psiField
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil


class PrivateMemberVariablesDocumentation: AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object: JavaElementVisitor() {
        override fun visitClass(psiClass: PsiClass) {
            super.visitClass(psiClass)
            val publicMemberVariables = mutableListOf<PsiField>()

            publicMemberVariables.clear()

            // Collect public member variables in the class
            psiClass.fields.forEach { field ->
                if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
                    publicMemberVariables.add(field)
                }
            }

            // If there are public member variables, add a single warning for all of them
            if (publicMemberVariables.isNotEmpty()) {
                val firstField = publicMemberVariables.first()
                holder.registerProblem(
                        firstField,
                        "Public member variables seem very convenient, but they often cause more problems than they're worth. We want to be sure the Suit and Rank of a Card isn't going to change without our say-so",
                        ProblemHighlightType.WARNING,
                        MakeMemberVariablesPrivateQuickFix(psiClass, publicMemberVariables)
                )
            } else {
                val privateMemberVariables = mutableListOf<PsiField>()
                var hasGetterMethods = false

                psiClass.fields.forEach { field ->
                    if (field.hasModifierProperty(PsiModifier.PRIVATE)) {
                        privateMemberVariables.add(field)
                    }

                    if (hasGetterMethod(psiClass, field)) {
                        hasGetterMethods = true
                    }
                }

                val firstPrivateField = privateMemberVariables.first()

                holder.registerProblem(
                        firstPrivateField.modifierList!!.firstChild,
                        "Giving the private visibility to fields (i.e., they are only visible within the class that declares them) is generally a good choice. It strengthens encapsulation through information hiding, and prevents client code from making unwanted modifications directly to the state of an object that might leave it in a corrupted state.",
                        ProblemHighlightType.WARNING,
                )

                if (!hasGetterMethods) {
                    holder.registerProblem(
                            firstPrivateField.modifierList!!.firstChild,
                            "It's great that our class is better encapsulated, but it isn't useful at all if we can't see any of its data!",
                            ProblemHighlightType.WARNING,
                            AddPublicGetterMethodsQuickFix(psiClass)
                    )
                }
            }
        }

        private fun hasGetterMethod(psiClass: PsiClass, field: PsiField): Boolean {
            val getterMethodName = "get" + field.name.capitalize()
            val methods = psiClass.findMethodsByName(getterMethodName, true)
            for (method in methods) {
                val body = method.body
                if (body != null && body.statements.size == 1) {
                    return true
                }
            }
            return false
        }
    }
}

private class AddPublicGetterMethodsQuickFix(private val psiClass: PsiClass) : com.intellij.codeInspection.LocalQuickFix {
    override fun getFamilyName(): String {
        return "Add getter methods"
    }

    override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: com.intellij.codeInspection.ProblemDescriptor) {
        psiClass.fields.forEach { field ->
            val getterMethodName = "get" + field.name.capitalize()

            // Check if the getter method already exists
            if (psiClass.findMethodsByName(getterMethodName, false).isNotEmpty()) {
                return  // Getter method already exists
            }

            // Create the getter method
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val getterMethod = elementFactory.createMethod(getterMethodName, field.type)

            // Create the return statement for the getter method
            val body = getterMethod.body
            if (body != null) {
                val returnStatement = elementFactory.createStatementFromText(
                        ("return " + field.name).toString() + ";", psiClass)
                body.add(returnStatement)
            }

            // Add the getter method to the class
            psiClass.add(getterMethod)
        }
    }
}

private class MakeMemberVariablesPrivateQuickFix(private val psiClass: PsiClass, private val publicMemberVariables: List<PsiField>) : com.intellij.codeInspection.LocalQuickFix {

    override fun getFamilyName(): String {
        return "Make member variables private"
    }

    override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: com.intellij.codeInspection.ProblemDescriptor) {
        publicMemberVariables.forEach { field ->
            field.modifierList?.setModifierProperty(PsiModifier.PRIVATE, true)
        }
    }
}