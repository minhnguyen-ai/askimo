package io.askimo.cli.session

import io.askimo.cli.model.core.ModelProvider
import io.askimo.cli.model.core.ProviderSettings
import kotlinx.serialization.Serializable

@Serializable
data class SessionParams(
    /**
     * Maps each provider to its last-used model name.
     */
    var models: MutableMap<ModelProvider, String> = mutableMapOf(),
    /**
     * The currently active model provider.
     */
    var currentProvider: ModelProvider = ModelProvider.UNKNOWN,
    /**
     * Maps each provider to its settings (e.g., API key, temperature, base URL).
     */
    var providerSettings: MutableMap<ModelProvider, ProviderSettings> = mutableMapOf(),
) {
    companion object {
        fun noOp(): SessionParams = SessionParams()
    }

    /**
     * Current model for the active provider.
     */
    var model: String
        get() = models[currentProvider] ?: ""
        set(value) {
            models[currentProvider] = value
        }

    /**
     * Gets the last-used model for a given provider.
     */
    fun getModel(provider: ModelProvider): String = models[provider] ?: ""
}
