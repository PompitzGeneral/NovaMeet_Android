package com.example.novameet.room.chat

import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

class SocketClient : Serializable {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    fun connect(port: Int) {
        try {
            socket = Socket("www.novameet.ga", port)
            outputStream = socket?.getOutputStream()
            inputStream  = socket?.getInputStream()
        } catch (e: Exception) {
            println("socket connect exception start!!")
            println("e: $e")
        }
    }

    fun sendData(data: String) {
        outputStream?.write(
            (data).toByteArray(Charsets.UTF_8)
        )
        outputStream?.flush()
    }

    fun read(): String? {
        var readLine:String? = null
        inputStream?.let {
            if (it?.available() > 0) {
                readLine = it?.bufferedReader(Charsets.UTF_8)?.readLine()
            }
        }
        return readLine
    }

    fun closeConnect() {
        outputStream?.close()
        inputStream?.close()
        socket?.close()
    }
}