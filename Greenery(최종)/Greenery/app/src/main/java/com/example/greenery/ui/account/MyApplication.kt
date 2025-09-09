package com.example.greenery.ui.account

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class MyApplication : Application() {
    companion object {
        lateinit var auth: FirebaseAuth
        var email: String? = null
        lateinit var db: FirebaseFirestore
        lateinit var storage: FirebaseStorage
        lateinit var INSTANCE: MyApplication


        // 현재 사용자가 인증 상태인지 확인하는 함수
        fun checkAuth(): Boolean {
            val currentUser = auth.currentUser
            email = currentUser?.email
            return currentUser?.isEmailVerified ?: false
        }

    }


    override fun onCreate() {
        super.onCreate()
        // Firebase 서비스 초기화
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        storage = Firebase.storage

        // 초기 상태 디버깅 (선택 사항)
        if (auth.currentUser != null) {
            email = auth.currentUser?.email
            println("FirebaseAuth: Logged in as $email")
        } else {
            println("FirebaseAuth: No user logged in")
        }
    }
}
