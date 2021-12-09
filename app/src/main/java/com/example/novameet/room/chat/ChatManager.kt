package com.example.novameet.room.chat

import com.example.novameet.model.ChatMessage
import com.example.novameet.model.User
import okhttp3.internal.immutableListOf
import java.net.Socket
import kotlin.concurrent.thread

class ChatManager(
    val userID:String?,
    val userDisplayName: String?,
    val userImageUrl: String?,
    val roomID: String?)
{
    val TAG = "ChatManager"

    var messageList: ArrayList<ChatMessage> = arrayListOf()

    // TCP Client
    var tcpClient = SocketClient()

    var isConnected = false
    var isCommunicated = false
    var communicateThread: Thread? = null
    var events: ChatEvents? = null

    fun start() {
        thread {
            connect()
            sendJoin()
            startCommunicate()
        }
    }

    fun stop() {
        thread {
            stopCommunicate()
            disconnect()
        }
    }

    fun sendJoin() {
        var messageName = "join"
        var message = "${messageName};${userID};${userDisplayName};${userImageUrl};${roomID}"
        tcpClient.sendData(message)
    }

    fun sendChatMessage(messageText: String?) {
        var messageName = "sendMessage"
        var message = "${messageName};${messageText}"
        thread {
            tcpClient.sendData(message)
        }
    }

    fun sendLeaveAll() {
        //Todo.
    }

    private fun doCommunicate() {
        isCommunicated = true
        while (isCommunicated) {
            // 1. read message
            var message: String? = tcpClient.read()
            message?.let {
                var fields = it.split(";")
                if (fields.get(0) == "message") {
                    receivedMessage(
                        fields.get(1),
                        fields.get(2),
                        fields.get(3),
                        fields.get(4),
                        fields.get(5).toLong()
                    )
                } else if (fields.get(0) == "leave_room") {
                    receivedLeaveRoom()
                }
            }

            Thread.sleep(100)
        }
    }

    private fun startCommunicate() {
        communicateThread = thread {
            doCommunicate()
        }
    }

    private fun stopCommunicate() {
        isCommunicated = false
    }

    private fun receivedMessage(userID: String?,
                                userDisplayName: String?,
                                userImageUrl: String?,
                                messageText: String?,
                                timestamp: Long) {
        var senderUser = User(0, userID, userDisplayName, userImageUrl, 0);
        var chatMessage = ChatMessage(messageText, senderUser, timestamp)
        messageList.add(chatMessage)

        events?.onReceivedMessage()
    }

    private fun receivedLeaveRoom() {

    }

    private fun connect() {
        tcpClient.connect( 5001)
    }

    private fun disconnect() {
        tcpClient.closeConnect()
    }
}