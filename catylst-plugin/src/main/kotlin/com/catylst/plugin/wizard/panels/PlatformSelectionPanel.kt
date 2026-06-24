package com.catylst.plugin.wizard.panels

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JPanel

/**
 * Lets the user pick which platforms the generated project targets.
 * Android, iOS and Desktop are all enabled by default; at least one must stay selected.
 */
class PlatformSelectionPanel : JPanel(GridBagLayout()) {

    val androidCheckbox = JCheckBox("Android", true)
    val iosCheckbox = JCheckBox("iOS", true)
    val desktopCheckbox = JCheckBox("Desktop (JVM)", true)

    init {
        border = BorderFactory.createTitledBorder("Target Platforms")
        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 8, 2, 8)
            anchor = GridBagConstraints.WEST
            gridx = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        }
        add(androidCheckbox, gbc.also { it.gridy = 0 })
        add(iosCheckbox, gbc.also { it.gridy = 1 })
        add(desktopCheckbox, gbc.also { it.gridy = 2 })
    }

    fun includeAndroid(): Boolean = androidCheckbox.isSelected
    fun includeIos(): Boolean = iosCheckbox.isSelected
    fun includeDesktop(): Boolean = desktopCheckbox.isSelected

    /** At least one platform must be selected for a valid project. */
    fun hasSelection(): Boolean = includeAndroid() || includeIos() || includeDesktop()
}
