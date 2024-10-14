package com.tcc.face.domain.models

data class CardResponse (
    val nameOnCard: String,
    val cardType: String,
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val createdDate: String?,
    val updatedDate: String?,
    val isPrimary: Boolean,
    val status: Int
)