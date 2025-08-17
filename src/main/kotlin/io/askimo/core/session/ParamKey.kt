package io.askimo.core.session

import io.askimo.core.providers.ModelProvider
import io.askimo.core.providers.ProviderSettings
import io.askimo.core.providers.Style
import io.askimo.core.providers.Verbosity
import io.askimo.core.providers.ollama.OllamaSettings
import io.askimo.core.providers.openai.OpenAiSettings

/**
 * Defines all configurable parameters for the chat application.
 *
 * This enum represents the available configuration parameters that can be set
 * in the application. Each parameter has metadata like key name, type, description,
 * provider association, getter/setter functions, and optional suggestions.
 *
 * Parameters are grouped into shared parameters (applicable to all providers)
 * and provider-specific parameters (OpenAI and Ollama).
 *
 * @property key The string key used to identify this parameter in commands and configuration
 * @property type The data type of this parameter (String, Boolean, Double, Int)
 * @property description A human-readable description of the parameter
 * @property provider The model provider this parameter is associated with, or null if shared
 * @property getValue Function to retrieve the parameter value from SessionParams
 * @property setValue Function to set the parameter value in SessionParams
 * @property suggestions Optional list of suggested values for this parameter
 */
enum class ParamKey(
    val key: String,
    val type: String,
    val description: String,
    val provider: ModelProvider?,
    val getValue: (SessionParams, ProviderSettings?) -> Any?,
    val setValue: (SessionParams, ProviderSettings?, String) -> Unit,
    val suggestions: List<String> = emptyList(),
) {
    MODEL_NAME(
        key = "model",
        type = "String",
        description = "Model name to use (e.g., gpt-4, llama3)",
        provider = null,
        getValue = { params, _ -> params.model },
        setValue = { params, _, v -> params.model = v },
    ),

    STYLE(
        key = "style",
        type = "Enum(precise|balanced|creative)",
        description = "Output style (determinism vs. creativity)",
        provider = null,
        getValue = { _, settings ->
            settings
                ?.presets
                ?.style
                ?.name
                ?.lowercase() ?: "balanced"
        },
        setValue = { _, settings, v ->
            val ps = settings ?: throw IllegalArgumentException("Missing provider settings")
            val style =
                when (v.trim().lowercase()) {
                    "precise" -> Style.PRECISE
                    "balanced" -> Style.BALANCED
                    "creative" -> Style.CREATIVE
                    else -> throw IllegalArgumentException("Use: precise | balanced | creative")
                }
            ps.presets = ps.presets.copy(style = style)
        },
        suggestions = listOf("precise", "balanced", "creative"),
    ),

    VERBOSITY(
        key = "verbosity",
        type = "Enum(short|normal|long)",
        description = "Controls response length/cost",
        provider = null,
        getValue = { _, settings ->
            settings
                ?.presets
                ?.verbosity
                ?.name
                ?.lowercase() ?: "normal"
        },
        setValue = { _, settings, v ->
            val ps = settings ?: throw IllegalArgumentException("Missing provider settings")
            val vb =
                when (v.trim().lowercase()) {
                    "short" -> Verbosity.SHORT
                    "normal" -> Verbosity.NORMAL
                    "long" -> Verbosity.LONG
                    else -> throw IllegalArgumentException("Use: short | normal | long")
                }
            ps.presets = ps.presets.copy(verbosity = vb)
        },
        suggestions = listOf("short", "normal", "long"),
    ),

    OPENAI_API_KEY(
        key = "api_key",
        type = "String",
        description = "OpenAI API key",
        provider = ModelProvider.OPEN_AI,
        getValue = { _, settings -> (settings as? OpenAiSettings)?.apiKey ?: "" },
        setValue = { _, settings, v -> (settings as? OpenAiSettings)?.apiKey = v },
    ),

    OLLAMA_BASE_URL(
        key = "base_url",
        type = "String",
        description = "Ollama server URL",
        provider = ModelProvider.OLLAMA,
        getValue = { _, settings -> (settings as? OllamaSettings)?.baseUrl },
        setValue = { _, settings, v -> (settings as? OllamaSettings)?.baseUrl = v },
    ),
    ;

    companion object {
        fun all(): List<ParamKey> = entries

        fun fromInput(key: String): ParamKey? = entries.find { it.key.equals(key, ignoreCase = true) }

        fun forProvider(provider: ModelProvider): List<ParamKey> = entries.filter { it.provider == null || it.provider == provider }
    }

    fun applyTo(
        params: SessionParams,
        providerSettings: ProviderSettings,
        value: String,
    ) {
        try {
            setValue(params, providerSettings, value)
        } catch (e: Exception) {
            throw IllegalArgumentException("❌ Failed to set '$key': expected type $type. ${e.message}")
        }
    }
}
