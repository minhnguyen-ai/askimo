/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.core.providers.copilot

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.ModelProvider
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.listener.ChatModelListener
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ChatRequestParameters
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * A custom StreamingChatModel implementation that integrates with GitHub Copilot CLI.
 * This model executes shell commands to interact with the Copilot CLI rather than making HTTP requests.
 */
class CopilotCliChatModel(
    private val settings: CopilotSettings,
    private val model: String
) : StreamingChatModel {

    override fun defaultRequestParameters(): ChatRequestParameters {
        return DefaultChatRequestParameters.EMPTY
    }

    override fun doChat(chatRequest: ChatRequest, handler: StreamingChatResponseHandler) {
        val messages = chatRequest.messages()
        val prompt = messages.joinToString("\n") { message ->
            when (message) {
                is dev.langchain4j.data.message.UserMessage -> message.singleText()
                is dev.langchain4j.data.message.SystemMessage -> message.text()
                is dev.langchain4j.data.message.AiMessage -> message.text()
                is dev.langchain4j.data.message.ToolExecutionResultMessage -> message.text()
                else -> message.toString()
            }
        }

        // Execute Copilot CLI command asynchronously to support streaming
        CompletableFuture.supplyAsync {
            executeCopilotCommandWithStreaming(prompt, handler)
        }.exceptionally { throwable ->
            handler.onError(RuntimeException("Failed to execute Copilot CLI: ${throwable.message}", throwable))
            null
        }
    }

    private fun executeCopilotCommandWithStreaming(prompt: String, handler: StreamingChatResponseHandler): String {
        return try {
            val escapedPrompt = prompt.replace("\"", "\\\"")
            val command = listOf("sh", "-c", "${settings.copilotCommand} \"$escapedPrompt\"")
            val processBuilder = ProcessBuilder(command)
                .redirectErrorStream(true)

            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val responseBuilder = StringBuilder()

            // Read the output character by character to simulate streaming
            var char: Int
            val buffer = CharArray(1)
            var wordBuffer = StringBuilder()

            while (reader.read(buffer).also { char = it } != -1) {
                val currentChar = buffer[0]
                wordBuffer.append(currentChar)
                responseBuilder.append(currentChar)

                // Stream word by word or on whitespace/punctuation
                if (currentChar.isWhitespace() || currentChar in ".,!?;:") {
                    if (wordBuffer.isNotEmpty()) {
                        handler.onPartialResponse(wordBuffer.toString())
                        wordBuffer.clear()
                    }
                }

                // Small delay to simulate streaming effect
                Thread.sleep(10)
            }

            // Send any remaining content
            if (wordBuffer.isNotEmpty()) {
                handler.onPartialResponse(wordBuffer.toString())
            }

            val completed = process.waitFor(settings.timeout, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                throw RuntimeException("Copilot CLI command timed out after ${settings.timeout}ms")
            }

            val output = responseBuilder.toString().trim()
            if (process.exitValue() != 0) {
                throw RuntimeException("Copilot CLI command failed with exit code ${process.exitValue()}: $output")
            }

            val finalOutput = output.ifEmpty { "No response from Copilot CLI" }

            // Create and send the complete response
            val aiMessage = AiMessage.from(finalOutput)
            val chatResponse = ChatResponse.builder()
                .aiMessage(aiMessage)
                .build()

            handler.onCompleteResponse(chatResponse)
            finalOutput

        } catch (e: Exception) {
            throw RuntimeException("Failed to execute Copilot CLI: ${e.message}", e)
        }
    }

    override fun listeners(): List<ChatModelListener> {
        return emptyList()
    }

    override fun provider(): ModelProvider {
        return ModelProvider.OTHER // Since GitHub Copilot is not a standard provider
    }
}
