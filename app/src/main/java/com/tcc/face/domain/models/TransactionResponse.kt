package com.tcc.face.domain.models

data class TransactionResponse(
    val id: String,
    val biometricVerification_ID: String,
    val paymentCard_ID: String,
    val account_ID: String,
    val transactionType: Int,
    val billNumber: String,
    val amount: Double,
    val createdDate: String,
    val transactionNumber: String,
    val status: Int,
    val business: BusinessResponse,
    val biometricVerification: String,
    val paymentCard: String
)

data class BusinessResponse(
    val id: String,
    val name: String,
    val businessTypes: Int,
    val email: String,
    val mobile: String,
    val address: String,
    val createdDate: String,
    val isEmailVerified: String,
    val isMobileVerified: String,
    val status: Int,
    val account: String
)