package com.mersive.plugins

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.consumeEachBufferRange

fun Application.configureRouting() {
    routing {
        get("{...}") {
            val url = "https://releases.ubuntu.com${call.request.uri}"
            val client = HttpClient(CIO)
            // https://releases.ubuntu.com/22.04/ubuntu-22.04-desktop-amd64.iso
            client.prepareGet(url).execute { incomingResponse: HttpResponse ->
                val incomingChannel: ByteReadChannel = incomingResponse.body()
                call.respondBytesWriter(
                    contentType = incomingResponse.contentType(),
                    status = incomingResponse.status,
                    contentLength = incomingResponse.contentLength(),
                ) {
                    incomingChannel.consumeEachBufferRange { buffer, last ->
                        println("Writing ${buffer.asCharBuffer().length} bytes...")
                        writeFully(buffer)
                        true
                    }
                }

            }
        }
    }
}
