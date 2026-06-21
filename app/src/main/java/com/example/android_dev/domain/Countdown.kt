package com.example.android_dev.domain

import java.time.LocalDate
import java.util.UUID

data class Countdown(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val targetDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)