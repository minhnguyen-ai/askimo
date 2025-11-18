# Chat Directives

Chat Directives allow users to define custom instructions for AI behavior in chat sessions. Users can specify tone, format, style, and other behavioral guidelines that will be consistently applied throughout a conversation.

## Domain Model

### ChatDirective
```kotlin
data class ChatDirective(
    val name: String,              // Unique identifier (e.g., "concise-code", "explain-like-im-5")
    val content: String,           // The actual directive text
    val isDefault: Boolean = false, // Auto-apply to new sessions
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

## Repository Operations

The `ChatDirectiveRepository` provides the following operations:

### Basic CRUD
- `save(directive: ChatDirective): ChatDirective` - Create or update a directive
- `get(name: String): ChatDirective?` - Get a directive by name
- `update(directive: ChatDirective): Boolean` - Update existing directive
- `delete(name: String): Boolean` - Delete a directive
- `exists(name: String): Boolean` - Check if directive exists

### Listing
- `list(): List<ChatDirective>` - Get all directives
- `listDefaults(): List<ChatDirective>` - Get only default directives
- `getByNames(names: List<String>): List<ChatDirective>` - Get multiple by names

## Service Layer

The `ChatDirectiveService` provides higher-level operations:

### Build System Prompt
```kotlin
val service = ChatDirectiveService(repository)

// Build prompt with specific directives
val prompt = service.buildSystemPrompt(
    directiveNames = listOf("concise-code", "markdown-format"),
    includeDefaults = true
)
```

### Manage Directives
```kotlin
// Create
service.createDirective(
    name = "concise-code",
    content = "Always provide concise code examples without verbose explanations.",
    isDefault = false
)

// Update
service.updateDirective(
    name = "concise-code",
    content = "Provide minimal, concise code with brief inline comments.",
    isDefault = true
)

// Delete
service.deleteDirective("concise-code")

// Query
val directive = service.getDirective("concise-code")
val all = service.listAllDirectives()
val defaults = service.listDefaultDirectives()
```

## Storage

Directives are stored in a SQLite database at `~/.askimo/chat_directives.db` with the following schema:

```sql
CREATE TABLE chat_directives (
    name TEXT PRIMARY KEY,
    content TEXT NOT NULL,
    is_default INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
)
```

## Usage Example

```kotlin
// Initialize
val repository = ChatDirectiveRepository()
val service = ChatDirectiveService(repository)

// Create some directives
service.createDirective(
    name = "explain-like-im-5",
    content = "Explain concepts in very simple terms, as if talking to a 5-year-old.",
    isDefault = false
)

service.createDirective(
    name = "markdown-format",
    content = "Format all responses using proper Markdown syntax with headers, lists, and code blocks.",
    isDefault = true
)

// Start a chat session with specific directives
val systemPrompt = service.buildSystemPrompt(
    directiveNames = listOf("explain-like-im-5", "markdown-format"),
    includeDefaults = true
)

// Use systemPrompt as the first system message in your chat
```

## Integration with Chat Sessions

When starting a new chat session, you can:

1. Get default directives automatically
2. Allow user to select additional directives
3. Build the system prompt using selected directives
4. Prepend the system prompt to the chat context

This ensures consistent AI behavior throughout the entire conversation.

