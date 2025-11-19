/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.desktop.model

import androidx.compose.ui.input.key.Key

/**
 * Represents a keyboard shortcut configuration.
 * Designed to be easily serializable for future user customization.
 */
data class KeyboardShortcut(
    val action: ShortcutAction,
    val key: Key,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false,
) {
    /**
     * Returns the display string for this shortcut.
     * Automatically uses Command symbol (⌘) for meta key on macOS.
     */
    fun getDisplayString(isMac: Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)): String {
        val modifiers = mutableListOf<String>()

        if (isMac) {
            if (meta) modifiers.add("⌘")
            if (shift) modifiers.add("⇧")
            if (alt) modifiers.add("⌥")
            if (ctrl) modifiers.add("⌃")
        } else {
            if (ctrl) modifiers.add("Ctrl")
            if (shift) modifiers.add("Shift")
            if (alt) modifiers.add("Alt")
            if (meta) modifiers.add("Win")
        }

        val keyName = when (key) {
            Key.Enter -> "Enter"
            Key.Escape -> "Escape"
            Key.F3 -> "F3"
            Key.Comma -> ","
            else -> key.keyCode.toChar().toString()
        }

        return (modifiers + keyName).joinToString("+")
    }
}

/**
 * Enum representing all available keyboard shortcut actions.
 */
enum class ShortcutAction(val description: String) {
    // Chat Actions
    NEW_CHAT("New Chat"),
    SEARCH_IN_CHAT("Search in Chat"),
    SEND_MESSAGE("Send Message"),
    NEW_LINE("New Line in Message"),
    ATTACH_FILE("Attach File"),
    STOP_AI_RESPONSE("Stop AI Response"),

    // Navigation
    OPEN_SETTINGS("Open Settings"),
    TOGGLE_CHAT_HISTORY("Toggle Chat History"),
    EXIT_SEARCH("Exit Search"),

    // Search Navigation
    NEXT_SEARCH_RESULT("Next Search Result"),
    PREV_SEARCH_RESULT("Previous Search Result"),

    // General
    QUIT_APP("Quit Application"),
}

/**
 * Default keyboard shortcuts configuration.
 * Can be overridden by user preferences in the future.
 */
object KeyboardShortcuts {
    private val isMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)

    // Define default shortcuts
    val defaults = listOf(
        // Chat Actions
        KeyboardShortcut(ShortcutAction.NEW_CHAT, Key.N, ctrl = !isMac, meta = isMac),
        KeyboardShortcut(ShortcutAction.SEARCH_IN_CHAT, Key.F, ctrl = !isMac, meta = isMac),
        KeyboardShortcut(ShortcutAction.SEND_MESSAGE, Key.Enter),
        KeyboardShortcut(ShortcutAction.NEW_LINE, Key.Enter, shift = true),
        KeyboardShortcut(ShortcutAction.ATTACH_FILE, Key.A, ctrl = !isMac, meta = isMac),
        KeyboardShortcut(ShortcutAction.STOP_AI_RESPONSE, Key.S, ctrl = !isMac, meta = isMac),

        // Navigation
        KeyboardShortcut(ShortcutAction.OPEN_SETTINGS, Key.Comma, ctrl = !isMac, meta = isMac),
        KeyboardShortcut(ShortcutAction.TOGGLE_CHAT_HISTORY, Key.H, ctrl = !isMac, meta = isMac),
        KeyboardShortcut(ShortcutAction.EXIT_SEARCH, Key.Escape),

        // Search Navigation
        KeyboardShortcut(ShortcutAction.NEXT_SEARCH_RESULT, Key.F3),
        KeyboardShortcut(ShortcutAction.PREV_SEARCH_RESULT, Key.F3, shift = true),

        // General
        KeyboardShortcut(ShortcutAction.QUIT_APP, Key.Q, ctrl = !isMac, meta = isMac),
    )

    // Map for quick lookup
    private val shortcutsMap = defaults.associateBy { it.action }

    /**
     * Get the shortcut for a specific action.
     */
    fun getShortcut(action: ShortcutAction): KeyboardShortcut? = shortcutsMap[action]

    /**
     * Get all shortcuts grouped by category.
     */
    fun getShortcutsByCategory(): Map<String, List<Pair<String, String>>> {
        val modifierKey = if (isMac) "⌘" else "Ctrl"

        return mapOf(
            "Chat Actions" to listOf(
                "New Chat" to "$modifierKey+N",
                "Search in Chat" to "$modifierKey+F",
                "Send Message" to "Enter",
                "New Line in Message" to "Shift+Enter",
                "Attach File" to "$modifierKey+A",
            ),
            "Navigation" to listOf(
                "Open Settings" to "$modifierKey+,",
                "Toggle Chat History" to "$modifierKey+H",
            ),
            "Search Navigation" to listOf(
                "Next Search Result" to "F3",
                "Previous Search Result" to "Shift+F3",
                "Close Search" to "Escape",
            ),
            "General" to listOf(
                "Stop AI Response" to "$modifierKey+S",
                "Quit Application" to "$modifierKey+Q",
            ),
        )
    }
}
