package io.askimo.web

import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

class WebServer(
    private val host: String = "127.0.0.1",
    private val port: Int = 8080,
) {
    private var server: EmbeddedServer<*, *>? = null

    fun start(wait: Boolean = false) {
        if (server != null) return
        server =
            embeddedServer(CIO, host = host, port = port) {
                routing {
                    get("/hello") { call.respondText("Hello, Askimo with Ktor CIO!") }
                    staticResources("/", basePackage = "public") {
                        staticResources("index.html", "public")
                    }
                }
            }.also { it.start(wait = wait) }

        println("Web server running at http://$host:$port  (GET /hello)")
    }

    fun stop() {
        server?.stop()
        server = null
    }
}
