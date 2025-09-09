package com.example.greenery.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.greenery.MainActivity
import com.example.greenery.R
import com.example.greenery.databinding.ActivityAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (MyApplication.checkAuth()) {
            changeVisibility("login")
        } else {
            changeVisibility("logout")
        }

        setupListeners()
    }

    private fun setupListeners() {
        // 회원가입 화면 이동
        binding.goSignInBtn.setOnClickListener {
            clearInputFields()
            changeVisibility("signin")
        }

        // 회원가입 버튼
        binding.signBtn.setOnClickListener {
            val email = binding.authEmailEditView.text.toString()
            val password = binding.authPasswordEditView.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("이메일과 비밀번호를 입력하세요.")
                return@setOnClickListener
            }

            MyApplication.auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { authTask ->
                    clearInputFields()
                    if (authTask.isSuccessful) {
                        MyApplication.auth.currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    showToast("회원가입 성공. 이메일을 확인하세요.")
                                    changeVisibility("logout")
                                } else {
                                    showToast("메일 발송 실패")
                                }
                            }
                    } else {
                        showToast("회원가입 실패: ${authTask.exception?.localizedMessage}")
                        changeVisibility("signin")
                    }
                }
        }

        // 가입 취소 버튼
        binding.signCancelBtn.setOnClickListener {
            clearInputFields()
            changeVisibility("logout")
        }

        // 로그인 버튼
        binding.loginBtn.setOnClickListener {
            val email = binding.authEmailEditView.text.toString().trim()
            val password = binding.authPasswordEditView.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("이메일과 비밀번호를 입력하세요.")
                return@setOnClickListener
            }

            MyApplication.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { loginTask ->
                    clearInputFields()
                    if (loginTask.isSuccessful) {
                        if (MyApplication.checkAuth()) {
                            MyApplication.email = email
                            changeVisibility("login")
                        } else {
                            showToast("이메일 인증이 필요합니다.")
                        }
                    } else {
                        showToast("로그인 실패: ${loginTask.exception?.localizedMessage}")
                    }
                }
        }

        // 구글 로그인 버튼
        binding.googleLoginBtn.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private val requestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val googleSignInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        clearInputFields()
        try {
            val account = googleSignInTask.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            MyApplication.auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { googleAuthTask ->
                    if (googleAuthTask.isSuccessful) {
                        MyApplication.email = account.email
                        changeVisibility("login")
                    } else {
                        showToast("구글 로그인 실패: ${googleAuthTask.exception?.localizedMessage}")
                        changeVisibility("logout")
                    }
                }
        } catch (e: ApiException) {
            showToast("구글 로그인 실패: ${e.localizedMessage}")
            changeVisibility("logout")
        }
    }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val signInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = signInClient.signInIntent
        requestLauncher.launch(signInIntent)
    }

    private fun changeVisibility(mode: String) {
        when (mode) {
            "login" -> {
                val intent = Intent(baseContext, MainActivity::class.java)
                intent.putExtra("user", MyApplication.email)
                startActivity(intent)
            }
            "logout" -> {
                binding.run {
                    authMainTextView.text = "로그인하거나 회원가입 해 주세요."
                    goSignInBtn.visibility = View.VISIBLE
                    googleLoginBtn.visibility = View.VISIBLE
                    authEmailEditView.visibility = View.VISIBLE
                    authPasswordEditView.visibility = View.VISIBLE
                    linearLayoutSign.visibility = View.GONE
                    loginBtn.visibility = View.VISIBLE
                }
            }
            "signin" -> {
                binding.run {
                    authMainTextView.text = "회원가입 해 주세요."
                    goSignInBtn.visibility = View.GONE
                    googleLoginBtn.visibility = View.GONE
                    authEmailEditView.visibility = View.VISIBLE
                    authPasswordEditView.visibility = View.VISIBLE
                    linearLayoutSign.visibility = View.VISIBLE
                    loginBtn.visibility = View.GONE
                }
            }
        }
    }

    private fun clearInputFields() {
        binding.authEmailEditView.text.clear()
        binding.authPasswordEditView.text.clear()
    }

    private fun showToast(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }
}
