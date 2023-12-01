package com.example.casdocplugin

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;

class PublicClassDocumentation: AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object: JavaElementVisitor() {
        override fun visitClass(psiClass: PsiClass) {
            super.visitClass(psiClass)
                // Find the class name identifier
                val classIdentifier = PsiTreeUtil.findChildOfType(psiClass, PsiIdentifier::class.java)
                val fields = psiClass.fields

                // additional warning with quick fix if no members
                if (fields.isEmpty()) {
                    holder.registerProblem(
                            psiClass.originalElement,
                            "A Class is a unit of code that defines some relation between data and actions on that data. A Card Class isn't going to be very useful if it doesn't hold any data",
                            ProblemHighlightType.WARNING,
                            AddMemberVariablesQuickFix(psiClass)
                    )
                }
                // Add a warning for the class name only
                classIdentifier?.let {
                    val className = classIdentifier.text
                    holder.registerProblem(
                            classIdentifier,
                            "$className\n\nA first step to have good encapsulation is to decide on the abstractions of your problem domain that will be represented as (Java) types.\nA playing card is one such concept. It maps to a clear element from the problem domain, that we will want to manipulate in our program.\n",
                            ProblemHighlightType.WARNING
                    )
                }
        }
    }
}

private class AddMemberVariablesQuickFix(private val psiClass: PsiClass) : com.intellij.codeInspection.LocalQuickFix {

    override fun getFamilyName(): String {
        return "Add suit and rank variables"
    }

    override fun applyFix(project: com.intellij.openapi.project.Project, descriptor: com.intellij.codeInspection.ProblemDescriptor) {
        val elementFactory = PsiElementFactory.SERVICE.getInstance(project)

        // Create "suit" field
        val suitField = elementFactory.createField("suit", elementFactory.createTypeByFQClassName("java.lang.String"))
        suitField.modifierList?.setModifierProperty(PsiModifier.PUBLIC, true)

        // Create "rank" field
        val rankField = elementFactory.createField("rank", elementFactory.createTypeByFQClassName("java.lang.String"))
        rankField.modifierList?.setModifierProperty(PsiModifier.PUBLIC, true)

        // Add the fields to the class
        psiClass.add(suitField)
        psiClass.add(rankField)
    }
}