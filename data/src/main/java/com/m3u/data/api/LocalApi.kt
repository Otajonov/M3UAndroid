package com.m3u.data.api

import com.m3u.core.architecture.logger.Logger
import com.m3u.core.architecture.logger.execute
import com.m3u.data.local.http.endpoint.Playlists
import com.m3u.data.local.http.endpoint.SayHello
import okhttp3.HttpUrl
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface LocalApi {
    @GET("/say_hello")
    suspend fun sayHello(): SayHello.Rep?

    @POST("/playlists/subscribe")
    suspend fun subscribe(
        @Query("title") title: String,
        @Query("url") url: String
    ): Playlists.SubscribeRep?
}

@Singleton
class LocalService @Inject constructor(
    private val builder: Retrofit.Builder,
    @Logger.Message private val logger: Logger
) : LocalApi {
    override suspend fun sayHello(): SayHello.Rep? = logger.execute { api?.sayHello() }
    override suspend fun subscribe(
        title: String,
        url: String
    ): Playlists.SubscribeRep? = logger.execute {
        api?.subscribe(title, url)
    }

    private var api: LocalApi? = null

    fun prepare(host: String, port: Int) {
        val baseUrl = HttpUrl.Builder()
            .scheme("http")
            .host(host)
            .port(port)
            .build()
        api = builder
            .baseUrl(baseUrl)
            .build()
            .create()
    }

    fun close() {
        api = null
    }
}
