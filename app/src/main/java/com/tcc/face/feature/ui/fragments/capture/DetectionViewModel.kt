package com.tcc.face.feature.ui.fragments.capture

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.HttpException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identy.face.AS
import com.identy.face.IdentyResponse
import com.identy.face.InitializationListener
import com.identy.face.enums.FaceTemplate
import com.identy.face.enums.UIOption
import com.tcc.face.base.Constants
import com.tcc.face.base.websocket.Trigger
import com.tcc.face.domain.models.BasicState
import com.tcc.face.domain.models.PaymentAuthenticationRequest
import com.tcc.face.feature.ui.fragments.AuthRepository
import com.tcc.face.utils.SharedPreferencesManager
import com.tcc_arr.tccface.TccFace
import com.tcc_arr.tccface.TccResponseListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val sharedPreferencesManager: SharedPreferencesManager
) : ViewModel() {

    var faceBit: Bitmap? = null
    var amount: String? = ""
    var billNum: String? = ""
    var accountId: String? = ""
    var face64: String? = ""


    private val _faceResponse = MutableSharedFlow<IdentyResponse>()
    val faceResponse: SharedFlow<IdentyResponse> = _faceResponse

    private val _errorResponse = MutableSharedFlow<String>()
    val errorResponse: SharedFlow<String> = _errorResponse

    private val _authenticationData = MutableStateFlow<AuthenticationData?>(null)
    var authenticationData: StateFlow<AuthenticationData?> = _authenticationData
    lateinit var photoUri: Uri
    private val _paymentState = MutableStateFlow<BasicState>(BasicState.Idle)
    var paymentState: StateFlow<BasicState> = _paymentState

    private val _payableState = MutableStateFlow<BasicState>(BasicState.Idle)
    var payableState: StateFlow<BasicState> = _payableState
    var newPayable: Trigger? = null

    // Functions to update step data
    fun setAuthenticationData(data: AuthenticationData) {
        _authenticationData.value = data
    }

    fun clearTransaction() {
        face64 = ""
        amount = ""
        billNum = ""
        face64 = ""
        accountId = ""
        resetViewModel()
    }

    fun getFirstTimeStatus(): Boolean {
        return sharedPreferencesManager.getFirstTime()
    }

    fun setFirstTime(boolean: Boolean) {
        sharedPreferencesManager.firstTime(boolean)
    }

    fun resetViewModel() {
        viewModelScope.launch {
            _authenticationData.emit(null)

            _paymentState.emit(BasicState.Idle)
            _payableState.emit(BasicState.Idle)
        }

    }

    fun updatePayableState(newState: BasicState) {
        viewModelScope.launch {
            _payableState.emit(newState) // Emit new state
        }
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
                if (result.isSuccess && result.getOrNull()?.success == true) {
                    _paymentState.value = BasicState.Success
                } else {
                    if (result.getOrNull()?.error?.errorMessage != null)
                        _paymentState.value =
                            BasicState.Error("Failed: ${result.getOrNull()?.error?.errorMessage}")
                    else
                        _paymentState.value =
                            BasicState.Error("Failed: Payment authentication is failed")
                }
            } catch (e: retrofit2.HttpException) {
                // Check if it's a 404 error
                val errorBody = e.response()?.errorBody()?.string() ?: "Unknown error"

                if (e.code() == 404) {
                    // Try to parse the error body
                    _paymentState.value = BasicState.Error("Error 404: $errorBody")
                } else {
                    _paymentState.value =
                        BasicState.Error("HTTP Error: ${errorBody} ${e.message()}")
                }
            } catch (e: Exception) {
                _paymentState.value = BasicState.Error("Error: ${e.message}")
            }
        }
    }

    fun getPayable() {
        viewModelScope.launch {
            // _payableState.value = BasicState.Loading
            try {
                val result = authRepo.getPayable(Constants.DEVICE_ID)
                if (result.isSuccess && result.getOrNull()?.success == true) {
                    Log.e("succ", "error" + result.getOrNull()?.data)

                    val billNum = result.getOrNull()?.data?.billNumber
                    if (billNum != null && billNum != newPayable?.billNumber) {
                        newPayable = result.getOrNull()?.data
                        _payableState.value = BasicState.Success
                    }

                } else {
                    Log.e("failed", "error" + result.getOrNull()?.data)

                    _payableState.value =
                        BasicState.Error("Failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("exception", e.message.toString())

                _payableState.value = BasicState.Error("Error: ${e.message}")
            }
        }
    }

    fun isPayableIdle(): Boolean =
        payableState.value == BasicState.Idle || payableState.value is BasicState.Error


    fun initFaceSdk(context: Activity) {

        viewModelScope.launch {

            try {
                TccFace.newInstance(
                    context, Constants.FACE_API_KEY, Constants.FACE_SECRET_KEY, object :
                        InitializationListener<TccFace?> {


                        override fun onInit(tccFace: TccFace?) {
                            tccFace?.disableTraining()
                            try {
                                tccFace?.setASSecLevel(AS.HIGHEST)
                                //tccFace.enableICAOChecks();
                                tccFace?.setUioption(UIOption.TICKING_V2)
                                val templates: ArrayList<FaceTemplate> = ArrayList<FaceTemplate>()
                                templates.add(FaceTemplate.PNG)
                                //templates.add(FaceTemplate.ISO_19794_5);
                                tccFace?.setRequiredTemplates(templates)
                                tccFace?.capture()
                            } catch (e: java.lang.Exception) {

                                viewModelScope.launch {
                                    Log.e("error", "=" + e.localizedMessage)
                                    _errorResponse.emit(e.localizedMessage)
                                }

                            }
                        }

                        override fun onInitFailed() {
                            viewModelScope.launch {
                                _errorResponse.emit("License error")

                            }
                            Log.e("error", "=" + "License error")

                        }
                    },
                    object : TccResponseListener {
                        override fun onAttempt(i: Int, attempt: com.identy.face.Attempt?) {
                        }

                        override fun onResponse(
                            identyResponse: com.identy.face.IdentyResponse?,
                            hashSet: HashSet<String?>
                        ) {
                            if (!hashSet.isEmpty()) {
                                //   val transactionId = hashSet.stream().findFirst().get()
                                Log.e("error", "=" + "transactionId")

                                /*
                                Log.d(
                                    TAG,
                                    "onResponse: transactionId $transactionId"
                                )

                                 */
                                //    SavedData.faceTransactionId = transactionId
                                viewModelScope.launch {
                                    _faceResponse.emit(identyResponse!!)

                                }
                            }
                        }

                        override fun onErrorResponse(
                            identyError: com.identy.face.IdentyError,
                            hashSet: HashSet<String?>?
                        ) {
                            viewModelScope.launch {
                                _errorResponse.emit(identyError.getMessage())

                            }
                        }
                    },
                    false,
                    false
                )
            } catch (e: java.lang.Exception) {
                Log.e("error", "=" + e.localizedMessage)
                viewModelScope.launch {
                    _errorResponse.emit(e.localizedMessage)

                }
            }

        }


    }

}
