/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.core.providers.localai

import io.askimo.core.providers.HasBaseUrl
import io.askimo.core.providers.Presets
import io.askimo.core.providers.ProviderSettings
import io.askimo.core.providers.Style
import io.askimo.core.providers.Verbosity
import kotlinx.serialization.Serializable

@Serializable
data class LocalAiSettings(
    override var baseUrl: String = "http://localhost:8080",
    override val defaultModel: String = "",
    override var presets: Presets = Presets(Style.BALANCED, Verbosity.NORMAL),
) : ProviderSettings,
    HasBaseUrl {
    override fun describe(): List<String> = listOf(
        "baseUrl:     $baseUrl",
        "presets: $presets",
    )
}
