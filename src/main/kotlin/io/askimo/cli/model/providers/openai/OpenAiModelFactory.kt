package io.askimo.cli.model.providers.openai

import dev.langchain4j.memory.ChatMemory
import io.askimo.cli.model.core.ChatModelFactory
import io.askimo.cli.model.core.ChatService
import io.askimo.cli.model.core.ModelProvider
import io.askimo.cli.model.core.ProviderSettings
import io.askimo.cli.util.json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI

class OpenAiModelFactory : ChatModelFactory {
    override val provider = ModelProvider.OPEN_AI

    override fun availableModels(settings: ProviderSettings): List<String> {
        val apiKey =
            (settings as? OpenAiSettings)?.apiKey?.takeIf { it.isNotBlank() }
                ?: return emptyList()

        return try {
            val url = URI("https://api.openai.com/v1/models").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")

            connection.inputStream.bufferedReader().use { reader ->
                val jsonElement = json.parseToJsonElement(reader.readText())

                val data = jsonElement.jsonObject["data"]?.jsonArray.orEmpty()

                data
                    .mapNotNull { element ->
                        element.jsonObject["id"]?.jsonPrimitive?.contentOrNull
                    }.distinct()
                    .sorted()
            }
        } catch (e: Exception) {
            println("⚠️ Failed to fetch models from OpenAI: ${e.message}")
            emptyList()
        }
    }

    override fun defaultModel(): String = "gpt-4o"

    override fun defaultSettings(): ProviderSettings = OpenAiSettings()

    override fun create(
        model: String,
        settings: ProviderSettings,
        memory: ChatMemory,
    ): ChatService {
        require(settings is OpenAiSettings) {
            "Invalid settings type for OpenAI: ${settings::class.simpleName}"
        }

        return OpenAiChatService(model, settings, memory)
    }
}
