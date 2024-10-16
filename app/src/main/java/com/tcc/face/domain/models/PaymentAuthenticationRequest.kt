package com.tcc.face.domain.models

data class PaymentAuthenticationRequest (
    val account_ID: String,
    val billNumber: String,
    val amount: String,
    val pin: String,
    val biometric: BiometricData
)

data class BiometricData (
    val biometricType: Int = 1,
    val biometricData: String
)

data class PaymentAuthenticationResponse (
    val id: String,
    val transactionType: Int,
    val billNumber: String,
    val amount: String,
    val transactionNumber: String,
    val status: Int,
    val biometricVerification: BiometricDataResponse,
    val payment: String,
    val account: String,
    val customer: CustomerResponse
)


data class BiometricDataResponse (
    val biometricType: Int,
    val biometricData: String,
    val createdDate: String,
    val verificationID: String,
    val verificationStatus: Int
)

data class CustomerResponse (
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val mobile: String,
    val status: Int
)