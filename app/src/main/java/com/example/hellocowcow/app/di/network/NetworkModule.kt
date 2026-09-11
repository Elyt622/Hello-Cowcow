package com.example.hellocowcow.app.di.network

import com.example.hellocowcow.data.network.api.MvxApi
import com.example.hellocowcow.data.network.api.MvxGatewayApi
import com.example.hellocowcow.data.network.api.XExchangeQuoteApi
import com.example.hellocowcow.data.network.api.XoxnoApi
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

  private const val customTimeout = 6L
  private const val maxRateLimitRetries = 2

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(customTimeout, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .addInterceptor { chain ->
      var request = chain.request()
      var response = chain.proceed(request)
      var retryCount = 0

      while (response.code == 429 && retryCount < maxRateLimitRetries) {
        val retryAfterSeconds = response.header("Retry-After")
          ?.toLongOrNull()
          ?.coerceIn(0L, 3L)
        val delayMillis = retryAfterSeconds
          ?.times(1_000L)
          ?: 750L * (retryCount + 1)

        response.close()
        if (delayMillis > 0L) Thread.sleep(delayMillis)

        retryCount += 1
        response = chain.proceed(request)
      }

      response
    }
    .build()

  @Provides
  @Singleton
  fun provideMvxApi(): MvxApi =
    Retrofit.Builder()
      .baseUrl("https://api.multiversx.com/")
      .client(httpClient)
      .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
      .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
      .build().create(MvxApi::class.java)

  @Provides
  @Singleton
  fun provideMvxGatewayApi(): MvxGatewayApi =
    Retrofit.Builder()
      .baseUrl("https://gateway.multiversx.com/")
      .client(httpClient)
      .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
      .build().create(MvxGatewayApi::class.java)

  @Provides
  @Singleton
  fun provideXExchangeQuoteApi(): XExchangeQuoteApi =
    Retrofit.Builder()
      .baseUrl("https://graph.xexchange.com/")
      .client(httpClient)
      .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
      .build().create(XExchangeQuoteApi::class.java)

  @Provides
  @Singleton
  fun provideXoxnoApi(): XoxnoApi =
    Retrofit.Builder()
      .baseUrl("https://api.xoxno.com/")
      .client(httpClient)
      .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
      .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
      .build().create(XoxnoApi::class.java)
}
