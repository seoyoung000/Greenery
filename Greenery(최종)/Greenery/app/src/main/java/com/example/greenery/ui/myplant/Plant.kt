package com.example.greenery.ui.myplant

data class Plant(
    var id: String = "",          // 식물의 고유 ID
    var name: String = "",        // 식물 이름
    var species: String = "",     // 식물 종
    var image: String = "",       // 식물 사진 URI
    var waterInterval: String = "", // 물 주기
    val userId: String = "" // 추가된 필드
)