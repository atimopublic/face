package com.tcc.face.feature.ui.fragments.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.face.domain.models.BasicState
import com.tcc.face.domain.models.PaymentAuthenticationRequest
import com.tcc.face.feature.ui.fragments.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _authenticationData = MutableStateFlow<AuthenticationData?>(null)
    var authenticationData: StateFlow<AuthenticationData?> = _authenticationData

    private val _paymentState = MutableStateFlow<BasicState>(BasicState.Idle)
    var paymentState: StateFlow<BasicState> = _paymentState

    // Functions to update step data
    fun setAuthenticationData(data: AuthenticationData) {
        _authenticationData.value = data
    }

    fun resetViewModel() {
        _authenticationData.value = null
        paymentState = MutableStateFlow<BasicState>(BasicState.Idle)
    }


    data class AuthenticationData(
        val billNum: String,
        val imageData: String?
    )

    fun payment(request: PaymentAuthenticationRequest) {
        viewModelScope.launch {
            _paymentState.value = BasicState.Loading
            try {
                val result = authRepo.authenticatePayment(request)
                if (result.isSuccess) {
                    _paymentState.value = BasicState.Success
                } else {
                    _paymentState.value = BasicState.Error("Failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _paymentState.value = BasicState.Error("Error: ${e.message}")
            }
        }
    }
}
