package com.sora.android.data.remote

import com.sora.android.domain.model.Message
import retrofit2.http.GET

interface ApiService {
    
    @GET("api/messages/hello")
    suspend fun getHelloWorld(): Message
}