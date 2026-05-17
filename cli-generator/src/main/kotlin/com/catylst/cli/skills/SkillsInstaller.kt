package com.catylst.cli.skills

import com.catylst.cli.model.SkillEntry
import com.catylst.cli.model.SkillSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object SkillsInstaller {

    // ── Catalogue ─────────────────────────────────────────────────────────────
    //  Sections:  kmpSkills (optional, project-specific) | toolSkills | remoteSkills

    val kmpSkills: List<SkillEntry> = listOf(
        local("bloom-build",    "Bloom Build",    "Build new screens end-to-end in your project: composable UI, navigation, ViewModel, Room Entity/DAO/Repository, and Koin DI", "kmp", SkillSource.LOCAL_KMP, ".claude/skills/bloom-build"),
        local("bloom-navigate", "Bloom Navigate", "Modify your project: swap AI provider, add notification channels, configure permissions, or cleanly remove any feature", "kmp", SkillSource.LOCAL_KMP, ".claude/skills/bloom-navigate"),
    )

    val toolSkills: List<SkillEntry> = listOf(
        local("opsx",       "OPSX Workflow",    "OpenSpec workflow: propose, apply, explore, archive changes",  "workflow", SkillSource.LOCAL_OPSX, ".claude/commands/opsx"),
        local("clean-code", "Clean Code",       "Enforces clean code guidelines on every write/edit/refactor", "quality",  SkillSource.LOCAL_TOOL, ".claude/skills/clean-code"),
        local("figma-mcp",  "Figma → Compose",  "Convert Figma designs into production Compose Multiplatform", "design",   SkillSource.LOCAL_TOOL, ".claude/skills/figma-mcp"),
    )

    val remoteSkills: List<SkillEntry> = listOf(
        remote("android-cli-agents", "Android CLI Agents",  "Google's agent-native Android CLI — 3× faster, 70% fewer tokens",  "community",
            skillMdUrl     = "https://raw.githubusercontent.com/Rohit-554/Android-KMP-Skills-Hub/main/IAmSpeed/android-cli-agents.md",
            contentsApiUrl = null),
        remote("material-3-skill",   "Material 3 Design",   "Material You theming, color schemes, and typography",               "community",
            skillMdUrl     = "https://raw.githubusercontent.com/hamen/material-3-skill/master/SKILL.md",
            contentsApiUrl = "https://api.github.com/repos/hamen/material-3-skill/contents"),
        remote("java-to-kotlin",     "Java → Kotlin",       "Convert Java files to idiomatic Kotlin, preserving git history",    "community",
            skillMdUrl     = "https://raw.githubusercontent.com/Kotlin/kotlin-agent-skills/main/skills/kotlin-tooling-java-to-kotlin/SKILL.md",
            contentsApiUrl = "https://api.github.com/repos/Kotlin/kotlin-agent-skills/contents/skills/kotlin-tooling-java-to-kotlin"),
        remote("cocoapods-spm",      "CocoaPods → SPM",     "Migrate iOS dependencies from CocoaPods to Swift Package Manager",  "community",
            skillMdUrl     = "https://raw.githubusercontent.com/Kotlin/kotlin-agent-skills/main/skills/kotlin-tooling-cocoapods-spm-migration/SKILL.md",
            contentsApiUrl = "https://api.github.com/repos/Kotlin/kotlin-agent-skills/contents/skills/kotlin-tooling-cocoapods-spm-migration"),
        remote("compose-expert",     "Compose Expert",      "Jetpack Compose UI patterns, state hoisting, and best practices",   "community",
            skillMdUrl     = "https://raw.githubusercontent.com/aldefy/compose-skill/master/skills/compose-expert/SKILL.md",
            contentsApiUrl = "https://api.github.com/repos/aldefy/compose-skill/contents/skills/compose-expert"),
        remote("kotlin-specialist",  "Kotlin Specialist",   "Idiomatic Kotlin, coroutines, Flow, and language best practices",   "community",
            skillMdUrl     = "https://raw.githubusercontent.com/Jeffallan/claude-skills/main/skills/kotlin-specialist/SKILL.md",
            contentsApiUrl = "https://api.github.com/repos/Jeffallan/claude-skills/contents/skills/kotlin-specialist"),
    )

    /** Skills shown in the wizard — KMP skills are pre-selected by default. */
    val catalogue: List<SkillEntry> = kmpSkills + toolSkills + remoteSkills

    /** KMP skills are pre-selected; everything else is opt-in. */
    val defaultSelected: Set<String> = kmpSkills.map { it.id }.toSet()

    // ── Install ───────────────────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Installs selected skills into the generated project.
     *
     * @param projectDir   Root of the generated project (output dir / project name)
     * @param templateDir  Root of the source template (used to copy local skills)
     * @param skills       Skills the user selected
     */
    fun install(projectDir: File, templateDir: File, skills: List<SkillEntry>) {
        for (skill in skills) {
            println("  📥  Installing: ${skill.label}")
            try {
                when (skill.source) {
                    SkillSource.LOCAL_KMP, SkillSource.LOCAL_TOOL -> installLocalSkill(skill, templateDir, projectDir)
                    SkillSource.LOCAL_OPSX                        -> installOpsx(templateDir, projectDir)
                    SkillSource.REMOTE                            -> installRemoteSkill(skill, projectDir)
                }
                println("     ✓  ${skill.id}")
            } catch (e: Exception) {
                println("     \u001B[33m⚠  Failed to install ${skill.label}: ${e.message}\u001B[0m")
            }
        }
    }

    // ── Local install helpers ─────────────────────────────────────────────────

    private fun installLocalSkill(skill: SkillEntry, templateDir: File, projectDir: File) {
        val srcDir  = File(templateDir, skill.localRelativePath!!)
        // Local skills always go into .claude/skills/<id>/
        val destDir = File(projectDir, ".claude/skills/${skill.id}")
        if (!srcDir.exists()) {
            println("     \u001B[33m⚠  Source not found: ${srcDir.absolutePath}\u001B[0m")
            return
        }
        copyDir(srcDir, destDir)
    }

    private fun installOpsx(templateDir: File, projectDir: File) {
        val srcDir  = File(templateDir, ".claude/commands/opsx")
        val destDir = File(projectDir, ".claude/commands/opsx")
        if (!srcDir.exists()) {
            println("     \u001B[33m⚠  OPSX source not found: ${srcDir.absolutePath}\u001B[0m")
            return
        }
        copyDir(srcDir, destDir)
    }

    private fun copyDir(src: File, dest: File) {
        dest.mkdirs()
        src.walkTopDown().forEach { file ->
            val relative = file.relativeTo(src)
            val target   = File(dest, relative.path)
            if (file.isDirectory) target.mkdirs()
            else Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    // ── Remote install helpers ────────────────────────────────────────────────

    private fun installRemoteSkill(skill: SkillEntry, projectDir: File) {
        val destDir  = File(projectDir, ".claude/skills/${skill.id}").also { it.mkdirs() }
        val fileName = if (skill.source == SkillSource.REMOTE && skill.contentsApiUrl == null)
            "${skill.id}.md" else "SKILL.md"
        download(skill.skillMdUrl, File(destDir, fileName))

        if (skill.contentsApiUrl != null) {
            downloadSupportingFiles(skill.contentsApiUrl, destDir, mainFile = fileName)
        }
    }

    private fun downloadSupportingFiles(contentsApiUrl: String, destDir: File, mainFile: String) {
        val text     = try { get(contentsApiUrl) } catch (_: Exception) { return }
        val elements = try { json.parseToJsonElement(text).jsonArray } catch (_: Exception) { return }

        for (element in elements) {
            val obj  = element.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: continue
            val type = obj["type"]?.jsonPrimitive?.content ?: continue
            if (name == mainFile || name == "SKILL.md") continue

            when (type) {
                "file" -> {
                    val dlUrl = obj["download_url"]?.jsonPrimitive?.content ?: continue
                    try { download(dlUrl, File(destDir, name)) } catch (_: Exception) {}
                }
                "dir" -> {
                    if (name !in setOf("assets", "references")) continue
                    val subApi = obj["url"]?.jsonPrimitive?.content ?: continue
                    val subDir = File(destDir, name).also { it.mkdirs() }
                    downloadSupportingFiles(subApi, subDir, mainFile = "")
                }
            }
        }
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private fun get(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.setRequestProperty("User-Agent", "catylst-cli/1.0")
        conn.connectTimeout = 8_000
        conn.readTimeout    = 8_000
        return conn.inputStream.bufferedReader().readText()
    }

    private fun download(urlString: String, dest: File) {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "catylst-cli/1.0")
        conn.connectTimeout = 8_000
        conn.readTimeout    = 15_000
        dest.parentFile?.mkdirs()
        conn.inputStream.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    private fun local(id: String, label: String, description: String, category: String,
                      source: SkillSource, path: String) =
        SkillEntry(id = id, label = label, description = description, category = category,
                   source = source, localRelativePath = path)

    private fun remote(id: String, label: String, description: String, category: String,
                       skillMdUrl: String, contentsApiUrl: String?) =
        SkillEntry(id = id, label = label, description = description, category = category,
                   source = SkillSource.REMOTE, skillMdUrl = skillMdUrl, contentsApiUrl = contentsApiUrl)
}
