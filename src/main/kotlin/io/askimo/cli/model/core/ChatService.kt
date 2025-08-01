package io.askimo.cli.model.core

/**
 * Interface representing a chat-based language model.
 *
 * This interface abstracts different language model providers and implementations,
 * allowing for a unified way to interact with various LLM services such as OpenAI and Ollama.
 * Implementations of this interface handle the specific details of communicating with
 * their respective model providers.
 */
interface ChatService {
    /**
     * Unique identifier for the model, typically the model name.
     *
     * Examples: "gpt-4", "llama2", etc.
     */
    val id: String

    /**
     * The provider of this language model.
     *
     * Indicates which service or platform is providing the model implementation.
     * @see ModelProvider
     */
    val provider: ModelProvider

    /**
     * Sends a prompt to the language model and returns the complete response.
     *
     * This method handles the communication with the underlying language model,
     * sending the user's prompt and processing the model's response. It supports
     * streaming responses through the onToken callback.
     *
     * @param prompt The input text to send to the language model
     * @param onToken Callback function that is called for each token as it's received,
     *                allowing for streaming responses to the user
     * @return The complete response from the language model as a string
     */
    fun chat(
        prompt: String,
        onToken: (String) -> Unit,
    ): String
}
