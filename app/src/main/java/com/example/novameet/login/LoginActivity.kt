package com.example.novameet.login
import android.app.Activity
import android.content.Intent
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.novameet.databinding.ActivityLoginBinding
import com.example.novameet.model.User
import com.example.novameet.network.RetrofitManager
import com.example.novameet.register.RegisterActivity
import com.google.gson.JsonObject
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    private val TAG : String = "[LoginActivity]"
    private val binding: ActivityLoginBinding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    val startForRegisterResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding?.userEmail.text?.clear()
            binding?.password.text?.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "btnLogin Clicked");
            val inputUserEmail: String? = binding.userEmail.text.toString()
            val inputPassword: String?  = binding.password.text.toString()

            if (!checkEmailPattern(inputUserEmail)) {
                val resultToast = Toast.makeText(this, "이메일 형식이 유효하지 않습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener
            }

            if (!checkPasswordPattern(inputPassword)) {
                val resultToast = Toast.makeText(this, "패스워드 형식이 유효하지 않습니다.(영문,숫자를 혼합하여 8~30자)", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener
            }

            requestLogin(inputUserEmail, inputPassword)
        }

        binding.btnRegister.setOnClickListener {
            doRegisterActivityForResult()
        }
    }

    private fun checkEmailPattern(userEmail: String?): Boolean {
        val pattern: Pattern = android.util.Patterns.EMAIL_ADDRESS
        return pattern.matcher(userEmail).matches()
    }

    private fun checkPasswordPattern(password: String?): Boolean {
        // Todo. this regex apply to WebApp
        val regex = Regex("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[\$@\$!%*#?&]).{8,30}.\$")
        return password?.matches(regex) ?: false
    }

    private fun requestLogin(userEmail: String?, password: String?){
        RetrofitManager.instance.requestLogin(userEmail=userEmail, password=password, completion = { loginResponse ->
            if (loginResponse.responseCode == 0) {
                val resultToast = Toast.makeText(this.applicationContext, "Email이 존재하지 않습니다", Toast.LENGTH_SHORT)
                resultToast.show()
            } else if (loginResponse.responseCode == -1) {
                val resultToast = Toast.makeText(this.applicationContext, "패스워드를 확인해 주세요", Toast.LENGTH_SHORT)
                resultToast.show()
            } else if (loginResponse.responseCode == 1) {
                val resultToast = Toast.makeText(this.applicationContext, "로그인 성공", Toast.LENGTH_SHORT)
                resultToast.show()
                var resultIntent = Intent()
                resultIntent.putExtra("userIdx", loginResponse.user_idx)
                resultIntent.putExtra("userEmail", loginResponse.user_id)
                resultIntent.putExtra("userDisplayName", loginResponse.user_displayname)
                resultIntent.putExtra("userImageUrl", loginResponse.user_image_url)
                resultIntent.putExtra("dailyFocusTime", loginResponse.dailyFocusTime)
                setResult(RESULT_OK, resultIntent)
                finish();
            } else {

            }
        })
    }

    private fun doRegisterActivityForResult() {
        startForRegisterResult.launch(Intent(this, RegisterActivity::class.java))
    }
}