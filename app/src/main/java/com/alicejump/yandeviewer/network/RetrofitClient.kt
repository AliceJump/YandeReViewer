package com.alicejump.yandeviewer.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val api: YandeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://yande.re/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YandeApi::class.java)
    }
}
