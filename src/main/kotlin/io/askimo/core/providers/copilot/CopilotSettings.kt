/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.core.providers.copilot

import io.askimo.core.providers.HasApiKey
import io.askimo.core.providers.Presets
import io.askimo.core.providers.ProviderSettings
import io.askimo.core.providers.Style
import io.askimo.core.providers.Verbosity
import kotlinx.serialization.Serializable

@Serializable
data class CopilotSettings(
    override var apiKey: String = "copilot-cli", // Not used, but required by interface
    override val defaultModel: String = "gpt-4",
    override var presets: Presets = Presets(Style.BALANCED, Verbosity.NORMAL),
    val copilotCommand: String = "gh copilot suggest",
    val timeout: Long = 30000,
) : ProviderSettings,
    HasApiKey {
    override fun describe(): List<String> =
        listOf(
            "command: $copilotCommand",
            "model: $defaultModel",
            "presets: $presets",
            "timeout: ${timeout}ms",
        )
}
