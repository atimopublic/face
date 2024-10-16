package com.tcc.face.domain.models

data class BasicResponse<T>(
    var data : T,
    val error: Error,
    val success: Boolean
)