package com.tcc.face.feature.ui.fragments.capture

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import com.tcc_arr.tccface.TccFace
import com.tcc_arr.tccface.TccResponseListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

     var faceBit :Bitmap?=null
     var amount :String?="12"
     var billNum: String?="12345"
     var accountId: String?="2F34A6A1-59F7-4FB7-4802-08DCEDF0023A"
     var face64:String?=""

    private val faceResponse = MutableLiveData<IdentyResponse?>()
    private val docResponse = MutableLiveData<IdentyResponse>()
    private val errorResponse = MutableLiveData<String>()
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

    fun clearTransaction()
    {
        face64=""
        amount=""
        billNum=""
        face64=""
        accountId=""
        payableState = MutableStateFlow<BasicState>(BasicState.Idle)
    }
    fun resetViewModel() {
        _authenticationData.value = null
        paymentState = MutableStateFlow<BasicState>(BasicState.Idle)
        payableState = MutableStateFlow<BasicState>(BasicState.Idle)
    }


    data class AuthenticationData(
        val billNum: String,
        val imageData: String?
    )
    fun getFaceResponse(): MutableLiveData<IdentyResponse?> {
        return faceResponse
    }
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

    fun getPayable() {
        viewModelScope.launch {
           // _payableState.value = BasicState.Loading
            try {
                val result = authRepo.getPayable(Constants.DEVICE_ID)
                if (result.isSuccess && result.getOrNull()?.success == true) {
                    newPayable = result.getOrNull()?.data
                    _payableState.value = BasicState.Success
                } else {
                    _payableState.value = BasicState.Error("Failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _payableState.value = BasicState.Error("Error: ${e.message}")
            }
        }
    }

    fun isPayableIdle(): Boolean =
        payableState.value == BasicState.Idle || payableState.value is BasicState.Error

    fun initFaceSdk(context: Activity) {

        try {
            TccFace.newInstance(context, Constants.FACE_API_KEY, Constants.FACE_SECRET_KEY, object :
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

                        Log.e("error","="+e.localizedMessage)
                        //  errorResponse.postValue(e.localizedMessage)
                    }
                }

                override fun onInitFailed() {
                    errorResponse.postValue("License error")
                    Log.e("error","="+"License error")

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
                            Log.e("error","="+"transactionId")

                            /*
                            Log.d(
                                TAG,
                                "onResponse: transactionId $transactionId"
                            )

                             */
                            //    SavedData.faceTransactionId = transactionId
                            faceResponse.postValue(identyResponse)
                        }
                    }

                    override fun onErrorResponse(
                        identyError: com.identy.face.IdentyError,
                        hashSet: HashSet<String?>?
                    ) {
                        errorResponse.postValue(identyError.getMessage())
                    }
                },
                false,
                false
            )
        } catch (e: java.lang.Exception) {
            Log.e("error","="+e.localizedMessage)

            errorResponse.postValue(e.localizedMessage)
        }

    }

}
