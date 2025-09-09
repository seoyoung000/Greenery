package com.example.greenery.ui.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.greenery.MainActivity
import com.example.greenery.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 알림 채널 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "task_notifications"
            val channelName = "Task Notifications"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 알림을 보냄
        postNotification(context, intent)
    }

    private fun postNotification(context: Context, intent: Intent) {
        // 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없는 경우 알림을 보내지 않음
                return
            }
        }

        // 알림 설정
        val notificationId = System.currentTimeMillis().toInt() // 고유한 ID 생성
        val notificationIntent = Intent(context, MainActivity::class.java) // 클릭 시 이동할 Activity
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 타입을 확인
        val notificationType = intent.getStringExtra("notification_type")

        // 알림 제목과 내용 설정
        val title = if (notificationType == "water") "물 주기 알림" else "할 일 알림"
        val contentText = if (notificationType == "water") "물 주기를 확인하세요!" else "할 일을 확인하세요!"

        // 알림 생성
        val notification = NotificationCompat.Builder(context, "task_notifications")
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 알림 보내기
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
