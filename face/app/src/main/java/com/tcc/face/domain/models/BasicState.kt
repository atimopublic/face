package com.tcc.face.domain.models

sealed class BasicState {
    object Idle : BasicState()
    object Loading : BasicState()
    object Success : BasicState()
    data class Error(val message: String) : BasicState()
}