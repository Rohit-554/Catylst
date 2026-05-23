package com.catylst.plugin.wizard.panels

import com.catylst.plugin.model.AiProvider
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

class AiProviderPanel : JPanel() {

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    private val claudeRadio = JRadioButton("Claude", true)
    private val groqRadio = JRadioButton("Groq")
    private val geminiRadio = JRadioButton("Gemini")
    val sampleCodeCheckbox = JCheckBox("Include sample / demo code", true)

    init {
        layout = GridBagLayout()
        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 8, 2, 8)
            anchor = GridBagConstraints.WEST
            gridx = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        }

        // AI provider section (shown only when AI feature selected)
        val aiPanel = JPanel(GridBagLayout()).also { panel ->
            panel.border = BorderFactory.createTitledBorder("AI Provider")
            val bg = ButtonGroup().also { it.add(claudeRadio); it.add(groqRadio); it.add(geminiRadio) }
            val inner = GridBagConstraints().apply {
                insets = Insets(2, 4, 2, 4)
                anchor = GridBagConstraints.WEST
                gridx = 0; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL
            }
            panel.add(claudeRadio, inner.also { it.gridy = 0 })
            panel.add(groqRadio, inner.also { it.gridy = 1 })
            panel.add(geminiRadio, inner.also { it.gridy = 2 })
        }

        cardPanel.add(aiPanel, "visible")
        cardPanel.add(JPanel(), "hidden")
        cardLayout.show(cardPanel, "hidden")

        add(cardPanel, gbc.also { it.gridy = 0 })

        add(JLabel("<html><b>Options</b></html>"), gbc.also { it.gridy = 1; it.insets = Insets(8, 8, 4, 8) })
        add(sampleCodeCheckbox, gbc.also { it.gridy = 2 })
    }

    fun showAiSection(visible: Boolean) {
        cardLayout.show(cardPanel, if (visible) "visible" else "hidden")
    }

    fun selectedProvider(): AiProvider = when {
        claudeRadio.isSelected -> AiProvider.CLAUDE
        groqRadio.isSelected   -> AiProvider.GROQ
        geminiRadio.isSelected -> AiProvider.GEMINI
        else                   -> AiProvider.CLAUDE
    }

    fun isSampleCodeEnabled(): Boolean = sampleCodeCheckbox.isSelected
}
