package com.tcc.face.feature.ui.fragments

import com.google.gson.Gson
import com.tcc.face.remote.api.ApiService
import com.tcc.face.domain.models.BasicResponse
import com.tcc.face.domain.models.CardRequest
import com.tcc.face.domain.models.CardResponse
import com.tcc.face.domain.models.Error
import com.tcc.face.domain.models.LoginRequest
import com.tcc.face.domain.models.PaymentAuthenticationRequest
import com.tcc.face.domain.models.PaymentAuthenticationResponse
import com.tcc.face.domain.models.SignUpRequest
import com.tcc.face.domain.models.TransactionResponse
import com.tcc.face.domain.models.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun login(request: LoginRequest): Result<BasicResponse<User>?> {
        return try {
            val response = apiService.login(request) // Assuming your API has a login endpoint
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.toString().contains("503")) {
                Result.failure(Exception("Server is down"))
            } else if (response.toString().contains("400")) {
                Result.failure(Exception("Bad request"))
            } else if (response.toString().contains("500")) {
                Result.failure(Exception("Internal server error"))
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(request: SignUpRequest): Result<BasicResponse<User>?> {
        return try {
            val response = apiService.signup(request) // Assuming your API has a login endpoint
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.toString().contains("503")) {
                Result.failure(Exception("Server is down"))
            } else if (response.toString().contains("400")) {
                Result.failure(Exception("Bad request"))
            } else if (response.toString().contains("500")) {
                Result.failure(Exception("Internal server error"))
            } else {
                Result.failure(Exception("Signup failed"))
            }
        } catch (e: Exception) {
            print(e.stackTrace)
            Result.failure(e)
        }
    }

    suspend fun addCard(request: CardRequest): Result<BasicResponse<CardResponse>?> =
        try {
            val response = apiService.addCard(request)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.toString().contains("503")) {
                Result.failure(Exception("Server is down"))
            } else if (response.toString().contains("400")) {
                Result.failure(Exception("Bad request"))
            } else if (response.toString().contains("500")) {
                Result.failure(Exception("Internal server error"))
            } else {
                Result.failure(Exception("Failed to add card information"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun getTransactionByCustomer(id: String): Result<BasicResponse<List<TransactionResponse>>?> =
        try {
            val response = apiService.getTransaction(id)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.toString().contains("503")) {
                Result.failure(Exception("Server is down"))
            } else if (response.toString().contains("400")) {
                Result.failure(Exception("Bad request"))
            } else if (response.toString().contains("500")) {
                Result.failure(Exception("Internal server error"))
            } else {
                Result.failure(Exception("Failed to add card information"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun authenticatePayment(paymentRequest: PaymentAuthenticationRequest): Result<BasicResponse<PaymentAuthenticationResponse>?> =
        try {
            val response = apiService.authenticatePayment(paymentRequest)

            if (response.isSuccessful) {
                Result.success(response.body())
            } else {

                val gson = Gson()
                val error: Error = gson.fromJson(response?.errorBody()?.string(), Error::class.java)
                Result.failure(Exception("${error.errorCode} (Payment Failed) ${error.errorMessage}"))
            }
            /*
            else if (response.toString().contains("503")) {
                Result.failure(Exception("Server is down"))
            } else if (response.toString().contains("400")) {
                Result.failure(Exception("$errorCode Bad request $errorMessage"))
            } else if (response.toString().contains("500")) {
                Result.failure(Exception("Internal server error"))
            } else {
                Result.failure(Exception("$errorCode Failed to process payment $errorMessage"))
            }*/
        } catch (e: Exception) {
            Result.failure(e)
        }
}