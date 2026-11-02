package com.example.data.remote

import com.example.data.model.PlanespottersResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface PlanespottersApiService {

    @GET("pub/photos/reg/{registration}")
    suspend fun getPhotosByRegistration(
        @Path("registration") registration: String
    ): PlanespottersResponse

    @GET("pub/photos/ac_type/{icaoCode}")
    suspend fun getPhotosByAircraftType(
        @Path("icaoCode") icaoCode: String
    ): PlanespottersResponse
}
