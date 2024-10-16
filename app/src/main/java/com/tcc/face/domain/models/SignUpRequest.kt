package com.tcc.face.domain.models

data class SignUpRequest(
    val biometric: List<BiometricX>?,
    val email: String,
    val firstName: String,
    val lastName: String,
    val mobile: String,
    val password: String,
    val pin: String
)