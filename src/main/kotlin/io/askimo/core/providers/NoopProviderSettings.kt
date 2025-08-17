package io.askimo.core.providers

object NoopProviderSettings : ProviderSettings {
    override var presets: Presets = Presets(Style.BALANCED, Verbosity.NORMAL)

    override fun describe(): List<String> = listOf()
}
