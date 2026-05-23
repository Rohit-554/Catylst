package com.catylst.plugin.action

import com.catylst.plugin.wizard.CatylstWizardDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class NewCatylstProjectAction : AnAction("Catylst KMP Project…", "Create a new Kotlin Multiplatform project with Catylst", null) {

    override fun actionPerformed(e: AnActionEvent) {
        CatylstWizardDialog(e.project).show()
    }
}
