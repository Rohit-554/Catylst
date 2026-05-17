package com.catylst.cli

import org.jline.terminal.TerminalBuilder
import java.io.File

// ─── ANSI ────────────────────────────────────────────────────────────────────
private fun bold(s: String)    = "\u001B[1m$s\u001B[0m"
private fun dim(s: String)     = "\u001B[2m$s\u001B[0m"
private fun green(s: String)   = "\u001B[32m$s\u001B[0m"
private fun cyan(s: String)    = "\u001B[36m$s\u001B[0m"
private fun yellow(s: String)  = "\u001B[33m$s\u001B[0m"
private fun bgCyan(s: String)  = "\u001B[46m\u001B[30m$s\u001B[0m"

// ─── Key legend ───────────────────────────────────────────────────────────────
private fun keyHint(key: String, desc: String) = "${bold(yellow(key))} ${dim(desc)}"

private val CHECKBOX_LEGEND =
    "  " + listOf(
        keyHint("↑↓", "move"),
        keyHint("Space", "toggle"),
        keyHint("Enter", "confirm"),
        keyHint("a", "all"),
        keyHint("n", "none")
    ).joinToString("   ")

private val RADIO_LEGEND =
    "  " + listOf(
        keyHint("↑↓", "move"),
        keyHint("Enter", "confirm")
    ).joinToString("   ")

// ─── Data ────────────────────────────────────────────────────────────────────
data class SelectOption<T>(val value: T, val label: String, val description: String = "")

// ─── Section header ───────────────────────────────────────────────────────────
fun sectionHeader(title: String, hint: String = "") {
    val width = 52
    val left  = "  \u001B[2m──\u001B[0m \u001B[1m$title\u001B[0m "
    val hintPart = if (hint.isNotEmpty()) dim(hint) else ""
    val dashes = dim("─".repeat(maxOf(2, width - title.length - hint.length - 6)))
    println("\n$left$dashes $hintPart")
}

// ─── Arrow-key interactive checkbox select ────────────────────────────────────
/**
 * Full interactive checkbox list driven by arrow keys:
 *   ↑ / ↓  — move cursor
 *   Space   — toggle current item
 *   Enter   — confirm selection
 *   a       — select all
 *   n       — deselect all
 *
 * Falls back to compact numbered input when stdin is not a TTY (piped/CI).
 */
fun <T> interactiveCheckboxSelect(
    options: List<SelectOption<T>>,
    initial: Set<T> = emptySet()
): Set<T> {
    // Non-TTY fallback (piped input, CI)
    if (System.console() == null) {
        return compactMultiSelect(options, initial)
    }

    val selected = options.map { it.value in initial }.toBooleanArray()
    var cursor = 0

    val terminal = TerminalBuilder.builder()
        .system(true)
        .dumb(false)
        .build()

    val savedAttrs = terminal.enterRawMode()
    val reader = terminal.reader()

    try {
        // Hide cursor
        print("\u001B[?25l")
        System.out.flush()

        fun renderAll() {
            options.forEachIndexed { i, opt ->
                val isCursor = (i == cursor)
                val isOn     = selected[i]
                val label    = when {
                    isCursor && isOn -> bgCyan(" [✓] ${bold(opt.label)} ")
                    isCursor         -> bgCyan(" [ ] ${opt.label} ")
                    isOn             -> "  [${green("✓")}] ${bold(opt.label)}"
                    else             -> "  [ ] ${opt.label}"
                }
                val desc = if (!isCursor && opt.description.isNotEmpty()) dim("  ${opt.description}") else ""
                println("\r$label$desc\u001B[K")
            }
            println("\r$CHECKBOX_LEGEND\u001B[K")
        }

        fun moveUp(n: Int) {
            // Move cursor up n+1 lines (options + hint line)
            print("\u001B[${n + 1}A\r")
            System.out.flush()
        }

        renderAll()

        while (true) {
            val ch = reader.read()

            when (ch) {
                // Enter
                13, 10 -> break

                // Space — toggle current
                32 -> selected[cursor] = !selected[cursor]

                // 'a' — select all
                97 -> selected.fill(true)

                // 'n' — deselect all
                110 -> selected.fill(false)

                // ESC sequence (arrow keys)
                27 -> {
                    val next = reader.read()
                    if (next == 91) { // '['
                        when (reader.read()) {
                            65 -> cursor = (cursor - 1 + options.size) % options.size // ↑
                            66 -> cursor = (cursor + 1) % options.size                 // ↓
                        }
                    }
                }
            }

            moveUp(options.size)
            renderAll()
        }

    } finally {
        // Restore terminal state and show cursor
        terminal.setAttributes(savedAttrs)
        print("\u001B[?25h")
        System.out.flush()
        terminal.close()
    }

    return options.indices.filter { selected[it] }.map { options[it].value }.toSet()
}

// ─── Interactive single-select ────────────────────────────────────────────────
/**
 * Arrow-key radio-button style single select.
 * Falls back to inlineChoice when not in a TTY.
 */
fun <T> interactiveSingleSelect(
    options: List<SelectOption<T>>,
    initial: T
): T {
    if (System.console() == null) {
        val labels = options.map { it.label.lowercase() }
        val defaultLabel = options.first { it.value == initial }.label.lowercase()
        val choice = inlineChoice("Select", labels, defaultLabel)
        return options.first { it.label.lowercase() == choice }.value
    }

    var cursor = options.indexOfFirst { it.value == initial }.coerceAtLeast(0)

    val terminal = TerminalBuilder.builder()
        .system(true)
        .dumb(false)
        .build()

    val savedAttrs = terminal.enterRawMode()
    val reader = terminal.reader()

    try {
        print("\u001B[?25l")
        System.out.flush()

        fun renderAll() {
            options.forEachIndexed { i, opt ->
                val isCursor = (i == cursor)
                val bullet   = if (isCursor) cyan("◉") else dim("○")
                val label = if (isCursor) bgCyan(" $bullet ${bold(opt.label)} ") else "  $bullet ${opt.label}"
                val desc  = if (!isCursor && opt.description.isNotEmpty()) dim("  ${opt.description}") else ""
                println("\r$label$desc\u001B[K")
            }
            println("\r$RADIO_LEGEND\u001B[K")
        }

        fun moveUp(n: Int) {
            print("\u001B[${n + 1}A\r")
            System.out.flush()
        }

        renderAll()

        while (true) {
            when (val ch = reader.read()) {
                13, 10 -> break
                27 -> {
                    val next = reader.read()
                    if (next == 91) {
                        when (reader.read()) {
                            65 -> cursor = (cursor - 1 + options.size) % options.size
                            66 -> cursor = (cursor + 1) % options.size
                        }
                    }
                }
            }
            moveUp(options.size)
            renderAll()
        }
    } finally {
        terminal.setAttributes(savedAttrs)
        print("\u001B[?25h")
        System.out.flush()
        terminal.close()
    }

    return options[cursor].value
}

// ─── Compact multi-select fallback (numbered table + toggle input) ─────────────────────
/**
 * Non-interactive fallback: numbered table, user types numbers to toggle.
 * Used automatically when stdin is not a TTY.
 */
fun <T> compactMultiSelect(
    options: List<SelectOption<T>>,
    initial: Set<T> = emptySet()
): Set<T> {
    val selected = options.map { it.value in initial }.toBooleanArray()

    fun renderTable() {
        options.forEachIndexed { i, opt ->
            val check = if (selected[i]) green("✓") else dim("·")
            val num   = (i + 1).toString().padStart(2)
            val label = if (selected[i]) bold(opt.label) else opt.label
            val desc  = if (opt.description.isNotEmpty()) dim("  ${opt.description}") else ""
            println("  $num  [$check] $label$desc")
        }
    }

    renderTable()
    print("  ${cyan("›")} Toggle numbers (e.g. ${dim("1 3")}) or ${dim("Enter")} to keep: ")
    System.out.flush()

    val input = readlnOrNull()?.trim() ?: ""

    when {
        input.isBlank() -> { /* keep initial */ }
        input.lowercase() == "all"  -> selected.fill(true)
        input.lowercase() == "none" -> selected.fill(false)
        else -> {
            val indices = input.split(Regex("[,\\s]+"))
                .mapNotNull { it.trim().toIntOrNull()?.minus(1) }
                .filter { it in selected.indices }
            indices.forEach { selected[it] = !selected[it] }
            println()
            renderTable()
            println()
        }
    }

    return options.indices.filter { selected[it] }.map { options[it].value }.toSet()
}

// ─── Compact permission selector (inline row) ─────────────────────────────────
fun <T> inlineMultiSelect(
    options: List<SelectOption<T>>,
    initial: Set<T> = emptySet()
): Set<T> {
    if (System.console() != null) {
        return interactiveCheckboxSelect(options, initial)
    }

    val selected = options.map { it.value in initial }.toBooleanArray()

    fun renderRow() {
        val parts = options.mapIndexed { i, opt ->
            val check = if (selected[i]) green("✓") else dim("·")
            "  ${i + 1} [$check] ${if (selected[i]) bold(opt.label) else opt.label}"
        }
        println(parts.joinToString("   "))
    }

    renderRow()
    print("  ${cyan("›")} Toggle or ${dim("Enter")} for all: ")
    System.out.flush()

    val input = readlnOrNull()?.trim() ?: ""

    when {
        input.isBlank() -> { /* keep initial */ }
        input.lowercase() == "all"  -> selected.fill(true)
        input.lowercase() == "none" -> selected.fill(false)
        else -> {
            val indices = input.split(Regex("[,\\s]+"))
                .mapNotNull { it.trim().toIntOrNull()?.minus(1) }
                .filter { it in selected.indices }
            indices.forEach { selected[it] = !selected[it] }
            println()
            renderRow()
            println()
        }
    }

    return options.indices.filter { selected[it] }.map { options[it].value }.toSet()
}

// ─── Inline confirm (Y/n on same line) ────────────────────────────────────────
fun confirm(prompt: String, default: Boolean = true): Boolean {
    val hint = if (default) dim("Y/n") else dim("y/N")
    print("  ${cyan("›")} $prompt ($hint): ")
    System.out.flush()
    return when (readlnOrNull()?.trim()?.lowercase()) {
        "y", "yes" -> true
        "n", "no"  -> false
        else       -> default
    }
}

// ─── Inline single-select by keyword ─────────────────────────────────────────
fun inlineChoice(prompt: String, choices: List<String>, default: String): String {
    val opts = choices.joinToString("/")
    print("  ${cyan("›")} $prompt ($opts) ${dim("[$default]")}: ")
    System.out.flush()
    val input = readlnOrNull()?.trim()?.lowercase() ?: ""
    return if (input in choices) input else default
}
