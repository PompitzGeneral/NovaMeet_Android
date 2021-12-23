package com.example.novameet.room.chat

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

//: Serializable
class SocketClient {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    fun connect(url:String, port: Int) {
        try {
            socket = Socket(url, port)
            outputStream = socket?.getOutputStream()
            inputStream  = socket?.getInputStream()
        } catch (e: Exception) {
            println("socket connect exception start!!")
            println("e: $e")
        }
    }

    fun sendData(data: String) {
        try {
            Thread {
                outputStream?.write(data.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
            }.start()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun read(): String? {
        var msg: String? = null
        inputStream?.let {
            //Todo. Refactoring
            val bytes = ByteArray(1024)
            try {
                if (it?.available() > 0) {
                    val byteCount: Int = inputStream?.read(bytes) ?: 0
                    msg = String(bytes, 0, byteCount, Charsets.UTF_8)
                }

//            if (it?.available() > 0) {
//                msg = it?.bufferedReader(Charsets.UTF_8)?.readLine()
//            }

            } catch (e: Exception){
                e.printStackTrace()
            } finally {
                try {
                    //socket?.close();
                } catch (ioException: IOException) {
                    ioException.printStackTrace();
                }
            }
        }
        return msg
    }

    fun closeConnect() {
        outputStream?.close()
        inputStream?.close()
        socket?.close()
    }
}