package com.example.domain.model

data class BodyMetric(
    val uuid: String,
    val date: Long,
    val weightKg: Float,
    val waistCm: Float? = null
)

enum class RecordType {
    LONGEST_STREAK,
    MOST_30S,
    MOST_60S,
    MOST_2M,
    MOST_5M,
    MOST_SESSION,
    PEAK_RATE
}

data class PersonalRecord(
    val uuid: String,
    val type: RecordType,
    val value: Float,
    val previousValue: Float = 0f,
    val date: Long = System.currentTimeMillis(),
    val sessionId: String? = null
)
