/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2025 Hai Nguyen
 */
package io.askimo.desktop.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.askimo.core.directive.ChatDirective
import io.askimo.core.directive.ChatDirectiveRepository
import io.askimo.core.directive.ChatDirectiveService
import io.askimo.desktop.model.ChatMessage
import io.askimo.desktop.model.FileAttachment
import io.askimo.desktop.ui.components.manageDirectivesDialog
import io.askimo.desktop.ui.components.messageList
import io.askimo.desktop.ui.components.newDirectiveDialog
import io.askimo.desktop.ui.theme.ComponentColors
import java.awt.FileDialog
import java.awt.Frame
import java.time.LocalDateTime

// Helper function for file size formatting
private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

// File attachment item composable
@Composable
private fun fileAttachmentItem(
    attachment: FileAttachment,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = ComponentColors.surfaceVariantCardColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatFileSize(attachment.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(24.dp)
                    .pointerHoverIcon(PointerIcon.Hand),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun chatView(
    messages: List<ChatMessage>,
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    onSendMessage: (String, List<FileAttachment>) -> Unit,
    onStopResponse: () -> Unit = {},
    isLoading: Boolean = false,
    isThinking: Boolean = false,
    thinkingElapsedSeconds: Int = 0,
    spinnerFrame: Char = '⠋',
    errorMessage: String? = null,
    attachments: List<FileAttachment> = emptyList(),
    onAttachmentsChange: (List<FileAttachment>) -> Unit = {},
    provider: String? = null,
    model: String? = null,
    onNavigateToSettings: () -> Unit = {},
    hasMoreMessages: Boolean = false,
    isLoadingPrevious: Boolean = false,
    onLoadPrevious: () -> Unit = {},
    isSearchMode: Boolean = false,
    searchQuery: String = "",
    searchResults: List<ChatMessage> = emptyList(),
    isSearching: Boolean = false,
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onJumpToMessage: (String, LocalDateTime) -> Unit = { _, _ -> },
    selectedDirective: String? = null,
    onDirectiveSelected: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Initialize directive service
    val directiveService = remember {
        ChatDirectiveService(ChatDirectiveRepository())
    }

    // Load all directives
    var availableDirectives by remember { mutableStateOf<List<ChatDirective>>(emptyList()) }
    var showNewDirectiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableDirectives = directiveService.listAllDirectives()
    }

    // Show new directive dialog
    if (showNewDirectiveDialog) {
        newDirectiveDialog(
            onDismiss = { showNewDirectiveDialog = false },
            onConfirm = { name, content, applyToCurrent ->
                // Create the new directive
                val newDirective = directiveService.createDirective(name, content)

                // Reload directives
                availableDirectives = directiveService.listAllDirectives()

                // Apply to current session if requested
                if (applyToCurrent) {
                    onDirectiveSelected(newDirective.id)
                }

                showNewDirectiveDialog = false
            },
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Configuration info header
        if (provider != null && model != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ComponentColors.sidebarSurfaceColor(),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left side: Provider, Model, and Change button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    shadowElevation = 4.dp,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        text = "Provider: $provider\nModel: $model",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                }
                            },
                        ) {
                            Text(
                                text = "$provider | $model",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        TextButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            colors = ComponentColors.primaryTextButtonColors(),
                        ) {
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    // Right side: Directive selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Directive:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        var showManageDirectivesDialog by remember { mutableStateOf(false) }
                        var directiveDropdownExpanded by remember { mutableStateOf(false) }

                        // Get the selected directive details
                        val selectedDirectiveObj = remember(selectedDirective, availableDirectives) {
                            selectedDirective?.let { id ->
                                availableDirectives.find { it.id == id }
                            }
                        }

                        Box {
                            TooltipArea(
                                tooltip = {
                                    if (selectedDirectiveObj != null) {
                                        Surface(
                                            modifier = Modifier.width(400.dp),
                                            shadowElevation = 4.dp,
                                            shape = MaterialTheme.shapes.small,
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                            ) {
                                                Text(
                                                    text = selectedDirectiveObj.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Text(
                                                    text = selectedDirectiveObj.content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(top = 8.dp),
                                                )
                                            }
                                        }
                                    }
                                },
                            ) {
                                TextButton(
                                    onClick = { directiveDropdownExpanded = true },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                    colors = ComponentColors.primaryTextButtonColors(),
                                ) {
                                    Text(
                                        text = selectedDirectiveObj?.name?.take(30)?.let {
                                            if (selectedDirectiveObj.name.length > 30) "$it..." else it
                                        } ?: "None",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select directive",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            ComponentColors.themedDropdownMenu(
                                expanded = directiveDropdownExpanded,
                                onDismissRequest = { directiveDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.3f),
                            ) {
                                // "None" option to clear directive
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "None",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    },
                                    onClick = {
                                        onDirectiveSelected(null)
                                        directiveDropdownExpanded = false
                                    },
                                    leadingIcon = if (selectedDirective == null) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                )

                                // Show available directives
                                if (availableDirectives.isNotEmpty()) {
                                    HorizontalDivider()

                                    availableDirectives.forEach { directive ->
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    modifier = Modifier.width(400.dp),
                                                    shadowElevation = 4.dp,
                                                    shape = MaterialTheme.shapes.small,
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                    ) {
                                                        Text(
                                                            text = directive.name,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                        )
                                                        Text(
                                                            text = directive.content,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.padding(top = 8.dp),
                                                        )
                                                    }
                                                }
                                            },
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = directive.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                    )
                                                },
                                                onClick = {
                                                    onDirectiveSelected(directive.id)
                                                    directiveDropdownExpanded = false
                                                },
                                                leadingIcon = if (selectedDirective == directive.id) {
                                                    {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                } else {
                                                    null
                                                },
                                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                            )
                                        }
                                    }
                                }

                                // Action items section
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )

                                // New Directive action
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "New directive",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Text(
                                                text = "New Directive",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                    onClick = {
                                        showNewDirectiveDialog = true
                                        directiveDropdownExpanded = false
                                    },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                )

                                // Manage Directives action
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Manage directives",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Text(
                                                text = "Manage Directives",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                    onClick = {
                                        showManageDirectivesDialog = true
                                        directiveDropdownExpanded = false
                                    },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                )
                            }

                            // Show manage directives dialog
                            if (showManageDirectivesDialog) {
                                manageDirectivesDialog(
                                    directives = availableDirectives,
                                    onDismiss = { showManageDirectivesDialog = false },
                                    onUpdate = { id, newName, newContent ->
                                        directiveService.updateDirective(id, newName, newContent)
                                        availableDirectives = directiveService.listAllDirectives()
                                    },
                                    onDelete = { id ->
                                        directiveService.deleteDirective(id)
                                        if (selectedDirective == id) {
                                            onDirectiveSelected(null)
                                        }
                                        availableDirectives = directiveService.listAllDirectives()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search in conversation...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                singleLine = true,
                colors = ComponentColors.outlinedTextFieldColors(),
            )
        }

        // Search mode indicator
        if (isSearchMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = ComponentColors.bannerCardColors(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isSearching) {
                            "Searching..."
                        } else {
                            "Found ${searchResults.size} result(s)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            when {
                isSearchMode && searchResults.isEmpty() && !isSearching -> {
                    Text(
                        "No messages found matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                isSearchMode -> {
                    messageList(
                        messages = searchResults,
                        isThinking = false,
                        thinkingElapsedSeconds = 0,
                        spinnerFrame = spinnerFrame,
                        hasMoreMessages = false,
                        isLoadingPrevious = false,
                        onLoadPrevious = {},
                        searchQuery = searchQuery,
                        onMessageClick = onJumpToMessage,
                    )
                }
                messages.isEmpty() -> {
                    Text(
                        "Welcome to Askimo!\nStart a conversation by typing a message below.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    messageList(
                        messages = messages,
                        isThinking = isThinking,
                        thinkingElapsedSeconds = thinkingElapsedSeconds,
                        spinnerFrame = spinnerFrame,
                        hasMoreMessages = hasMoreMessages,
                        isLoadingPrevious = isLoadingPrevious,
                        onLoadPrevious = onLoadPrevious,
                    )
                }
            }
        }

        HorizontalDivider()

        // Input area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // File attachments display
            if (attachments.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    attachments.forEach { attachment ->
                        fileAttachmentItem(
                            attachment = attachment,
                            onRemove = {
                                onAttachmentsChange(attachments - attachment)
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attach file button
                IconButton(
                    onClick = {
                        // Open file picker
                        val fileChooser =
                            FileDialog(null as Frame?, "Select File", FileDialog.LOAD)
                        fileChooser.isVisible = true
                        val selectedFile = fileChooser.file
                        val selectedDir = fileChooser.directory
                        if (selectedFile != null && selectedDir != null) {
                            val file = java.io.File(selectedDir, selectedFile)
                            try {
                                val content = file.readText()
                                val attachment = FileAttachment(
                                    fileName = file.name,
                                    content = content,
                                    mimeType = file.extension,
                                    size = file.length(),
                                )
                                onAttachmentsChange(attachments + attachment)
                            } catch (e: Exception) {
                                // Handle error - file too large or unreadable
                                println("Error reading file: ${e.message}")
                            }
                        }
                    },
                    colors = ComponentColors.primaryIconButtonColors(),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            // Handle Enter key to send message (without Shift)
                            // Shift+Enter adds a new line
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.isShiftPressed) {
                                    // Shift+Enter: insert new line at cursor position
                                    val cursorPosition = inputText.selection.start
                                    val textBeforeCursor = inputText.text.substring(0, cursorPosition)
                                    val textAfterCursor = inputText.text.substring(cursorPosition)
                                    val newText = textBeforeCursor + "\n" + textAfterCursor
                                    val newCursorPosition = cursorPosition + 1
                                    onInputTextChange(
                                        TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newCursorPosition),
                                        ),
                                    )
                                    true // consume the event
                                } else {
                                    // Enter without Shift: send message
                                    if (inputText.text.isNotBlank() && !isLoading) {
                                        onSendMessage(inputText.text, attachments)
                                    }
                                    true // consume the event
                                }
                            } else {
                                false // don't consume other key events
                            }
                        },
                    placeholder = { Text("Type your message... (Enter to send, Shift+Enter for new line)") },
                    maxLines = 5,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    colors = ComponentColors.outlinedTextFieldColors(),
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Show stop button when loading, send button otherwise
                if (isLoading) {
                    IconButton(
                        onClick = onStopResponse,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.text.isNotBlank()) {
                                onSendMessage(inputText.text, attachments)
                            }
                        },
                        enabled = inputText.text.isNotBlank(),
                        colors = ComponentColors.primaryIconButtonColors(),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                        )
                    }
                }
            }
        }
    }
}
