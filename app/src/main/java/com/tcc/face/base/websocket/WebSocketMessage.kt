package com.tcc.face.base.websocket

data class WebSocketMessage (
    val billNumber: String,
    val amount: Double,
    val accountId: String,
)


data class Trigger (
    val id: String,
    val device_ID: String,
    val account_ID: String,
    val billNumber: String,
    val amount: Double,
    val createdDate: String,
    val status: Int
)