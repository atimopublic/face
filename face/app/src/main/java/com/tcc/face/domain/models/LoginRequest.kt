package com.tcc.face.domain.models

data class LoginRequest(
    val email: String,
    val password: String
)