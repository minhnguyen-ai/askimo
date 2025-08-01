package io.askimo.cli.util

import io.askimo.cli.model.core.ProviderSettings
import io.askimo.cli.model.providers.ollama.OllamaSettings
import io.askimo.cli.model.providers.openai.OpenAiSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val coreProvidersModule =
    SerializersModule {
        polymorphic(ProviderSettings::class) {
            subclass(OpenAiSettings::class, OpenAiSettings.serializer())
            subclass(OllamaSettings::class, OllamaSettings.serializer())
        }
    }

fun buildJson(vararg extraModules: SerializersModule): Json =
    Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "__type" // -> "ollama"/"openai" in saved JSON
        serializersModule =
            SerializersModule {
                include(coreProvidersModule)
                extraModules.forEach { include(it) }
            }
    }

val json = buildJson()
