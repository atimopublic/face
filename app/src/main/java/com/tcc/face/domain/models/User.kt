package com.tcc.face.domain.models

data class User(
    val biometric: List<Biometric>,
    val email: String,
    val firstName: String,
    val id: String,
    val lastName: String,
    val mobile: String,
    val password: String,
    val pin: String,
    val status: Int
)