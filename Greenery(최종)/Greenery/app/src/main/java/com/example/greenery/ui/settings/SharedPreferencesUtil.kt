package com.example.greenery.ui.settings

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesUtil {

    private const val PREFS_NAME = "com.example.greenery.preferences"
    private const val KEY_ALL_NOTIFICATIONS_ENABLED = "key_all_notifications_enabled"
    private const val KEY_WATER_INTERVALS_ENABLED = "key_water_intervals_enabled"
    private const val KEY_TASK_NOTIFICATION_ENABLED = "key_task_notification_enabled"
    private const val KEY_TASK_NOTIFICATION_HOUR = "key_task_notification_hour"
    private const val KEY_TASK_NOTIFICATION_MINUTE = "key_task_notification_minute"
    private const val KEY_WATER_NOTIFICATION_HOUR = "key_water_notification_hour"
    private const val KEY_WATER_NOTIFICATION_MINUTE = "key_water_notification_minute"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 전체 알림 상태 저장
    fun setAllNotificationsEnabled(context: Context, isEnabled: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(KEY_ALL_NOTIFICATIONS_ENABLED, isEnabled)
        editor.apply()
    }

    // 전체 알림 상태 불러오기
    fun getAllNotificationsEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_ALL_NOTIFICATIONS_ENABLED, true)
    }

    // 물 주기 알림 상태 저장
    fun setWaterIntervalsEnabled(context: Context, isEnabled: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(KEY_WATER_INTERVALS_ENABLED, isEnabled)
        editor.apply()
    }

    // 물 주기 알림 상태 불러오기
    fun getWaterIntervalsEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_WATER_INTERVALS_ENABLED, true)
    }

    // 할 일 알림 상태 저장
    fun setTaskNotificationEnabled(context: Context, isEnabled: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(KEY_TASK_NOTIFICATION_ENABLED, isEnabled)
        editor.apply()
    }

    // 할 일 알림 상태 불러오기
    fun getTaskNotificationEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_TASK_NOTIFICATION_ENABLED, true)
    }

    // 알림 시간 (할 일 - 시간) 저장
    fun setTaskNotificationHour(context: Context, hour: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(KEY_TASK_NOTIFICATION_HOUR, hour)
        editor.apply()
    }

    // 알림 시간 (할 일 - 시간) 불러오기
    fun getTaskNotificationHour(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_TASK_NOTIFICATION_HOUR, 21) // 기본값: 21시
    }

    // 알림 시간 (할 일 - 분) 저장
    fun setTaskNotificationMinute(context: Context, minute: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(KEY_TASK_NOTIFICATION_MINUTE, minute)
        editor.apply()
    }

    // 알림 시간 (할 일 - 분) 불러오기
    fun getTaskNotificationMinute(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_TASK_NOTIFICATION_MINUTE, 0) // 기본값: 0분
    }

    // 알림 시간 (물 주기 - 시간) 저장
    fun setWaterNotificationHour(context: Context, hour: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(KEY_WATER_NOTIFICATION_HOUR, hour)
        editor.apply()
    }

    // 알림 시간 (물 주기 - 시간) 불러오기
    fun getWaterNotificationHour(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_WATER_NOTIFICATION_HOUR, 21) // 기본값: 21시
    }

    // 알림 시간 (물 주기 - 분) 저장
    fun setWaterNotificationMinute(context: Context, minute: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(KEY_WATER_NOTIFICATION_MINUTE, minute)
        editor.apply()
    }

    // 알림 시간 (물 주기 - 분) 불러오기
    fun getWaterNotificationMinute(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_WATER_NOTIFICATION_MINUTE, 0) // 기본값: 0분
    }
}
