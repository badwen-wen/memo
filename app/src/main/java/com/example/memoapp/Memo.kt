package com.example.memoapp

import java.util.Date
import java.util.UUID

data class Memo(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val dateCreated: Date = Date(),
    val dateModified: Date = Date(),
    val reminderTime: Long? = null, // 时间戳，用于提醒
    var isCompleted: Boolean = false,
    val latitude: Double? = null,  // 纬度，用于位置相关的提醒
    val longitude: Double? = null  // 经度，用于位置相关的提醒
)