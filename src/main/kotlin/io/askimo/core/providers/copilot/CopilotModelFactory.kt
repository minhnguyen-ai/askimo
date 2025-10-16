/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.core.providers.copilot

import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.service.AiServices
import io.askimo.core.providers.ChatModelFactory
import io.askimo.core.providers.ChatService
import io.askimo.core.providers.ProviderSettings
import io.askimo.core.providers.verbosityInstruction
import io.askimo.core.util.SystemPrompts.systemMessage
import io.askimo.tools.fs.LocalFsTools

class CopilotModelFactory : ChatModelFactory {
    override fun availableModels(settings: ProviderSettings): List<String> {
        // GitHub Copilot CLI supports these models
        return listOf(
            "gpt-4",
            "gpt-4-turbo",
            "gpt-3.5-turbo",
        )
    }

    override fun defaultSettings(): ProviderSettings = CopilotSettings()

    override fun create(
        model: String,
        settings: ProviderSettings,
        memory: ChatMemory,
        retrievalAugmentor: RetrievalAugmentor?,
    ): ChatService {
        require(settings is CopilotSettings) {
            "Invalid settings type for Copilot: ${settings::class.simpleName}"
        }

        val chatModel = CopilotCliChatModel(settings, model)

        val builder =
            AiServices
                .builder(ChatService::class.java)
                .streamingChatModel(chatModel)
                .chatMemory(memory)
                .tools(LocalFsTools())
                .systemMessageProvider {
                    systemMessage(verbosityInstruction(settings.presets.verbosity))
                }

        if (retrievalAugmentor != null) {
            builder.retrievalAugmentor(retrievalAugmentor)
        }

        return builder.build()
    }
}
