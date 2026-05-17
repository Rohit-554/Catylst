package com.catylst.cli.template

import java.io.File

private val SKIP_DIRS = setOf("build", ".gradle", ".git", ".idea", ".kotlin")

/**
 * Walks the project source tree, skipping build/tooling directories.
 * Use this everywhere instead of bare walkTopDown() to avoid matching
 * stale build-output files.
 */
fun File.walkSrc(): Sequence<File> =
    walkTopDown().onEnter { dir -> dir.name !in SKIP_DIRS }

/** Find a file by name anywhere in the project source tree. */
fun findFile(projectDir: File, name: String): File? =
    projectDir.walkSrc().firstOrNull { it.isFile && it.name == name }

/** Find a directory by name anywhere in the project source tree. */
fun findDir(projectDir: File, name: String): File? =
    projectDir.walkSrc().firstOrNull { it.isDirectory && it.name == name }
