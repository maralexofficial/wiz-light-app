package com.example.data.remote

import com.example.data.model.ApiResponse
import com.example.data.model.BrightnessRequest
import com.example.data.model.ColorRequest
import com.example.data.model.CreateGroupRequest
import com.example.data.model.CreatePresetRequest
import com.example.data.model.DiscoveryData
import com.example.data.model.GroupBrightnessRequest
import com.example.data.model.GroupTemperatureRequest
import com.example.data.model.IpRequest
import com.example.data.model.SyncData
import com.example.data.model.UpdateGroupRequest
import com.example.data.model.UpdatePresetRequest
import com.example.data.model.WizGroupDto
import com.example.data.model.WizPresetDto
import com.example.data.model.WizRoomDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    // --- Rooms Endpoints ---
    @GET("api/rooms")
    suspend fun getRooms(): Response<ResponseBody>

    @GET("api/rooms/{roomId}/groups")
    suspend fun getRoomGroups(@Path("roomId") roomId: Long): Response<ResponseBody>

    // --- Groups Endpoints ---
    @GET("api/groups")
    suspend fun getGroups(): Response<ResponseBody>

    @GET("api/groups/{id}")
    suspend fun getGroup(@Path("id") id: String): Response<ResponseBody>

    @POST("api/groups")
    suspend fun createGroup(@Body body: CreateGroupRequest): Response<ResponseBody>

    @PATCH("api/groups/{id}")
    suspend fun updateGroup(@Path("id") id: String, @Body body: UpdateGroupRequest): Response<ResponseBody>

    @DELETE("api/groups/{id}")
    suspend fun deleteGroup(@Path("id") id: String): Response<ResponseBody>

    // --- Group Actions ---
    @POST("api/groups/{id}/on")
    suspend fun groupOn(@Path("id") id: String): Response<ResponseBody>

    @POST("api/groups/{id}/off")
    suspend fun groupOff(@Path("id") id: String): Response<ResponseBody>

    @POST("api/groups/{id}/brightness")
    suspend fun groupBrightness(@Path("id") id: String, @Body body: GroupBrightnessRequest): Response<ResponseBody>

    @POST("api/groups/{id}/temperature")
    suspend fun groupTemperature(@Path("id") id: String, @Body body: GroupTemperatureRequest): Response<ResponseBody>

    // --- Synchronization ---
    @GET("api/sync")
    suspend fun getSync(): Response<ResponseBody>

    // --- Presets Endpoints ---
    @GET("api/presets")
    suspend fun getPresets(): Response<ResponseBody>

    @GET("api/presets/{id}")
    suspend fun getPreset(@Path("id") id: String): Response<ResponseBody>

    @POST("api/presets")
    suspend fun createPreset(@Body body: CreatePresetRequest): Response<ResponseBody>

    @PATCH("api/presets/{id}")
    suspend fun updatePreset(@Path("id") id: String, @Body body: UpdatePresetRequest): Response<ResponseBody>

    @DELETE("api/presets/{id}")
    suspend fun deletePreset(@Path("id") id: String): Response<ResponseBody>

    @POST("api/presets/{id}/apply")
    suspend fun applyPreset(@Path("id") id: String): Response<ResponseBody>
}
