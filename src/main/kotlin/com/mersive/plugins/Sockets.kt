package com.mersive.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mersive.models.TunnelHttpReq
import com.mersive.models.TunnelHttpResp
import com.mersive.models.TunnelMsg
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.consumeEachBufferRange
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.reflect.jvm.internal.impl.types.checker.ClassicTypeSystemContext.DefaultImpls.original


fun Application.configureSockets() {
    val mapper = ObjectMapper().registerModule(KotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/") { // websocketSession
            var done = false
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val msg: TunnelMsg = mapper.readValue(text, TunnelMsg::class.java)
                        if (msg.type == "TunnelHttpReq") {
                            val req: TunnelHttpReq = mapper.readValue(text, TunnelHttpReq::class.java)
                            val url = "https://releases.ubuntu.com${req.uri}"
                            val client = HttpClient(CIO)
                            // TODO: forward other request fields (method, headers)
                            // https://releases.ubuntu.com/22.04/ubuntu-22.04-desktop-amd64.iso
                            client.prepareGet(url).execute { incomingResponse: HttpResponse ->
                                val headers = mutableMapOf<String, List<String>>()
                                incomingResponse.headers.forEach { key, list ->
                                    headers.put(key, list)
                                }
                                val resp = TunnelHttpResp(
                                    statusCode = incomingResponse.status.value,
                                    statusMsg = incomingResponse.status.description,
                                    headers = headers,
                                )
                                val json = mapper.writeValueAsString(resp)
                                outgoing.send(Frame.Text(json))

                                val incomingChannel: ByteReadChannel = incomingResponse.body()
                                incomingChannel.consumeEachBufferRange { buffer, last ->
                                    println("Sending last=${last} ${buffer.asCharBuffer().length} bytes to websocket...")
                                    outgoing.send(Frame.Binary(true, buffer))
                                    done = last
                                    !last
                                }
                                println("exit consumeEachBufferRange")
                            }
                        }
                    }
                }
                if (done) break
            }
        }
    }
}
