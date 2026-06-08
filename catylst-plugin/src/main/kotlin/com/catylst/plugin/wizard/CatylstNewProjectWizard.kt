package com.catylst.plugin.wizard

import com.catylst.plugin.model.AiProvider

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

fun CatylstProjectSettings.effectiveAiProvider() =
    if ("ai" in features) aiProvider else AiProvider.NONE
