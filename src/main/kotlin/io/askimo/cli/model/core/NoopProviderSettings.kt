package io.askimo.cli.model.core

object NoopProviderSettings : ProviderSettings {
    override var presets: Presets = Presets(Style.BALANCED, Verbosity.NORMAL)

    override fun describe(): List<String> = listOf()
}
