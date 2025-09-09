package com.example.greenery.ui.home

// Task 데이터 클래스
data class Task(
    val id: String, // 고유 ID 추가
    val title: String,
    var isChecked: Boolean // 체크 상태를 포함
)
