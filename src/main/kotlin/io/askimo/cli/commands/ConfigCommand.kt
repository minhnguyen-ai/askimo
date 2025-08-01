package io.askimo.cli.commands

import io.askimo.cli.session.Session
import org.jline.reader.ParsedLine

/**
 * Handles the command to display the current configuration.
 *
 * This class provides a summary view of the active configuration, including the current
 * provider, model, and all configured settings. It helps users understand the current state
 * of their chat environment.
 */
class ConfigCommand(
    private val session: Session,
) : CommandHandler {
    override val keyword: String = ":config"
    override val description: String = "Show the current provider, model, and settings."

    override fun handle(line: ParsedLine) {
        val provider = session.getActiveProvider()
        val settings = session.getCurrentProviderSettings()

        println("🔧 Current configuration:")
        println("  Provider:    $provider")
        println("  Model:       ${if (session.hasChatService()) session.chatService.id else "(not set)"}")
        println("  Settings:")

        settings.describe().forEach {
            println("    $it")
        }
    }
}
