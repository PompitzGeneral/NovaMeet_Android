package com.example.novameet.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import com.example.novameet.databinding.ActivityRegisterBinding
import com.example.novameet.network.RetrofitManager
import java.security.SecureRandom
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private val binding: ActivityRegisterBinding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }
    private var authNumberString: String? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        initEmailAuthButton()
        initRegisterButton()
    }

    private fun initEmailAuthButton() {
        binding.btnEmailAuth.setOnClickListener {
            // 1. 난수생성
            val rand = SecureRandom()
            authNumberString = rand.nextDouble().toString().substring(2, 8)
            // 2. 이메일 인증번호 발송 요청(이메일과 난수)
            val inputUserEmail = binding.userEmail.text.toString()
            requestEmailAuth(inputUserEmail, authNumberString);
        }
    }

    private fun initRegisterButton() {
        binding.btnRegister.setOnClickListener {
            val inputDisplayName = binding.userNickname.text.toString()
            val inputUserEmail = binding.userEmail.text.toString()
            val inputEmailAuthNumber = binding.authNumber.text.toString()
            val inputPassword = binding.password.text.toString()
            val inputRePassword = binding.repassword.text.toString()

            if (!checkDisplayNamePattern(inputDisplayName)) {
                val resultToast = Toast.makeText(this.applicationContext, "닉네임 형식이 유효하지 않습니다.(한글,영문 2~30자)", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (!checkEmailPattern(inputUserEmail)) {
                val resultToast = Toast.makeText(this.applicationContext, "이메일 형식이 유효하지 않습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (inputEmailAuthNumber.isNullOrEmpty()) {
                val resultToast = Toast.makeText(this.applicationContext, "이메일 인증 번호를 입력해 주세요", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (authNumberString != inputEmailAuthNumber) {
                val resultToast = Toast.makeText(this.applicationContext, "이메일 인증 번호가 올바르지 않습니다", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (!checkPasswordPattern(inputPassword)) {
                val resultToast = Toast.makeText(this.applicationContext, "패스워드 형식이 유효하지 않습니다.(영문,숫자를 혼합하여 8~30자)", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            if (inputPassword != inputRePassword) {
                val resultToast = Toast.makeText(this.applicationContext, "패스워드가 일치하지 않습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
                return@setOnClickListener;
            }
            requestRegester(inputUserEmail, inputPassword, inputDisplayName)
        }
    }


    private fun requestEmailAuth(userEmail: String?, authNumberString: String?) {
        RetrofitManager.instance.requestEmailAuth(userEmail=userEmail, authNumber=authNumberString, completion = { emailAuthResponse ->
            if (emailAuthResponse.responseCode == 1) {
                val resultToast = Toast.makeText(this.applicationContext, "입력된 이메일 주소로 인증번호를 발송했습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
            } else if (emailAuthResponse.responseCode == 0) {
                val resultToast = Toast.makeText(this.applicationContext, "이미 사용중인 이메일 주소입니다.", Toast.LENGTH_SHORT)
                resultToast.show()
            } else if (emailAuthResponse.responseCode == 1) {
                val resultToast = Toast.makeText(this.applicationContext, "인증번호 발송 실패.", Toast.LENGTH_SHORT)
                resultToast.show()
            } else {
                val resultToast = Toast.makeText(this.applicationContext, "인증번호 발송 실패.", Toast.LENGTH_SHORT)
                resultToast.show()
            }
        })
    }

    private fun requestRegester(userEmail: String?, password: String?, displayName: String?) {
        RetrofitManager.instance.requestRegister(userEmail=userEmail, password=password, displayName=displayName, completion = { registerResponse ->
            if (registerResponse.responseCode == 1) {
                val resultToast = Toast.makeText(this.applicationContext, "회원가입이 정상적으로 완료되었습니다.", Toast.LENGTH_SHORT)
                resultToast.show()
                var resultIntent = Intent()
//                resultIntent.putExtra("userEmail", loginResponse.user_id)
//                resultIntent.putExtra("userDisplayName", loginResponse.user_displayname)
//                resultIntent.putExtra("userImageUrl", loginResponse.user_image_url)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else if (registerResponse.responseCode == 0) {
                val resultToast = Toast.makeText(this.applicationContext, "이미 사용중인 이메일 주소입니다.", Toast.LENGTH_SHORT)
                resultToast.show()
            } else if (registerResponse.responseCode == 1) {
                val resultToast = Toast.makeText(this.applicationContext, "서버측 오류 발생", Toast.LENGTH_SHORT)
                resultToast.show()
            } else {
                val resultToast = Toast.makeText(this.applicationContext, "서버측 오류 발생", Toast.LENGTH_SHORT)
                resultToast.show()
            }
        })
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

    private fun checkDisplayNamePattern(displayName: String?): Boolean {
        val regex = Regex("^[가-힣]{2,30}|[a-zA-Z]{2,30}\\s|[a-zA-Z]{2,30}\$")
        return displayName?.matches(regex) ?: false
    }
}