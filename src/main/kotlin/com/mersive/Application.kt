package com.mersive

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.mersive.plugins.*
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging)
        configureRouting()
        configureSockets()
    }.start(wait = true)
}
