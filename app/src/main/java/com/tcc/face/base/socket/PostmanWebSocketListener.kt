package com.tcc.face.base.socket

import android.util.Log
import com.squareup.okhttp.Request
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class PostmanWebSocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        // WebSocket connection opened successfully
        Log.d("WebSocket", "Connection opened")
        
        // Send a test message to the echo server
        webSocket.send("Hello, Postman WebSocket!")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // Log the message received from the server (should echo back)
        Log.d("WebSocket", "Received: $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        // Log the received byte message
        Log.d("WebSocket", "Received bytes: $bytes")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        Log.d("WebSocket", "Connection closing: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // Log any errors or failures
        Log.e("WebSocket", "Error: ${t.message}")
    }
}

