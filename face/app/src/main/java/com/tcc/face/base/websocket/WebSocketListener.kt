package com.tcc.face.base.websocket

import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


interface WebSocketCallback {
    fun onMessageReceived(message: WebSocketMessage)
    fun onConnectionSuccess()
    fun onConnectionFailure(error: String?)
    fun onConnectionClosed()
}

class WebSocketListener (
    private val callback: WebSocketCallback
) : WebSocketListener() {

    private val gson = Gson()
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        // WebSocket connection established
        println("WebSocket Opened")
        callback.onConnectionSuccess()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)

        // Log the raw received message
        println("Message Received: $text")

        try {
            // First, parse the stringified JSON (which is a valid JSON string)
            val jsonString = gson.fromJson(text, String::class.java)

            // Now parse the unescaped JSON string into the WebSocketMessage object
            val message = gson.fromJson(jsonString, WebSocketMessage::class.java)

            // Handle the parsed WebSocketMessage
            callback.onMessageReceived(message)
        } catch (e: Exception) {
            // Handle JSON parsing errors
            println("WS: Failed to parse message: $text")
            println(e.localizedMessage)
        }
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
        callback.onConnectionClosed()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        println("Error: ${t.message}")
        callback.onConnectionFailure(t.message)
    }

}