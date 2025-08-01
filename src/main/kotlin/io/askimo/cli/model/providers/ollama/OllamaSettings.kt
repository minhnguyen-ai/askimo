package io.askimo.cli.model.providers.ollama

import io.askimo.cli.model.core.Presets
import io.askimo.cli.model.core.ProviderSettings
import io.askimo.cli.model.core.Style
import io.askimo.cli.model.core.Verbosity
import kotlinx.serialization.Serializable

@Serializable
data class OllamaSettings(
    var baseUrl: String = "http://localhost:11434",
    override var presets: Presets = Presets(Style.BALANCED, Verbosity.NORMAL),
) : ProviderSettings {
    override fun describe(): List<String> =
        listOf(
            "baseUrl:     $baseUrl",
            "presets: $presets",
        )
}
