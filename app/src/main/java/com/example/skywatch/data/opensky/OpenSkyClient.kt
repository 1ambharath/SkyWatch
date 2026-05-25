package com.example.skywatch.data.opensky

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object OpenSkyClient {
    private const val BaseUrl = "https://opensky-network.org/api/"
    
    // Credentials provided for the 1amsam-api-client
    private const val Username = "1amsam"
    private const val Password = "V5yLgwu*Mqu38L@"

    fun createApi(): OpenSkyApi {
        val logging =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

        val okHttp =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val authenticatedRequest = chain.request().newBuilder()
                        .header("Authorization", Credentials.basic(Username, Password))
                        .build()
                    chain.proceed(authenticatedRequest)
                }
                .build()

        val moshi =
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(BaseUrl)
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

        return retrofit.create(OpenSkyApi::class.java)
    }
}
