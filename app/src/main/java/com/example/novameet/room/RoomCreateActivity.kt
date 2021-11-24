package com.example.novameet.room

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.novameet.R
import com.example.novameet.databinding.ActivityRoomCreateBinding
import com.example.novameet.network.RetrofitManager

private val TAG : String = "[RoomCreateActivity]"

class RoomCreateActivity : AppCompatActivity() {

    private val binding: ActivityRoomCreateBinding by lazy { ActivityRoomCreateBinding.inflate(layoutInflater) }
    private var selectedImageFilePath: String? = null;

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val uri = intent?.data

            setProfileImage(uri.toString());

            selectedImageFilePath = getPathFromUri(uri)
            Log.d(TAG, "Hi: ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        val userImageView = binding.thumbnailImageView
        userImageView?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //            var photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startForResult.launch(photoPickerIntent)
            } else {
                val REQUEST_CODE = 1
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            }
        }

        binding.imageButtonExit.setOnClickListener {
            finish()
        }

        binding.imageButtonOk.setOnClickListener {
            requestCreateRoom(
                binding.roomId.text.toString(),
                intent?.extras?.getString("roomOwner"),
                selectedImageFilePath,
                binding.password.text.toString(),
                binding.memberCount.text.toString()
            )
        }
    }

    private fun setProfileImage(uri: String?){
        val thumbnailImageView = binding.thumbnailImageView
        try {
            if(!uri.isNullOrEmpty()) {
                thumbnailImageView?.let {
                    Glide.with(this)?.load(uri)?.into(it)
                }
            }
        }catch(e: Exception){
            e.printStackTrace()
            Log.d(TAG, e.toString())
        }
    }

    private fun getPathFromUri(uri: Uri?): String? {
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToNext()
        val path = cursor.getString(cursor.getColumnIndex("_data"))
        cursor.close()
        return path
    }

    private fun requestCreateRoom(roomId: String?,
                                  roomOwner: String?,
                                  roomThumbnailPath: String?,
                                  roomPassword: String?,
                                  roomMemberMaxCount: String?) {
        RetrofitManager.instance.requestCreateRoom(
            roomId=roomId,
            roomOwner=roomOwner,
            roomThumbnailPath= roomThumbnailPath,
            roomPassword= roomPassword,
            roomMemberMaxCount= roomMemberMaxCount,
            completion={ createRoomResponse ->
                if (createRoomResponse.responseCode == 1) {
                    var resultIntent = Intent()
                    resultIntent.putExtra("roomId", roomId)

                    val resultToast = Toast.makeText(this.applicationContext, "방 생성 완료", Toast.LENGTH_SHORT)
                    resultToast.show()

                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else if (createRoomResponse.responseCode == 0) {
                    val resultToast = Toast.makeText(this.applicationContext, "이미 같은 이름의 방이 존재합니다.", Toast.LENGTH_SHORT)
                    resultToast.show()
                } else {
                    val resultToast = Toast.makeText(this.applicationContext, "방 생성 예외 케이스. Server Log 확인 필요", Toast.LENGTH_SHORT)
                    resultToast.show()
                }
            })
    }
}