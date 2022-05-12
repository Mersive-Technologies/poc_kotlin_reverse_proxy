package com.mersive.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mersive.models.TunnelHttpReq
import com.mersive.models.TunnelHttpResp
import com.mersive.models.TunnelMsg
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Application.configureRouting() {
    val mapper = ObjectMapper().registerModule(KotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    routing {
        get("{...}") {
            val headers = mutableMapOf<String, List<String>>()
            call.request.headers.forEach { key, list ->
                headers.put(key, list)
            }
            val req = TunnelHttpReq(
                method = call.request.httpMethod.value,
                uri = call.request.uri,
                headers = headers,
            )
            val client = HttpClient(CIO) {
                install(WebSockets) {
                    // Configure WebSockets
                }
            }
            val httpCall = call
            client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080) {
                val msg = mapper.writeValueAsString(req)
                send(Frame.Text(msg))
                val frame = incoming.receive() as Frame.Text
                val resp: TunnelHttpResp = mapper.readValue(frame.readText(), TunnelHttpResp::class.java)
//                httpCall.response.status(HttpStatusCode(resp.statusCode, resp.statusMsg))
                resp.headers.forEach { e ->
                    if(!"Content-Type".equals(e.key, true) && !"Content-Length".equals(e.key, true)) {
                        e.value.forEach { s ->
                            httpCall.response.headers.append(e.key, s, false)
                        }
                    }
                }

                println("Waiting for websocket frames...")
                httpCall.respondBytesWriter(
                    status = HttpStatusCode(resp.statusCode, resp.statusMsg),
                    contentType = ContentType.parse(resp.headers["Content-Type"]!![0]),
                    contentLength = resp.headers["Content-Length"]!![0].toLong(),
                ) {
                    for (frame in incoming) {
                        println("Got frame")
                        when (frame) {
                            is Frame.Binary -> {
                                println("Sending ${frame.buffer.asCharBuffer().length} bytes to HTTP client...")
                                writeFully(frame.buffer)
                                flush()
                            }
                            is Frame.Text -> {
                                println("Unexpected text frame")
                            }
                        }
                    }
                }
            }
            println("Closing socket...")
            client.close()

        }
    }
}
