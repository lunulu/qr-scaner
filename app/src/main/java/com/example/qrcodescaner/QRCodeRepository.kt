package com.example.qrcodescaner

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QRCodeRepository {
    fun fetchQRCodeData(id: Long, onSuccess: (QRCodeResponse) -> Unit, onError: (String) -> Unit) {
        ApiClient.api.getQRCodeInfo(id).enqueue(object : Callback<QRCodeResponse> {
            override fun onResponse(call: Call<QRCodeResponse>, response: Response<QRCodeResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it) }
                } else {
                    onError("Server error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<QRCodeResponse>, t: Throwable) {
                onError("Request failed: ${t.message}")
            }
        })
    }
}
