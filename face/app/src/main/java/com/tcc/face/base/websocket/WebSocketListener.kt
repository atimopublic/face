package com.tcc.face.base.websocket

import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


interface WebSocketCallback {
    fun onMessageReceived(message: WebSocketMessage)
}

class WebSocketListener (
    private val callback: WebSocketCallback
) : WebSocketListener() {

    private val gson = Gson()
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        // WebSocket connection established
        println("WebSocket Opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        // Text message received
        println("Message Received: $text")
        val message = gson.fromJson(text, WebSocketMessage::class.java)
        callback.onMessageReceived(message)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        // Binary message received
        println("Binary Message Received: ${bytes.hex()}")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        webSocket.close(1000, null)
        println("WebSocket Closing: $code/$reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        println("Error: ${t.message}")
    }

}