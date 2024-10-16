package com.tcc.face.domain.models

data class Error(
    val errorCode: String,
    val errorDetails: String,
    val errorMessage: String
)