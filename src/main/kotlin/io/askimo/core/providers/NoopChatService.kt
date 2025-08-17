package io.askimo.core.providers

/**
 * A Noop implementation of the ChatModel interface.
 * This is used as a placeholder when no chat model is configured.
 * It provides a helpful message to guide users on how to set up a model.
 */
object NoopChatService : ChatService {
    override val id: String = "none"
    override val provider: ModelProvider = ModelProvider.UNKNOWN

    override fun chat(
        prompt: String,
        onToken: (String) -> Unit,
    ): String = "⚠️ No chat model is configured. Use ':setparam provider' and ':setparam model' to set up a model."
}
