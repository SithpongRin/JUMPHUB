package com.example.domain.model

data class UserAccount(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = false,
    val lastSyncTime: Long = 0L
)
