package com.catylst.plugin.wizard.panels

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private class ColorWheelPicker(initialColor: Color) : JPanel() {

    private val size = 160
    private val indicatorR = 6

    var selectedColor: Color = initialColor
        private set

    var onColorSelected: (Color) -> Unit = {}

    private var indicatorX: Int
    private var indicatorY: Int

    private val wheelImage: BufferedImage by lazy { buildWheelImage() }

    init {
        isOpaque = false
        preferredSize = Dimension(size, size)
        minimumSize = preferredSize

        val hsb = Color.RGBtoHSB(initialColor.red, initialColor.green, initialColor.blue, null)
        val angle = hsb[0] * 2 * Math.PI
        val dist = hsb[1] * (size / 2.0 - indicatorR)
        indicatorX = (size / 2 + dist * cos(angle)).toInt()
        indicatorY = (size / 2 - dist * sin(angle)).toInt()

        val handler = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = pick(e)
            override fun mouseDragged(e: MouseEvent) = pick(e)
        }
        addMouseListener(handler)
        addMouseMotionListener(handler)
    }

    private fun pick(e: MouseEvent) {
        if (!isEnabled) return
        val cx = size / 2.0
        val cy = size / 2.0
        val dx = e.x - cx
        val dy = e.y - cy
        val dist = sqrt(dx * dx + dy * dy)
        val maxR = size / 2.0 - indicatorR
        val clamped = min(dist, maxR)
        val angle = atan2(-dy, dx)
        indicatorX = (cx + clamped * cos(angle)).toInt()
        indicatorY = (cy - clamped * sin(angle)).toInt()
        val hue = ((angle / (2 * Math.PI)) + 1.0) % 1.0
        val saturation = (clamped / maxR).toFloat()
        selectedColor = Color(Color.HSBtoRGB(hue.toFloat(), saturation, 1.0f))
        onColorSelected(selectedColor)
        repaint()
    }

    private fun buildWheelImage(): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val cx = size / 2.0
        val cy = size / 2.0
        val r = size / 2.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= r) {
                    val angle = atan2(-dy, dx)
                    val hue = ((angle / (2 * Math.PI)) + 1.0) % 1.0
                    val sat = (dist / r).toFloat().coerceIn(0f, 1f)
                    val rgb = Color.HSBtoRGB(hue.toFloat(), sat, 1.0f)
                    img.setRGB(x, y, rgb or (0xFF shl 24))
                }
            }
        }
        return img
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            if (!isEnabled) {
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f)
            }
            g2.drawImage(wheelImage, 0, 0, null)
            // Outer white ring then black ring for contrast
            g2.stroke = BasicStroke(2.5f)
            g2.color = Color.WHITE
            g2.drawOval(
                indicatorX - indicatorR, indicatorY - indicatorR,
                indicatorR * 2, indicatorR * 2
            )
            g2.stroke = BasicStroke(1f)
            g2.color = Color.BLACK
            g2.drawOval(
                indicatorX - indicatorR - 1, indicatorY - indicatorR - 1,
                indicatorR * 2 + 2, indicatorR * 2 + 2
            )
        } finally {
            g2.dispose()
        }
    }

    fun selectColor(color: Color) {
        selectedColor = color
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        val angle = hsb[0] * 2 * Math.PI
        val maxR = size / 2.0 - indicatorR
        val dist = hsb[1] * maxR
        indicatorX = (size / 2 + dist * cos(angle)).toInt()
        indicatorY = (size / 2 - dist * sin(angle)).toInt()
        repaint()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        cursor = if (enabled) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                 else Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        repaint()
    }
}

class ThemePanel : JPanel(GridBagLayout()) {

    private val customiseCheckbox = JCheckBox("Customise theme")
    val expressiveCheckbox = JCheckBox("Enable Material 3 Expressive motion (alpha)").also {
        it.isEnabled = false
    }

    private val colorPicker = ColorWheelPicker(Color(0x6750A4)).also {
        it.isEnabled = false
    }

    private val hexField = JTextField("#6750A4", 8).also {
        it.isEnabled = false
    }

    private var suppressSync = false

    init {
        border = BorderFactory.createTitledBorder("Theme")

        wireWheelToHexField()
        wireHexFieldToWheel()

        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 8, 2, 8)
            anchor = GridBagConstraints.WEST
            gridx = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        }

        add(customiseCheckbox, gbc.also { it.gridy = 0 })

        val colorRow = JPanel().also { row ->
            row.isOpaque = false
            row.add(JLabel("Seed color:"))
            row.add(colorPicker)
            row.add(hexField)
        }
        add(colorRow, gbc.also { it.gridy = 1 })
        add(expressiveCheckbox, gbc.also { it.gridy = 2 })

        customiseCheckbox.addActionListener {
            val enabled = customiseCheckbox.isSelected
            colorPicker.isEnabled = enabled
            hexField.isEnabled = enabled
            expressiveCheckbox.isEnabled = enabled
        }
    }

    private fun wireWheelToHexField() {
        colorPicker.onColorSelected = { color ->
            if (!suppressSync) {
                suppressSync = true
                hexField.text = hexOf(color)
                suppressSync = false
            }
        }
    }

    private fun wireHexFieldToWheel() {
        hexField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyHexToWheel()
            override fun removeUpdate(e: DocumentEvent) = applyHexToWheel()
            override fun changedUpdate(e: DocumentEvent) = applyHexToWheel()
        })
    }

    private fun applyHexToWheel() {
        if (suppressSync) return
        val parsed = parseHex(hexField.text) ?: return
        suppressSync = true
        colorPicker.selectColor(parsed)
        suppressSync = false
    }

    private fun parseHex(raw: String): Color? {
        val cleaned = raw.trimStart('#')
        if (cleaned.length != 6) return null
        return runCatching { Color(cleaned.toInt(16)) }.getOrNull()
    }

    private fun hexOf(color: Color) = "#%06X".format(color.rgb and 0xFFFFFF)

    fun isCustomising(): Boolean = customiseCheckbox.isSelected

    fun seedColorHex(): String? {
        if (!customiseCheckbox.isSelected) return null
        return hexOf(colorPicker.selectedColor)
    }

    fun isExpressive(): Boolean = expressiveCheckbox.isSelected && customiseCheckbox.isSelected
}
