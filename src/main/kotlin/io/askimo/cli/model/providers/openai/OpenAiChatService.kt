package io.askimo.cli.model.providers.openai

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import io.askimo.cli.model.core.ChatService
import io.askimo.cli.model.core.ModelProvider
import io.askimo.cli.model.core.presetsOrDefault
import io.askimo.cli.model.core.samplingFor
import io.askimo.cli.model.core.tokensFor
import java.util.concurrent.CountDownLatch
import kotlin.collections.plusAssign

class OpenAiChatService(
    private val modelName: String,
    private val settings: OpenAiSettings,
    private val memory: ChatMemory,
) : ChatService {
    override val id: String = modelName
    override val provider: ModelProvider = ModelProvider.OPEN_AI

    private val chatModel: OpenAiStreamingChatModel by lazy {
        val b =
            OpenAiStreamingChatModel
                .builder()
                .apiKey(settings.apiKey)
                .modelName(modelName)

        val cap = tokensFor(settings.presetsOrDefault().verbosity)
        if (usesCompletionCap(modelName)) b.maxCompletionTokens(cap) else b.maxTokens(cap)

        if (supportsSampling(modelName)) {
            val s = samplingFor(settings.presetsOrDefault().style)
            b.temperature(s.temperature).topP(s.topP)
        }
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
        list += existing
        list += UserMessage(userText)
        return list
    }

    private fun supportsSampling(model: String): Boolean {
        val m = model.lowercase()
        return !(m.startsWith("o") || m.startsWith("gpt-5") || m.contains("reasoning"))
    }

    private fun usesCompletionCap(name: String): Boolean {
        val m = name.lowercase()
        return (m.startsWith("o") || m.startsWith("gpt-5") || m.contains("reasoning"))
    }
}
