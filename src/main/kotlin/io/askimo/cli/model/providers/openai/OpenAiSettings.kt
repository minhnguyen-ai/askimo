package io.askimo.cli.model.providers.openai

import io.askimo.cli.model.core.Presets
import io.askimo.cli.model.core.ProviderSettings
import io.askimo.cli.model.core.Style
import io.askimo.cli.model.core.Verbosity
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiSettings(
    var apiKey: String = "",
    override var presets: Presets = Presets(Style.BALANCED, Verbosity.NORMAL),
) : ProviderSettings {
    override fun describe(): List<String> =
        listOf(
            "apiKey:      ${apiKey.take(5)}***",
            "presets: $presets",
        )

    override fun toString(): String = "OpenAiSettings(apiKey=****, presets=$presets)"
}
