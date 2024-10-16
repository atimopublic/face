package com.tcc.face.base.websocket

import com.tcc.face.base.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class WebSocketManager(
    private val callback: WebSocketCallback
) {

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket

    fun startWebSocket() {
        val request = Request.Builder().url(Constants.WEB_SOCKET).build()
        val listener = WebSocketListener(callback)
        webSocket = client.newWebSocket(request, listener)

        // Optional: Trigger a message to be sent after establishing a connection
        // webSocket.send("Hello from Android WebSocket!")
    }

    fun closeWebSocket() {
        webSocket.close(1000, "Client closed")
    }
}