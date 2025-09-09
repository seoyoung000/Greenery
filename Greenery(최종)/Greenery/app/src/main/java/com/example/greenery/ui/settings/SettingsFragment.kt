package com.example.greenery.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.greenery.R
import com.example.greenery.databinding.FragmentSettingsBinding
import com.example.greenery.ui.account.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var waterIntervalsSwitch: SwitchCompat
    private lateinit var taskNotificationSwitch: SwitchCompat
    private lateinit var auth: FirebaseAuth

    private val alarmManager: AlarmManager by lazy {
        requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val alarmPermissionRequestCode = 1 // 권한 요청 코드

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // FirebaseAuth 초기화
        auth = FirebaseAuth.getInstance()

        // 스위치 초기화
        waterIntervalsSwitch = binding.ivSettingToggleWaterIntervals
        taskNotificationSwitch = binding.ivSettingToggleTaskNotification

        // 스위치 상태 설정
        waterIntervalsSwitch.isChecked = SharedPreferencesUtil.getWaterIntervalsEnabled(requireContext())
        taskNotificationSwitch.isChecked = SharedPreferencesUtil.getTaskNotificationEnabled(requireContext())

        // 알림 시간 설정 UI 초기 상태
        updateNotificationTimeVisibility()

        // 물 주기 스위치 변경 리스너
        waterIntervalsSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleSwitchChange(isChecked, true) // 물 주기 알림
        }

        // 할 일 알림 스위치 변경 리스너
        taskNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleSwitchChange(isChecked, false) // 할 일 알림
        }

        // 스피너 값 저장
        binding.spinnerHour.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedHour = parentView?.getItemAtPosition(position).toString().toInt()
                SharedPreferencesUtil.setTaskNotificationHour(requireContext(), selectedHour)
                SharedPreferencesUtil.setWaterNotificationHour(requireContext(), selectedHour)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        binding.spinnerMinute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMinute = parentView?.getItemAtPosition(position).toString().toInt()
                SharedPreferencesUtil.setTaskNotificationMinute(requireContext(), selectedMinute)
                SharedPreferencesUtil.setWaterNotificationMinute(requireContext(), selectedMinute)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        // "설정 완료" 버튼 클릭 리스너
        binding.btnSetTimeComplete.setOnClickListener {
            val hour = SharedPreferencesUtil.getTaskNotificationHour(requireContext())
            val minute = SharedPreferencesUtil.getTaskNotificationMinute(requireContext())
            checkAndRequestPermissions(hour, minute) // 권한 확인 후 알림 설정
            Toast.makeText(requireContext(), "알림 시간이 설정되었습니다: $hour:$minute", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃 버튼 클릭 리스너
        binding.tvSettingText9.setOnClickListener {
            showLogoutDialog()
        }

        // 계정 삭제 버튼 클릭 리스너
        binding.tvSettingText10.setOnClickListener {
            showDeleteAccountDialog()
        }

        // 현재 로그인된 사용자의 이메일 가져오기
        val email = auth.currentUser?.email ?: "Unknown User"
        val userId = email.substringBefore("@")
        val userIdWithSuffix = userId + " 님"
        val profileTextView: TextView = binding.tvMypageText2
        profileTextView.text = userIdWithSuffix // 이메일에서 추출한 ID + "님"

        return root
    }

    // 스위치 변경 처리
    private fun handleSwitchChange(isChecked: Boolean, isWatering: Boolean) {
        if (isChecked) {
            val savedHour = SharedPreferencesUtil.getTaskNotificationHour(requireContext())
            val savedMinute = SharedPreferencesUtil.getTaskNotificationMinute(requireContext())
            if (isWatering) {
                SharedPreferencesUtil.setWaterIntervalsEnabled(requireContext(), true)
                checkAndRequestPermissions(savedHour, savedMinute) // 물 주기 알림 설정
            } else {
                SharedPreferencesUtil.setTaskNotificationEnabled(requireContext(), true)
                checkAndRequestPermissions(savedHour, savedMinute) // 할 일 알림 설정
            }
        } else {
            if (isWatering) {
                SharedPreferencesUtil.setWaterIntervalsEnabled(requireContext(), false)
                cancelDailyReminder(true) // 물 주기 알림 취소
            } else {
                SharedPreferencesUtil.setTaskNotificationEnabled(requireContext(), false)
                cancelDailyReminder(false) // 할 일 알림 취소
            }
        }
        updateNotificationTimeVisibility()
    }

    // 권한을 요청하고 알림 설정
    private fun checkAndRequestPermissions(hour: Int, minute: Int) {
        val permissions = mutableListOf<String>()

        // Android 13 이상에서만 POST_NOTIFICATIONS 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12 이상에서 SCHEDULE_EXACT_ALARM 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isExactAlarmPermissionGranted()) {
            Toast.makeText(requireContext(), "정확한 알람 권한을 설정하세요.", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        // 권한 요청
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissions.toTypedArray(),
                alarmPermissionRequestCode
            )
        } else {
            // 스위치 상태에 따라 알림 설정
            if (waterIntervalsSwitch.isChecked) {
                setDailyReminder(hour, minute, isWatering = true) // 물 주기 알림 설정
            }
            if (taskNotificationSwitch.isChecked) {
                setDailyReminder(hour, minute, isWatering = false) // 할 일 알림 설정
            }
        }
    }

    // SCHEDULE_EXACT_ALARM 권한 확인
    private fun isExactAlarmPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // 알림 시간 설정 UI 보이기/숨기기
    private fun updateNotificationTimeVisibility() {
        val isWateringEnabled = waterIntervalsSwitch.isChecked
        val isTaskEnabled = taskNotificationSwitch.isChecked
        binding.llTaskNotificationTime.visibility = if (isWateringEnabled || isTaskEnabled) View.VISIBLE else View.GONE
    }

    private fun setDailyReminder(hour: Int, minute: Int, isWatering: Boolean) {
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("notification_type", if (isWatering) "water" else "task") // 알림 유형 추가
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            if (isWatering) 1 else 2, // 고유 ID 분리
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 현재 시간보다 이전 시간인 경우 다음 날로 설정
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 정확한 알람 설정
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
        Log.d("AlarmReceiver", "알람이 설정되었습니다: ${calendar.time}") // 로그 추가
    }

    private fun cancelDailyReminder(isWatering: Boolean) {
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            if (isWatering) 1 else 2, // 고유 ID 분리
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == alarmPermissionRequestCode) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                val hour = SharedPreferencesUtil.getTaskNotificationHour(requireContext())
                val minute = SharedPreferencesUtil.getTaskNotificationMinute(requireContext())
                if (waterIntervalsSwitch.isChecked) {
                    setDailyReminder(hour, minute, isWatering = true) // 물 주기 알림 설정
                }
                if (taskNotificationSwitch.isChecked) {
                    setDailyReminder(hour, minute, isWatering = false) // 할 일 알림 설정
                }
                Toast.makeText(requireContext(), "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 로그아웃 다이얼로그
    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val cancelButton = dialogView.findViewById<View>(R.id.tv_logout_cancel)
        val okButton = dialogView.findViewById<View>(R.id.tv_logout_ok)

        cancelButton.setOnClickListener {
            dialog.dismiss() // 다이얼로그 닫기
        }

        okButton.setOnClickListener {
            dialog.dismiss() // 다이얼로그 닫기
            logOut() // 로그아웃 처리
        }

        dialog.show()
    }

    // 계정 삭제 다이얼로그
    private fun showDeleteAccountDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val cancelButton = dialogView.findViewById<View>(R.id.tv_delete_cancle)
        val okButton = dialogView.findViewById<View>(R.id.tv_delete_ok)

        cancelButton.setOnClickListener {
            dialog.dismiss() // 다이얼로그 닫기
        }
        okButton.setOnClickListener {
            dialog.dismiss() // 다이얼로그 닫기
            deleteAccount()
        }
        dialog.show()
    }

    // 계정 삭제 완료 다이얼로그
    private fun showDeleteCompleteDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_complete, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        dialog.show()

        // 3초 후에 AuthActivity로 이동
        Handler().postDelayed({
            dialog.dismiss() // 다이얼로그 닫기
            navigateToAuthActivity() // AuthActivity로 이동
        }, 3000) // 3초 대기 후 실행
    }

    // 로그아웃 함수
    private fun logOut() {
        auth.signOut()
        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
        navigateToAuthActivity()
    }

    // 계정 삭제 기능
    private fun deleteAccount() {
        val currentUser = auth.currentUser
        currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showDeleteCompleteDialog()
            } else {
                Toast.makeText(context, "계정 삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // AuthActivity로 이동
    private fun navigateToAuthActivity() {
        val intent = Intent(context, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        activity?.finish() // 현재 프래그먼트를 포함한 액티비티 종료
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
