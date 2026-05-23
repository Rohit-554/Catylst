package com.catylst.plugin.wizard

import com.catylst.plugin.model.AiProvider
import com.catylst.plugin.model.GeneratorConfig
import com.catylst.plugin.model.loadManifest
import com.catylst.plugin.template.ProjectGenerator
import com.catylst.plugin.wizard.panels.AiProviderPanel
import com.catylst.plugin.wizard.panels.FeatureSelectionPanel
import com.catylst.plugin.wizard.panels.ProjectConfigPanel
import com.catylst.plugin.wizard.panels.ThemePanel
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.ProjectGeneratorPeer
import java.io.File
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

data class CatylstProjectSettings(
    val appName: String,
    val packageName: String,
    val projectFolder: String,
    val features: Set<String>,
    val aiProvider: AiProvider,
    val sampleCode: Boolean,
    val themeSeedColor: String?,
    val themeExpressive: Boolean,
    val selectedPermissions: Set<String> = emptySet(),
    val outputDir: String = ""
)

class CatylstNewProjectWizard : DirectoryProjectGenerator<CatylstProjectSettings> {

    override fun getName(): String = "Catylst KMP"

    override fun getLogo(): Icon? = null

    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    override fun createPeer(): ProjectGeneratorPeer<CatylstProjectSettings> =
        CatylstProjectGeneratorPeer()

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: CatylstProjectSettings,
        module: com.intellij.openapi.module.Module
    ) {
        val outputDir = File(baseDir.path).parentFile ?: File(baseDir.path)
        val config = GeneratorConfig(
            packageName = settings.packageName,
            appName = settings.appName,
            projectName = settings.projectFolder,
            features = settings.features,
            sampleCode = settings.sampleCode,
            aiProvider = if ("ai" in settings.features) settings.aiProvider else AiProvider.NONE,
            themeSeedColor = settings.themeSeedColor,
            themeExpressive = settings.themeExpressive,
            outputDir = outputDir
        )

        ProgressManager.getInstance().run(object : Task.Modal(project, "Generating Catylst KMP Project", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                try {
                    ProjectGenerator.generate(config) { message ->
                        indicator.text = message
                        indicator.fraction += 0.1
                    }
                } catch (e: Exception) {
                    Messages.showErrorDialog(project, e.message ?: "Unknown error", "Generation Failed")
                }
            }
        })
    }
}

class CatylstProjectGeneratorPeer : ProjectGeneratorPeer<CatylstProjectSettings> {

    private val manifest = runCatching { loadManifest() }.getOrNull()

    private val configPanel = ProjectConfigPanel()
    private val featurePanel = FeatureSelectionPanel(manifest?.features ?: emptyList())
    private val aiPanel = AiProviderPanel()
    private val themePanel = ThemePanel()

    private val root: JPanel = JPanel().also { root ->
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.add(configPanel)
        root.add(featurePanel)
        root.add(aiPanel)
        root.add(themePanel)

        // Initial AI section visibility
        aiPanel.showAiSection(featurePanel.isAiSelected())
    }

    private val scrollPane = JScrollPane(root).also { it.border = null }

    override fun getComponent(): JComponent = scrollPane

    override fun buildUI(settingsStep: SettingsStep) {
        settingsStep.addExpertPanel(scrollPane)
    }

    override fun getSettings(): CatylstProjectSettings = CatylstProjectSettings(
        appName = configPanel.appNameField.text.trim(),
        packageName = configPanel.packageNameField.text.trim(),
        projectFolder = configPanel.projectFolderField.text.trim(),
        features = featurePanel.selectedFeatures(),
        aiProvider = aiPanel.selectedProvider(),
        sampleCode = aiPanel.isSampleCodeEnabled(),
        themeSeedColor = themePanel.seedColorHex(),
        themeExpressive = themePanel.isExpressive()
    )

    override fun validate(): ValidationInfo? {
        if (!configPanel.isComponentValid()) {
            return ValidationInfo("Please enter a valid app name and package name.")
        }
        return null
    }

    override fun addSettingsListener(listener: ProjectGeneratorPeer.SettingsListener) {}

    override fun isBackgroundJobRunning(): Boolean = false
}
