package com.example.novameet.home

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.novameet.R
import com.example.novameet.databinding.ItemRoomBinding
import com.example.novameet.model.Room
import com.example.novameet.network.RetrofitManager
import com.example.novameet.room.RoomActivity

private val TAG : String = "[RoomsRVAdapter]"

class RoomsRecyclerViewAdapter(var roomClickCallback : ((String?, Boolean) -> Unit)?)
    : RecyclerView.Adapter<RoomsRecyclerViewAdapter.ViewHolder>() {

    lateinit var mContext: Context
    var items: List<Room>? = arrayListOf();

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : ViewHolder {
        val binding = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        mContext = parent.context
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items!!.get(position), position)
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    fun setItems(newItems: ArrayList<Room>?) {
        items = newItems;
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemRoomBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(room: Room, position: Int) {
            var thumbnailImageView = binding.itemImageviewThumbnail
            var userImageView = binding.itemImageviewUserImg
            var roomIdTextView = binding.itemTxtviewRoomId
            var roomOwnerTextView = binding.itemTxtviewRoomOwner
            var memberCountTextView = binding.itemTxtviewMemberCount

            try {
                if(!room.roomThumbnailUrl.isNullOrEmpty()) {
                    thumbnailImageView?.let {
                        Glide.with(mContext).
                        load(room.roomThumbnailUrl).
                        into(it)
                    }
                }
                if(!room.roomOwnerImageUrl.isNullOrEmpty()) {
                    userImageView?.let {
                        Glide.with(mContext).
                        load(room.roomOwnerImageUrl).
                        apply(RequestOptions()?.circleCrop()).
                        into(it)
                    }
                }
            }catch(e: Exception){
                e.printStackTrace()
                Log.d(TAG, e.toString())
            }

            room.roomId.let {
                roomIdTextView.setText(it)
            }
            room.roomOwner.let {
                roomOwnerTextView.setText(it)
            }
            memberCountTextView.setText("참여인원 : ${room.roomMemberCurrentCount?:0}/${room.roomMemberMaxCount?:0}")

            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    roomClickCallback?.invoke(room.roomId, room.hasPassword)
                }
            }
        }
    }
}