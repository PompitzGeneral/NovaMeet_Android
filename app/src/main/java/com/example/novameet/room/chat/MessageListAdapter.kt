package com.example.novameet.room.chat

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

import android.widget.TextView

import android.view.LayoutInflater
import android.view.View

import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.novameet.R
import com.example.novameet.model.ChatMessage
import com.example.novameet.databinding.ItemChatMessageMeBinding
import com.example.novameet.databinding.ItemChatMessageOtherBinding
import com.example.novameet.util.Utils


class MessageListAdapter(
    private val context: Context,
    private var messageList: ArrayList<ChatMessage>,
    private var loginUserID: String?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TAG : String = "MessageListAdapter"
    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2

    private inner class SentMessageHolder(private val binding : ItemChatMessageMeBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.textChatMessageMe.setText(message.message)
            binding.textChatTimestampMe.setText(
                Utils.convertTimestampToHourAndMinute(message.createdAt)
            )
        }
    }

    private inner class ReceivedMessageHolder(private val binding : ItemChatMessageOtherBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.textChatMessageOther.text = message.message
            // Format the stored timestamp into a readable String using method.
            binding.textChatTimestampOther.text =
                Utils.convertTimestampToHourAndMinute(message.createdAt)
            binding.textChatUserOther.text = message.sender?.userDisplayName

            // Insert the profile image from the URL into the ImageView.
            try {
                if(!message.sender?.userImageUrl.isNullOrEmpty()) {
                    binding.imageChatProfileOther?.let {
                        Glide.with(context).
                        load(message.sender?.userImageUrl).
                        into(it)
                    }
                }
            } catch (e: Exception){
                e.printStackTrace()
                Log.d(TAG, e.toString())
            }
        }
    }

    fun setItems(items: ArrayList<ChatMessage>) {
        messageList = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    // Determines the appropriate ViewType according to the sender of the message.
    override fun getItemViewType(position: Int): Int {
        val message: ChatMessage = messageList[position]

        return if (message.sender?.userID.equals(loginUserID)) {
            // If the current user is the sender of the message
            VIEW_TYPE_MESSAGE_SENT
        } else {
            // If some other user sent the message
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_me, parent, false)
            return SentMessageHolder(ItemChatMessageMeBinding.bind(view))
        } else { // else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_other, parent, false)
            return ReceivedMessageHolder(ItemChatMessageOtherBinding.bind(view))
        }
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message: ChatMessage = messageList[position]
        when (holder!!.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageHolder?)?.bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder?)?.bind(message)
        }
    }
}