package io.askimo.cli

object Logger {
    var debug: Boolean = System.getenv("ASKIMO_DEBUG") == "true"

    fun log(message: () -> String) {
        if (debug) println("[debug] ${message()}")
    }
}
