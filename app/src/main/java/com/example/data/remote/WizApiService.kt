package com.example.data.remote

import com.example.data.model.ApiResponse
import com.example.data.model.BrightnessRequest
import com.example.data.model.ColorRequest
import com.example.data.model.DiscoveryData
import com.example.data.model.IpRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WizApiService {

    @GET("api/discover")
    suspend fun discover(): Response<ApiResponse<DiscoveryData>>

    @GET("api/bulbs")
    suspend fun getBulbs(): Response<ResponseBody>

    @GET("api/states")
    suspend fun getStates(): Response<ResponseBody>

    @GET("api/state/{ip}")
    suspend fun getState(@Path("ip") ip: String): Response<ResponseBody>

    @POST("api/on")
    suspend fun turnOn(@Body body: IpRequest): Response<ApiResponse<Any>>

    @POST("api/off")
    suspend fun turnOff(@Body body: IpRequest): Response<ApiResponse<Any>>

    @POST("api/toggle")
    suspend fun toggle(@Body body: IpRequest): Response<ApiResponse<Any>>

    @POST("api/brightness")
    suspend fun setBrightness(@Body body: BrightnessRequest): Response<ApiResponse<Any>>

    @POST("api/color")
    suspend fun setColor(@Body body: ColorRequest): Response<ApiResponse<Any>>

    @POST("api/broadcast/on")
    suspend fun broadcastOn(): Response<ApiResponse<Any>>

    @POST("api/broadcast/off")
    suspend fun broadcastOff(): Response<ApiResponse<Any>>

    @GET("api/health")
    suspend fun checkHealth(): Response<ResponseBody>

    @GET("/")
    suspend fun getRootInfo(): Response<ResponseBody>
}
