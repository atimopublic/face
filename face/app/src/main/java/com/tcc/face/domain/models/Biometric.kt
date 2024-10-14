package com.tcc.face.domain.models

data class Biometric(
    val abisReferenceID: String,
    val biometricData: String,
    val biometricType: Int,
    val status: Int
)