package com.d101.data.model.user.response

data class UserResponse(
    val userEmail: String,
    val userNickname: String,
    val userLeafStatus: Int,
    val userNotification: Boolean,
    val userFruitStatus: Boolean,
    val social: Boolean,
)
