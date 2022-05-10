package com.mersive.models

data class TunnelHttpResp(
    val type: String = "TunnelHttpResp",
    val statusCode: Int,
    val statusMsg: String,
    val headers: Map<String, List<String>>
)
