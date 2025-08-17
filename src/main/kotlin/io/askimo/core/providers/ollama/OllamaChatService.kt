package io.askimo.core.providers.ollama

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import io.askimo.core.providers.ChatService
import io.askimo.core.providers.ModelProvider
import io.askimo.core.providers.samplingFor
import io.askimo.core.providers.tokensFor
import java.util.concurrent.CountDownLatch

class OllamaChatService(
    private val modelName: String,
    private val settings: OllamaSettings,
    private val memory: ChatMemory,
    private val systemPrompt: String?,
) : ChatService {
    override val id: String = modelName
    override val provider: ModelProvider = ModelProvider.OLLAMA

    private val chatModel: OllamaStreamingChatModel by lazy {
        val b =
            OllamaStreamingChatModel
                .builder()
                .baseUrl(settings.baseUrl)
                .modelName(modelName)

        val s = samplingFor(settings.presets.style)
        b.temperature(s.temperature).topP(s.topP)

        val cap = tokensFor(settings.presets.verbosity)
        b.numPredict(cap)

        b.build()
    }

    override fun chat(
        prompt: String,
        onToken: (String) -> Unit,
    ): String {
        val msgs = buildMessageList(prompt)
        val sb = StringBuilder()
        val done = CountDownLatch(1)

        val handler =
            object : StreamingChatResponseHandler {
                override fun onPartialResponse(partial: String) {
                    sb.append(partial)
                    onToken(partial)
                }

                override fun onCompleteResponse(response: ChatResponse) {
                    memory.add(UserMessage(prompt))
                    memory.add(response.aiMessage()) // final AiMessage
                    done.countDown()
                }

                override fun onError(error: Throwable) {
                    onToken("\n[error] ${error.message ?: "unknown error"}\n")
                    done.countDown()
                }
            }

        chatModel.chat(msgs, handler)
        done.await()
        return sb.toString()
    }

    private fun buildMessageList(userText: String): List<ChatMessage> {
        val existing = memory.messages()
        val list = mutableListOf<ChatMessage>()
        if (!systemPrompt.isNullOrBlank()) {
            list += SystemMessage(systemPrompt)
        }
        list += existing
        list += UserMessage(userText)
        return list
    }
}
