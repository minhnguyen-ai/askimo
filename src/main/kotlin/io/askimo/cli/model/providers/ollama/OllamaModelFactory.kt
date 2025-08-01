package io.askimo.cli.model.providers.ollama

import dev.langchain4j.memory.ChatMemory
import io.askimo.cli.model.core.ChatModelFactory
import io.askimo.cli.model.core.ChatService
import io.askimo.cli.model.core.ModelProvider
import io.askimo.cli.model.core.ProviderSettings

class OllamaModelFactory : ChatModelFactory {
    override val provider: ModelProvider = ModelProvider.OLLAMA

    override fun availableModels(settings: ProviderSettings): List<String> =
        try {
            val process =
                ProcessBuilder("ollama", "list")
                    .redirectErrorStream(true)
                    .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Parse lines like:
            // llama2 7B   4.3 GB
            // mistral 7B 4.1 GB
            output
                .lines()
                .drop(1) // skip header
                .mapNotNull { line ->
                    line.trim().split("\\s+".toRegex()).firstOrNull()
                }.filter { it.isNotBlank() }
                .distinct()
        } catch (e: Exception) {
            println("⚠️ Failed to fetch models from Ollama: ${e.message}")
            emptyList()
        }

    override fun defaultSettings(): ProviderSettings =
        OllamaSettings(
            baseUrl = "http://localhost:11434", // default Ollama endpoint
        )

    override fun create(
        model: String,
        settings: ProviderSettings,
        memory: ChatMemory,
    ): ChatService {
        require(settings is OllamaSettings) {
            "Invalid settings type for Ollama: ${settings::class.simpleName}"
        }

        return OllamaChatService(model, settings, memory, null)
    }
}
