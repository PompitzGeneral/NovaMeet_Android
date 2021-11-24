package com.example.novameet.room
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.novameet.R
import com.example.novameet.databinding.ItemSurfaceViewRendererBinding
import com.example.novameet.model.SurfaceViewRVItem
import okhttp3.internal.notifyAll
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack

class SurfaceViewAdapter(var rootEglBase: EglBase, val display: Display) : RecyclerView.Adapter<SurfaceViewAdapter.ViewHolder>() {
    private val TAG : String = "SurfaceViewAdapter"
    private var items : ArrayList<SurfaceViewRVItem>? = arrayListOf()
    private var itemHeight: Int = 0

    inner class ViewHolder(private val binding : ItemSurfaceViewRendererBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SurfaceViewRVItem) {
            Log.d(TAG, "bind - items:${items}, item:${item}")
            if(itemHeight != 0) {
                binding.constaraintLayout.layoutParams.height = itemHeight
            }
            // 뷰 객체가 재사용되기 때문에 release 시킨 후, 다시 init
            binding.surfaceViewRenderer.release()
            binding.surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            binding.surfaceViewRenderer.setMirror(true)
            binding.surfaceViewRenderer.setEnableHardwareScaler(true)
            binding.surfaceViewRenderer.init(rootEglBase?.eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    Log.d(TAG, "VideoView onFirstFrameRendered")
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    Log.d(TAG, "VideoView onFrameResolutionChanged")
                }
            })
            item.track?.addSink(binding.surfaceViewRenderer)

            if (item.isAudioEnabled) {
                binding.micIcon.setImageResource(R.drawable.ic_baseline_mic_24)
            } else {
                binding.micIcon.setImageResource(R.drawable.ic_baseline_mic_off_24)
            }
            binding.tvName.text = "${item.userDisplayName}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder{
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.item_surface_view_renderer, parent, false)
        return ViewHolder(ItemSurfaceViewRendererBinding.bind(inflater))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items!!.get(position))
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    fun setItems(newItems: ArrayList<SurfaceViewRVItem>?) {
        items = newItems;
        notifyDataSetChanged()
    }

    fun addItem(item: SurfaceViewRVItem) {
        items?.add(item)
        items?.let {
            var newItemindex = it.indexOf(item)
            notifyItemInserted(newItemindex)
        }
    }

    fun removeItem(socketID: String?) {
        var targetItem = items?.find { it.socketID == socketID }

        targetItem?.let {
            var removedItemIndex = items?.indexOf(targetItem)
            items?.remove(targetItem)
            removedItemIndex?.let {
                notifyItemRemoved(it)
            }
        }
    }

    fun clearItems() {
        items?.clear()
        notifyDataSetChanged()
    }

    fun changeItemHeight(spanCount: Int) {
        var displayMetrics = DisplayMetrics();
        display.getRealMetrics(displayMetrics)
        this.itemHeight = displayMetrics?.heightPixels / spanCount
        notifyDataSetChanged()
    }

    fun updateItemIsEnabeld(socketID: String?, type: String?, isEnabled: Boolean) {
        var targetItem = items?.find { it.socketID == socketID }

        targetItem?.let {
            var targetItemIndex = items?.indexOf(targetItem)
            if (type == "video") {
                targetItem.isVideoEnabled = isEnabled
            } else if (type == "audio") {
                targetItem.isAudioEnabled = isEnabled
            }
            targetItemIndex?.let {
                Log.d(TAG, "updateItemIsEnabeld(notifyItemChanged) - " +
                        "targetItem:${targetItem}, isEnabled:${isEnabled}, index:${targetItemIndex}")
//                notifyItemChanged(targetItemIndex)
                notifyDataSetChanged()
            }
        }
    }
}