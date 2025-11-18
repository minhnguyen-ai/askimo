/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.core.directive

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a custom instruction/directive that users can apply to chat sessions
 * to influence AI behavior (tone, format, style, etc.)
 */
data class ChatDirective(
    val id: String = UUID.randomUUID().toString(), // unique identifier
    val name: String, // user-friendly name (e.g., "Concise Code", "Explain Like I'm 5")
    val content: String, // raw directive text provided by user
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
