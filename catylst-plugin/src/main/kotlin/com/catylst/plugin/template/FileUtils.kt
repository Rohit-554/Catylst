package com.catylst.plugin.template

import java.io.File

private val SKIP_DIRS = setOf("build", ".gradle", ".git", ".idea", ".kotlin")

fun File.walkSrc(): Sequence<File> =
    walkTopDown().onEnter { dir -> dir.name !in SKIP_DIRS }

fun findFile(projectDir: File, name: String): File? =
    projectDir.walkSrc().firstOrNull { it.isFile && it.name == name }

fun findDir(projectDir: File, name: String): File? =
    projectDir.walkSrc().firstOrNull { it.isDirectory && it.name == name }
