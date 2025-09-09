package com.example.greenery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.greenery.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        lateinit var auth: FirebaseAuth
        var email: String? = null
        lateinit var db: FirebaseFirestore
        lateinit var storage: FirebaseStorage

        fun checkAuth(): Boolean {
            val currentUser = auth.currentUser
            email = currentUser?.email
            return currentUser?.isEmailVerified ?: false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // 뷰 바인딩 설정
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // 메뉴 ID를 최상위 대상으로 전달
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_myplant,
                R.id.navigation_community,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}
