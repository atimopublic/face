package com.tcc.face.remote.api


import com.tcc.face.base.websocket.Trigger
import com.tcc.face.base.websocket.WebSocketMessage
import com.tcc.face.domain.models.BasicResponse
import com.tcc.face.domain.models.CardRequest
import com.tcc.face.domain.models.CardResponse
import com.tcc.face.domain.models.LoginRequest
import com.tcc.face.domain.models.PaymentAuthenticationRequest
import com.tcc.face.domain.models.PaymentAuthenticationResponse
import com.tcc.face.domain.models.SignUpRequest
import com.tcc.face.domain.models.TransactionResponse
import com.tcc.face.domain.models.User
import retrofit2.Response
import retrofit2.http.*

// sub-domain
interface ApiService {

        @POST("/api/customer/login")
        suspend fun login(@Body loginRequest: LoginRequest): Response<BasicResponse<User>>  // Define your response model appropriately

        @POST("/api/customer/create")
        suspend fun signup(@Body loginRequest: SignUpRequest): Response<BasicResponse<User>>

        @POST("/api/paymentCard/create")
        suspend fun addCard(@Body cardRequest: CardRequest): Response<BasicResponse<CardResponse>>  // Define your response model appropriately

        @GET("/api/transaction/getbycustomer")
        suspend fun getTransaction(@Query("Id") id: String): Response<BasicResponse<List<TransactionResponse>>>  // Define your response model appropriately

        @POST("/api/transaction/biometric-verification-and-payment")
        suspend fun authenticatePayment(@Body authenticationRequest: PaymentAuthenticationRequest): Response<BasicResponse<PaymentAuthenticationResponse>>  // Define your response model appropriately

        @GET("/api/trigger/getlive")
        suspend fun getPayable(@Query("Id") Id: String): Response<BasicResponse<Trigger>>  // Define your response model appropriately


}

    /*
    @FormUrlEncoded
    @POST("app/mobile-versions/check-update")
    suspend fun checkVersion(
        @FieldMap dto: HashMap<String, String>,
    ): GeneralResponseModel<JsonObject>

     */




