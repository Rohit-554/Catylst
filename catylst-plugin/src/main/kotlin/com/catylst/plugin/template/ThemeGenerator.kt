package com.catylst.plugin.template

import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import java.io.File

object ThemeGenerator {

    fun generate(
        projectDir: File,
        packageName: String,
        seedColorHex: String,
        expressiveMotion: Boolean = false,
        fonts: Map<String, File?> = emptyMap()
    ) {
        val seedArgb = parseColor(seedColorHex)
        val sourceHct = Hct.fromInt(seedArgb)

        val lightScheme = SchemeTonalSpot(sourceHct, false, 0.0)
        val darkScheme = SchemeTonalSpot(sourceHct, true, 0.0)

        val colors = MaterialDynamicColors()

        val lightColors = extractColorRoles(colors, lightScheme)
        val darkColors = extractColorRoles(colors, darkScheme)

        writeColorKt(projectDir, packageName, lightColors, darkColors)
        writeThemeKt(projectDir, packageName, expressiveMotion)
        writeTypographyKt(projectDir, packageName)
        updateAppKt(projectDir, packageName)
    }

    private fun parseColor(hex: String): Int {
        val clean = hex.removePrefix("#")
        return if (clean.length == 6) {
            0xFF000000.toInt() or clean.toLong(16).toInt()
        } else {
            clean.toLong(16).toInt()
        }
    }

    private fun extractColorRoles(colors: MaterialDynamicColors, scheme: DynamicScheme): Map<String, Int> {
        return mapOf(
            "primary" to colors.primary().getArgb(scheme),
            "onPrimary" to colors.onPrimary().getArgb(scheme),
            "primaryContainer" to colors.primaryContainer().getArgb(scheme),
            "onPrimaryContainer" to colors.onPrimaryContainer().getArgb(scheme),
            "secondary" to colors.secondary().getArgb(scheme),
            "onSecondary" to colors.onSecondary().getArgb(scheme),
            "secondaryContainer" to colors.secondaryContainer().getArgb(scheme),
            "onSecondaryContainer" to colors.onSecondaryContainer().getArgb(scheme),
            "tertiary" to colors.tertiary().getArgb(scheme),
            "onTertiary" to colors.onTertiary().getArgb(scheme),
            "tertiaryContainer" to colors.tertiaryContainer().getArgb(scheme),
            "onTertiaryContainer" to colors.onTertiaryContainer().getArgb(scheme),
            "error" to colors.error().getArgb(scheme),
            "onError" to colors.onError().getArgb(scheme),
            "errorContainer" to colors.errorContainer().getArgb(scheme),
            "onErrorContainer" to colors.onErrorContainer().getArgb(scheme),
            "background" to colors.background().getArgb(scheme),
            "onBackground" to colors.onBackground().getArgb(scheme),
            "surface" to colors.surface().getArgb(scheme),
            "onSurface" to colors.onSurface().getArgb(scheme),
            "surfaceVariant" to colors.surfaceVariant().getArgb(scheme),
            "onSurfaceVariant" to colors.onSurfaceVariant().getArgb(scheme),
            "surfaceTint" to colors.primary().getArgb(scheme),
            "inverseSurface" to colors.inverseSurface().getArgb(scheme),
            "inverseOnSurface" to colors.inverseOnSurface().getArgb(scheme),
            "inversePrimary" to colors.inversePrimary().getArgb(scheme),
            "outline" to colors.outline().getArgb(scheme),
            "outlineVariant" to colors.outlineVariant().getArgb(scheme),
            "scrim" to colors.scrim().getArgb(scheme),
            "surfaceBright" to colors.surfaceBright().getArgb(scheme),
            "surfaceDim" to colors.surfaceDim().getArgb(scheme),
            "surfaceContainer" to colors.surfaceContainer().getArgb(scheme),
            "surfaceContainerHigh" to colors.surfaceContainerHigh().getArgb(scheme),
            "surfaceContainerHighest" to colors.surfaceContainerHighest().getArgb(scheme),
            "surfaceContainerLow" to colors.surfaceContainerLow().getArgb(scheme),
            "surfaceContainerLowest" to colors.surfaceContainerLowest().getArgb(scheme),
        )
    }

    private fun argbToHex(argb: Int): String {
        return String.format("0xFF%06X", argb and 0xFFFFFF)
    }

    private fun writeColorKt(
        projectDir: File,
        packageName: String,
        lightColors: Map<String, Int>,
        darkColors: Map<String, Int>
    ) {
        val themeDir = findOrCreateThemeDir(projectDir, packageName)
        val file = File(themeDir, "Color.kt")

        val lightEntries = lightColors.entries.joinToString("\n") { (name, color) ->
            "    $name = Color(${argbToHex(color)}),"
        }

        val darkEntries = darkColors.entries.joinToString("\n") { (name, color) ->
            "    $name = Color(${argbToHex(color)}),"
        }

        val content = """package ${packageName}.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
$lightEntries
)

val DarkColorScheme = darkColorScheme(
$darkEntries
)
""".trimIndent()

        file.writeText(content)
    }

    private fun writeThemeKt(projectDir: File, packageName: String, expressiveMotion: Boolean) {
        val themeDir = findOrCreateThemeDir(projectDir, packageName)
        val file = File(themeDir, "Theme.kt")

        val motionLine = if (expressiveMotion) {
            "        motionScheme = MotionScheme.expressive(),"
        } else ""

        val content = """package ${packageName}.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
${if (expressiveMotion) "import androidx.compose.material3.MotionScheme" else ""}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
$motionLine
        content = content
    )
}
""".trimIndent()

        file.writeText(content)
    }

    private fun writeTypographyKt(projectDir: File, packageName: String) {
        val themeDir = findOrCreateThemeDir(projectDir, packageName)
        val file = File(themeDir, "Typography.kt")

        val content = """package ${packageName}.ui.theme

import androidx.compose.material3.Typography

val AppTypography = Typography()
""".trimIndent()

        file.writeText(content)
    }

    private fun updateAppKt(projectDir: File, packageName: String) {
        val appFile = findFile(projectDir, "App.kt") ?: return
        var content = appFile.readText()

        if (!content.contains("${packageName}.ui.theme.AppTheme")) {
            val pkgLine = content.lines().firstOrNull { it.startsWith("package ") } ?: ""
            content = content.replace(
                pkgLine,
                "$pkgLine\n\nimport ${packageName}.ui.theme.AppTheme"
            )
        }

        content = content.replace("MaterialTheme {", "AppTheme {")
        appFile.writeText(content)
    }

    private fun findOrCreateThemeDir(projectDir: File, packageName: String): File {
        val pkgPath = packageName.replace(".", "/")
        val themeDir = File(projectDir, "composeApp/src/commonMain/kotlin/${pkgPath}/ui/theme")
        themeDir.mkdirs()
        return themeDir
    }
}
