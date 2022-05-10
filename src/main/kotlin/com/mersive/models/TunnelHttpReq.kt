package com.mersive.models

data class TunnelHttpReq(
    val type: String = "TunnelHttpReq",
    val method: String,
    val uri: String,
    val headers: Map<String, List<String>>,
)
