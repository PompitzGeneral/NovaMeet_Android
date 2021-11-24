package com.example.novameet.profile
import android.Manifest
import android.R.attr
import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.novameet.R
import com.example.novameet.databinding.ActivityProfileBinding
import com.example.novameet.network.RetrofitManager
import java.security.SecureRandom
import android.graphics.BitmapFactory

import android.graphics.Bitmap

import android.R.attr.data
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.InputStream


class ProfileActivity : AppCompatActivity() {

    private val TAG : String = "[ProfileActivity]"
    private val binding: ActivityProfileBinding by lazy { ActivityProfileBinding.inflate(layoutInflater) }
    private var selectedImageFilePath: String? = null;

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val uri = intent?.data

            setProfileImage(uri.toString());
            
            selectedImageFilePath = getPathFromUri(uri)
            Log.d(TAG, "Hi: ")
//          selectedImageFilePath = uri?.path
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        val userEmail: String? = intent?.extras?.getString("userEmail");
        val userDisplayName: String? = intent?.extras?.getString("userDisplayName");
        val userImageUrl: String? = intent?.extras?.getString("userImageUrl");

        setProfileImage(userImageUrl)

        val userImageView = binding.userImageView
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

        binding.userEmail.setText(userEmail)
        binding.userNickname.setText(userDisplayName)

        initChangeUserInfoButton()
        initChangePasswordButton()
    }

    private fun initChangeUserInfoButton() {
        binding.btnChangeUserinfo.setOnClickListener {
            val inputUserEmail = binding.userEmail.text.toString()
            val inputUserDisplayName = binding.userNickname.text.toString()

            if (!checkDisplayNamePattern(inputUserDisplayName)) {
                val resultToast = Toast.makeText(this.applicationContext, "닉네임 형식이 유효하지 않습니다.(한글,영문 2~30자)", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }

            requestChangeUserInfo(selectedImageFilePath, inputUserEmail, inputUserDisplayName)
        }
    }

    private fun initChangePasswordButton() {
        binding.btnChangePassword.setOnClickListener {
            val inputUserEmail = binding.userEmail.text.toString()
            val inputCurrentPassword = binding.currentPassword.text.toString()
            val inputNewPassword = binding.newPassword.text.toString()
            val inputNewRePassword = binding.newRepassword.text.toString()

            if (!checkPasswordPattern(inputNewPassword)) {
                val resultToast = Toast.makeText(this.applicationContext, "변경하려는 패스워드 형식이 유효하지 않습니다.(영문,숫자를 혼합하여 8~30자)", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (inputCurrentPassword == inputNewPassword) {
                val resultToast = Toast.makeText(this.applicationContext, "기존 패스워드와 변경하려는 패스워드가 같습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (inputNewPassword != inputNewRePassword) {
                val resultToast = Toast.makeText(this.applicationContext, "변경하려는 패스워드가 일치하지 않습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }

            requestChangePassword(inputUserEmail, inputCurrentPassword, inputNewPassword)
        }
    }

    private fun requestChangeUserInfo(filePath: String?, userEmail: String?, userDisplayName: String?) {
        RetrofitManager.instance.requestUpdateUserInfo(
            filePath=filePath,
            userEmail=userEmail,
            userDisplayName= userDisplayName,
            completion={ updateUserInfoResponse ->
                if (updateUserInfoResponse.responseCode == 1) {
                    var resultIntent = Intent()
                    resultIntent.putExtra("userEmail", updateUserInfoResponse.user_id)
                    resultIntent.putExtra("userDisplayName", updateUserInfoResponse.user_displayname)
                    resultIntent.putExtra("userImageUrl", updateUserInfoResponse.user_image)
                    setResult(RESULT_OK, resultIntent)

                    val resultToast = Toast.makeText(this.applicationContext, "회원정보가 정상적으로 변경되었습니다.", Toast.LENGTH_SHORT)
                    resultToast.show()
                } else if (updateUserInfoResponse.responseCode == 0) {
                    val resultToast = Toast.makeText(this.applicationContext, "회원정보 변경 실패.", Toast.LENGTH_SHORT)
                    resultToast.show()
                } else {

                }
            })
    }

    private fun requestChangePassword(userEmail: String?, currentPassword: String?, newPassword: String?) {
        RetrofitManager.instance.requestUpdateUserPassword(
            userEmail=userEmail,
            currentPassword=currentPassword,
            newPassword=newPassword,
            completion={ updateUserPasswordResponse ->
                if (updateUserPasswordResponse.responseCode == 1) {
                    val resultToast = Toast.makeText(this.applicationContext, "패스워드가 정상적으로 변경되었습니다.", Toast.LENGTH_SHORT)
                    resultToast.show()
                } else if (updateUserPasswordResponse.responseCode == 0) {
                    val resultToast = Toast.makeText(this.applicationContext, "패스워드가 틀렸습니다.", Toast.LENGTH_SHORT)
                    resultToast.show()
                } else if (updateUserPasswordResponse.responseCode == -1) {
                    val resultToast = Toast.makeText(this.applicationContext, "패스워드 변경 실패", Toast.LENGTH_SHORT)
                    resultToast.show()
                } else {

                }
            })
    }

    fun setProfileImage(uri: String?){
        val userImageView = binding.userImageView
        try {
            if(!uri.isNullOrEmpty()) {
                userImageView?.let {
                    Glide.with(this)?.load(uri)?.apply(RequestOptions()?.circleCrop()).into(it)
                }
            }
        }catch(e: Exception){
            e.printStackTrace()
            Log.d(TAG, e.toString())
        }
    }

    fun getPathFromUri(uri: Uri?): String? {
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToNext()
        val path = cursor.getString(cursor.getColumnIndex("_data"))
        cursor.close()
        return path
    }

    private fun checkDisplayNamePattern(displayName: String?): Boolean {
        val regex = Regex("^[가-힣]{2,30}|[a-zA-Z]{2,30}\\s|[a-zA-Z]{2,30}\$")
        return displayName?.matches(regex) ?: false
    }

    private fun checkPasswordPattern(password: String?): Boolean {
        // Todo. this regex apply to WebApp
        val regex = Regex("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[\$@\$!%*#?&]).{8,30}.\$")
        return password?.matches(regex) ?: false
    }
}