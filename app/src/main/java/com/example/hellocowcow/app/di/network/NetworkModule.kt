package com.example.hellocowcow.app.di.network

import com.example.hellocowcow.datas.network.api.CCToolsApi
import com.example.hellocowcow.datas.network.api.XoxnoApi
import com.example.hellocowcow.datas.network.api.MvxApi
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Provides
    @Singleton
    fun provideMvxApi() : MvxApi =
        Retrofit.Builder()
            .baseUrl("https://api.multiversx.com/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build().create(MvxApi::class.java)

    @Provides
    @Singleton
    fun provideXoxnoApi() : XoxnoApi =
        Retrofit.Builder()
            .baseUrl("https://proxy-api.xoxno.com/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build().create(XoxnoApi::class.java)

    @Provides
    @Singleton
    fun provideCCToolsApi() : CCToolsApi =
        Retrofit.Builder()
            .baseUrl("https://api.cowcowtools.com/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build().create(CCToolsApi::class.java)

}