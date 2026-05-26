package com.example.hycoms

data class NotificationModel(

    val title: String = "",
    val message: String = "",
    val time: Long = System.currentTimeMillis(),
    val isRead: Boolean = false

)