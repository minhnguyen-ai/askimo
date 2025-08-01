package io.askimo.cli.model.core

import io.askimo.cli.model.providers.ollama.OllamaModelFactory
import io.askimo.cli.model.providers.openai.OpenAiModelFactory

/**
 * Central registry for managing chat model factories across different AI providers.
 *
 * This singleton object maintains a registry of model factories for different providers
 * (like OpenAI, Ollama) and provides methods to:
 * - Register new model factories
 * - Retrieve factories for specific providers
 * - Get available models for a provider
 * - Create chat model instances with appropriate configuration
 *
 * The registry is initialized with default factories for known providers.
 */
object ModelRegistry {
    /**
     * Internal registry mapping model providers to their respective factory implementations.
     */
    private val factories = mutableMapOf<ModelProvider, ChatModelFactory>()

    init {
        register(OpenAiModelFactory())
        register(OllamaModelFactory())
    }

    /**
     * Registers a new chat model factory in the registry.
     *
     * If a factory for the same provider already exists, it will be replaced.
     *
     * @param factory The chat model factory to register
     */
    fun register(factory: ChatModelFactory) {
        factories[factory.provider] = factory
    }

    /**
     * Retrieves the chat model factory for the specified provider.
     *
     * @param provider The model provider to get the factory for
     * @return The chat model factory for the provider, or null if no factory is registered
     */
    fun getFactory(provider: ModelProvider): ChatModelFactory? = factories[provider]

    /**
     * Returns the set of model providers for which a factory is currently registered.
     *
     * This reflects the providers available at runtime, including those registered during
     * initialization and any added later via [register]. The returned set is a live view
     * backed by the internal registry; it will reflect subsequent registrations or removals.
     * If you need a stable snapshot, make a defensive copy using `toSet()`.
     *
     * @return set of [ModelProvider]s that this registry can build models for
     */
    fun getSupportedProviders(): Set<ModelProvider> = factories.keys
}
