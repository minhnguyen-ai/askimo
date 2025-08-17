package io.askimo.cli.commands

import io.askimo.core.session.ParamKey
import io.askimo.core.session.Session
import org.jline.reader.ParsedLine

/**
 * Handles the command to display model parameters.
 *
 * This class provides functionality to view the current model parameters or list all available
 * parameter keys for the active provider. It helps users understand what parameters can be
 * configured and their current values.
 */
class ParamsCommandHandler(
    private val session: Session,
) : CommandHandler {
    override val keyword = ":params"
    override val description = "Show current model parameters or list available param keys"

    override fun handle(line: ParsedLine) {
        if (line.words().contains("--list")) {
            listKeys()
        } else {
            showParams()
        }
    }

    private fun showParams() {
        val provider = session.getActiveProvider()
        val model = session.params.getModel(provider)

        println("Provider: ${provider.name.lowercase()}")
        println("Current model: $model")
        println()

        val paramKeys = ParamKey.forProvider(provider)
        if (paramKeys.isEmpty()) {
            println("No configurable parameters for this model.")
            return
        }

        println("Model parameters:")
        paramKeys.forEach { key ->
            println("  ${key.key} (${key.type}) – ${key.description}")
        }

        println()
        println("Use :setparam <key> <value> to change parameters.")
        println("Use :params --list to see available keys.")
    }

    private fun listKeys() {
        val provider = session.getActiveProvider()
        val model = session.params.getModel(provider)
        println("Available parameter keys for $model ($provider):")
        ParamKey.forProvider(provider).forEach {
            println("  ${it.key} (${it.type}) – ${it.description}")
        }
    }
}
