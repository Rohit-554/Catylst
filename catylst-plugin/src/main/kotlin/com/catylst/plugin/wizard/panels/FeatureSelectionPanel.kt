package com.catylst.plugin.wizard.panels

import com.catylst.plugin.model.AiProvider
import com.catylst.plugin.model.FeatureDef
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.ButtonGroup
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

private data class SkillDef(val id: String, val label: String)

private val BUNDLED_SKILLS = listOf(
    SkillDef("bloom-build",       "Bloom Build"),
    SkillDef("bloom-navigate",    "Bloom Navigate"),
    SkillDef("compose-expert",    "Compose Expert"),
    SkillDef("clean-code",        "Clean Code"),
    SkillDef("figma-to-compose",  "Figma to Compose")
)

class FeatureSelectionPanel(features: List<FeatureDef>) : JPanel(GridBagLayout()) {

    private val featureCheckboxes    = mutableMapOf<String, JCheckBox>()
    private val permissionCheckboxes = mutableMapOf<String, JCheckBox>()
    private val skillCheckboxes      = mutableMapOf<String, JCheckBox>()

    private val claudeRadio  = JRadioButton("Claude", true)
    private val groqRadio    = JRadioButton("Groq")
    private val geminiRadio  = JRadioButton("Gemini")
    val sampleCodeCheckbox   = JCheckBox("Include sample / demo code", true)

    init {
        val gbc = baseConstraints()
        add(JLabel("<html><b>Select features to include:</b></html>"), gbc.also {
            it.gridy = 0; it.insets = Insets(8, 8, 4, 8)
        })

        var row = 1
        features.forEach { feature ->
            val checkbox = featureCheckbox(feature)
            featureCheckboxes[feature.id] = checkbox
            add(checkbox, gbc.also { it.gridy = row++; it.insets = Insets(2, 8, 2, 8) })

            when (feature.id) {
                "permissions" -> {
                    val subPanel = permissionSubPanel(feature)
                    add(subPanel, gbc.also { it.gridy = row++; it.insets = Insets(0, 28, 4, 8) })
                    checkbox.addActionListener { toggleSubPanel(subPanel, checkbox.isSelected) }
                    toggleSubPanel(subPanel, checkbox.isSelected)
                }
                "agent-skills" -> {
                    val subPanel = skillSubPanel()
                    add(subPanel, gbc.also { it.gridy = row++; it.insets = Insets(0, 28, 4, 8) })
                    checkbox.addActionListener { toggleSubPanel(subPanel, checkbox.isSelected) }
                    toggleSubPanel(subPanel, checkbox.isSelected)
                }
                "ai" -> {
                    val subPanel = aiProviderSubPanel()
                    add(subPanel, gbc.also { it.gridy = row++; it.insets = Insets(0, 28, 4, 8) })
                    checkbox.addActionListener { toggleSubPanel(subPanel, checkbox.isSelected) }
                    toggleSubPanel(subPanel, checkbox.isSelected)
                }
            }
        }

        add(sampleCodeCheckbox, gbc.also { it.gridy = row; it.insets = Insets(6, 8, 4, 8) })

        wireAiKtorDependency()
    }

    fun selectedFeatures(): Set<String> =
        featureCheckboxes.filter { (_, cb) -> cb.isSelected }.keys.toSet()

    fun selectedPermissions(): Set<String> =
        permissionCheckboxes.filter { (_, cb) -> cb.isSelected }.keys.toSet()

    fun selectedSkills(): Set<String> =
        skillCheckboxes.filter { (_, cb) -> cb.isSelected }.keys.toSet()

    fun isAiSelected(): Boolean = featureCheckboxes["ai"]?.isSelected == true

    private fun permissionSubPanel(feature: FeatureDef): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = subOptionConstraints()
        feature.permissionTypes.forEachIndexed { i, perm ->
            val cb = JCheckBox(perm.label, true)
            permissionCheckboxes[perm.id] = cb
            panel.add(cb, gbc.also { it.gridy = i })
        }
        return panel
    }

    private fun skillSubPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = subOptionConstraints()
        BUNDLED_SKILLS.forEachIndexed { i, skill ->
            val cb = JCheckBox(skill.label, true)
            skillCheckboxes[skill.id] = cb
            panel.add(cb, gbc.also { it.gridy = i })
        }
        return panel
    }

    private fun toggleSubPanel(panel: JPanel, visible: Boolean) {
        panel.isVisible = visible
        revalidate()
        repaint()
    }

    fun selectedAiProvider(): AiProvider = when {
        !isAiSelected()      -> AiProvider.NONE
        claudeRadio.isSelected -> AiProvider.CLAUDE
        groqRadio.isSelected   -> AiProvider.GROQ
        geminiRadio.isSelected -> AiProvider.GEMINI
        else                   -> AiProvider.CLAUDE
    }

    fun isSampleCodeEnabled(): Boolean = sampleCodeCheckbox.isSelected

    private fun aiProviderSubPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = subOptionConstraints()
        ButtonGroup().also { it.add(claudeRadio); it.add(groqRadio); it.add(geminiRadio) }
        panel.add(claudeRadio,  gbc.also { it.gridy = 0 })
        panel.add(groqRadio,    gbc.also { it.gridy = 1 })
        panel.add(geminiRadio,  gbc.also { it.gridy = 2 })
        return panel
    }

    private fun wireAiKtorDependency() {
        featureCheckboxes["ai"]?.addActionListener {
            val aiSelected = featureCheckboxes["ai"]?.isSelected == true
            featureCheckboxes["ktor"]?.let { ktorCb ->
                if (aiSelected) {
                    ktorCb.isSelected = true
                    ktorCb.isEnabled = false
                    ktorCb.toolTipText = "Required by AI Integration"
                } else {
                    ktorCb.isEnabled = true
                    ktorCb.toolTipText = null
                }
            }
        }
    }

    private fun featureCheckbox(feature: FeatureDef) = JCheckBox(
        "<html><b>${feature.name}</b><br><font color='#888888' size='-1'>${feature.description}</font></html>",
        feature.default
    )

    private fun baseConstraints() = GridBagConstraints().apply {
        anchor = GridBagConstraints.WEST
        gridx = 0
        weightx = 1.0
        fill = GridBagConstraints.HORIZONTAL
    }

    private fun subOptionConstraints() = GridBagConstraints().apply {
        insets = Insets(1, 4, 1, 4)
        anchor = GridBagConstraints.WEST
        gridx = 0
        weightx = 1.0
        fill = GridBagConstraints.HORIZONTAL
    }
}
