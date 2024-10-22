package com.tcc.face.feature.ui.fragments.capture

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identy.face.AS
import com.identy.face.IdentyResponse
import com.identy.face.InitializationListener
import com.identy.face.enums.FaceTemplate
import com.identy.face.enums.UIOption
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.tcc.face.base.Constants
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

    var hubConnection: HubConnection? = null

    private val _faceResponse = MutableSharedFlow<IdentyResponse>()
    val faceResponse: SharedFlow<IdentyResponse> = _faceResponse

    private val _errorResponse = MutableSharedFlow<String>()
    val errorResponse: SharedFlow<String> = _errorResponse

    private val _authenticationData = MutableStateFlow<AuthenticationData?>(null)
    var authenticationData: StateFlow<AuthenticationData?> = _authenticationData
    lateinit var photoUri: Uri
    private val _paymentState = MutableStateFlow<BasicState>(BasicState.Idle)
    var paymentState: StateFlow<BasicState> = _paymentState

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

    //SignalR
    fun startConnection() {
        if (hubConnection == null) {
            hubConnection = HubConnectionBuilder
                .create(Constants.SIGNAL_R_HOST)
                .build()

            try {
                hubConnection?.start()?.blockingAwait()
            } catch (e: Exception) {
                Log.e("SignalR: ", "Failed to build signalR connection")
            }
        }
    }

    fun stopConnection() {
        hubConnection?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopConnection()
    }
}
