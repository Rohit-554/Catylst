package com.catylst.plugin.wizard.panels

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ProjectConfigPanel : JPanel(GridBagLayout()) {

    val appNameField = JTextField("MyApp", 30)
    val packageNameField = JTextField("com.example.myapp", 30)
    val projectFolderField = JTextField("MyApp", 30)

    val packageError = JLabel("").also { it.foreground = java.awt.Color.RED }

    private val packageRegex = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

    init {
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
        }

        fun row(label: String, field: JTextField, row: Int) {
            gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE
            add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
            add(field, gbc)
            gbc.weightx = 0.0
        }

        row("Application Name:", appNameField, 0)
        row("Package Name:", packageNameField, 1)

        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL
        add(packageError, gbc)

        row("Project Folder:", projectFolderField, 3)

        // Auto-populate project folder from app name
        appNameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = sync()
            override fun removeUpdate(e: DocumentEvent) = sync()
            override fun changedUpdate(e: DocumentEvent) = sync()
            private fun sync() {
                val sanitized = appNameField.text.trim().replace(" ", "")
                projectFolderField.text = sanitized
            }
        })

        packageNameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = validate()
            override fun removeUpdate(e: DocumentEvent) = validate()
            override fun changedUpdate(e: DocumentEvent) = validate()
            private fun validate() {
                packageError.text = if (isPackageValid()) "" else "Invalid package name"
            }
        })
    }

    fun isPackageValid(): Boolean = packageRegex.matches(packageNameField.text.trim())

    fun isComponentValid(): Boolean {
        val name = appNameField.text.trim()
        return name.isNotEmpty() && !name.contains(" ") && isPackageValid()
    }
}
