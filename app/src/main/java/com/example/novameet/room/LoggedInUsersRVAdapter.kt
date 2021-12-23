package com.example.novameet.room

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
import com.example.novameet.databinding.ItemLoggedInUserBinding
import com.example.novameet.model.LoggedInUsersRVItem
import com.example.novameet.model.Room
import com.example.novameet.util.Utils


class LoggedInUsersRVAdapter (var pushMessageButtonClickCallback : (String?) -> Unit )
    : RecyclerView.Adapter<LoggedInUsersRVAdapter.ViewHolder>() {

    private val TAG : String = "LoggedInUsersRVAdapter"
    private var context: Context? = null
    private var loggedInUsers: ArrayList<LoggedInUsersRVItem> = arrayListOf();

    inner class ViewHolder(private val binding : ItemLoggedInUserBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(loggedInUser: LoggedInUsersRVItem) {
            // set UserID
            binding.itemTxtviewUserId.text = loggedInUser.userDisplayName
            // set UserAvatarImage
            try {
                if(!loggedInUser?.userImageUrl.isNullOrEmpty()) {
                    binding.itemImageviewUserImg?.let {
                        context?.let {
                            Glide.
                            with(it).
                            load(loggedInUser?.userImageUrl).
                            apply(RequestOptions()?.circleCrop()).
                            into(binding.itemImageviewUserImg)
                        }
                    }
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
            // set PushMessageButton Click Event
            binding.itemBtnPush.setOnClickListener {
                pushMessageButtonClickCallback?.invoke(loggedInUser.userID)
            }
        }
    }

    fun setItems(items: ArrayList<LoggedInUsersRVItem>) {
        loggedInUsers = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return loggedInUsers?.size
    }

    // Inflates the appropriate layout according to the ViewType.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.context = parent.context
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_logged_in_user, parent, false)
        return ViewHolder(ItemLoggedInUserBinding.bind(view))
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(loggedInUsers!!.get(position))
    }
}