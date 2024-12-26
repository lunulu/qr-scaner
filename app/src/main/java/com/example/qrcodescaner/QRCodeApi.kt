package com.example.qrcodescaner

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface QRCodeApi {
    @GET("api/qrcode/{id}")
    fun getQRCodeInfo(@Path("id") id: Long): Call<QRCodeResponse>
}

data class QRCodeResponse(
    val id: Long,
    val data: String
)

object ApiClient {
    private const val BASE_URL = "https://your-api-server.com/"

    val api: QRCodeApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(QRCodeApi::class.java)
    }
}