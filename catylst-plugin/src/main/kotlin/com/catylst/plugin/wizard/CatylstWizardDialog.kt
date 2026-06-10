package com.catylst.plugin.wizard

import com.catylst.plugin.model.GeneratorConfig
import com.catylst.plugin.model.loadManifest
import com.catylst.plugin.template.ProjectGenerator
import com.catylst.plugin.wizard.panels.FeatureSelectionPanel
import com.catylst.plugin.wizard.panels.ProjectConfigPanel
import com.catylst.plugin.wizard.panels.ThemePanel
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane

class CatylstWizardDialog(private val project: Project?) : DialogWrapper(project) {

    private val manifest = runCatching { loadManifest() }.getOrNull()

    private val configPanel = ProjectConfigPanel()
    private val featurePanel = FeatureSelectionPanel(manifest?.features ?: emptyList())
    private val themePanel = ThemePanel()
    private val locationField = createLocationField()

    init {
        title = "New Catylst KMP Project"
        setOKButtonText("Create Project")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val content = JPanel().also { panel ->
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.add(locationRow())
            panel.add(configPanel)
            panel.add(featurePanel)
            panel.add(themePanel)
        }
        return JScrollPane(content).also {
            it.border = null
            it.preferredSize = Dimension(600, 620)
            it.verticalScrollBar.unitIncrement = 16
            it.horizontalScrollBar.unitIncrement = 16
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (!configPanel.isComponentValid()) {
            return ValidationInfo("Please enter a valid app name and package name.", configPanel.packageNameField)
        }
        if (locationField.text.trim().isEmpty()) {
            return ValidationInfo("Please choose an output directory.", locationField)
        }
        return null
    }

    override fun doOKAction() {
        val settings = buildSettings()
        close(OK_EXIT_CODE)
        generateProject(settings)
    }

    private fun createLocationField(): TextFieldWithBrowseButton {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Choose Output Directory")
            .withDescription("Select where to create the project")
        return TextFieldWithBrowseButton().also { field ->
            field.text = "${System.getProperty("user.home")}/Projects"
            field.addBrowseFolderListener(TextBrowseFolderListener(descriptor, project))
        }
    }

    private fun locationRow(): JPanel = JPanel(GridBagLayout()).also { panel ->
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
        }
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Location:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(locationField, gbc)
    }

    private fun generateProject(settings: CatylstProjectSettings) {
        val outputDir = File(settings.outputDir).also { it.mkdirs() }
        val projectDir = File(outputDir, settings.projectFolder)
        val config = buildGeneratorConfig(settings, outputDir)
        ProgressManager.getInstance().run(generationTask(config, projectDir, settings.projectFolder))
    }

    private fun buildGeneratorConfig(settings: CatylstProjectSettings, outputDir: File): GeneratorConfig =
        GeneratorConfig(
            packageName = settings.packageName,
            appName = settings.appName,
            projectName = settings.projectFolder,
            features = settings.features,
            sampleCode = settings.sampleCode,
            aiProvider = settings.effectiveAiProvider(),
            themeSeedColor = settings.themeSeedColor,
            themeExpressive = settings.themeExpressive,
            selectedPermissions = settings.selectedPermissions,
            outputDir = outputDir
        )

    private fun generationTask(config: GeneratorConfig, projectDir: File, projectName: String) =
        object : Task.Modal(project, "Generating Catylst KMP Project", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                try {
                    runGeneration(config, indicator)
                    openGeneratedProject(projectDir, projectName)
                } catch (e: Exception) {
                    reportGenerationFailure(e)
                }
            }
        }

    private fun runGeneration(config: GeneratorConfig, indicator: ProgressIndicator) {
        ProjectGenerator.generate(config) { message ->
            indicator.text = message
            indicator.fraction = minOf(indicator.fraction + 0.1, 0.95)
        }
        indicator.fraction = 1.0
    }

    @Suppress("UnstableApiUsage")
    private fun openGeneratedProject(projectDir: File, projectName: String) {
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir)
        ApplicationManager.getApplication().invokeLater {
            ProjectUtil.openOrImport(
                projectDir.toPath(),
                OpenProjectTask.build()
                    .withForceOpenInNewFrame(true)
                    .withProjectName(projectName)
            )
        }
    }

    private fun reportGenerationFailure(e: Exception) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, e.message ?: "Unknown error", "Generation Failed")
        }
    }

    private fun buildSettings() = CatylstProjectSettings(
        appName = configPanel.appNameField.text.trim(),
        packageName = configPanel.packageNameField.text.trim(),
        projectFolder = configPanel.projectFolderField.text.trim(),
        features = featurePanel.selectedFeatures(),
        aiProvider = featurePanel.selectedAiProvider(),
        sampleCode = featurePanel.isSampleCodeEnabled(),
        themeSeedColor = themePanel.seedColorHex(),
        themeExpressive = themePanel.isExpressive(),
        selectedPermissions = featurePanel.selectedPermissions(),
        outputDir = locationField.text.trim()
    )
}
