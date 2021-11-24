package com.example.novameet.home

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.novameet.R
import com.example.novameet.databinding.FragmentHomeBinding
import com.example.novameet.databinding.FragmentRecordBinding
import com.example.novameet.model.Room
import com.example.novameet.model.User
import com.example.novameet.network.RetrofitManager
import com.example.novameet.room.RoomActivity
import pub.devrel.easypermissions.EasyPermissions
import java.util.ArrayList


/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var binding: FragmentHomeBinding? = null

    private var rooms: ArrayList<Room>? = arrayListOf()
    private var roomClickCallback: ( (roomId: String?, hasPassword: Boolean) -> Unit )? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //userInfo = it.getParcelable<User?>(ARG_PARAM1)
        }

        requestRoomInfos()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding?.root

        initUI()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null;
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun initUI() {
        val roomsRecyclerView = binding?.rvRooms
        roomsRecyclerView?.adapter = RoomsRecyclerViewAdapter(roomClickCallback)
        roomsRecyclerView?.layoutManager = LinearLayoutManager(context)
    }

    private fun requestRoomInfos() {
        RetrofitManager.instance.requestRoomInfos(completion = { roomInfosResponse ->
            rooms = roomInfosResponse.roomInfos
            var roomsRecyclerViewAdapter = binding?.rvRooms?.adapter as RoomsRecyclerViewAdapter?
            roomsRecyclerViewAdapter?.setItems(rooms)
        })
    }

    fun setRoomClickCallback(roomClickCallback: ( (roomId: String?, hasPassword: Boolean) -> Unit )?) {
        this.roomClickCallback = roomClickCallback
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {

            }
    }
}