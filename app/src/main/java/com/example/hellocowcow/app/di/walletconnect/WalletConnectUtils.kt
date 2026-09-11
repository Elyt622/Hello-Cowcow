package com.example.hellocowcow.app.di.walletconnect

import com.example.hellocowcow.core.wallet.ReownWalletClient
import com.example.hellocowcow.core.wallet.WalletClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WalletConnectModule {

  @Binds
  @Singleton
  abstract fun bindWalletClient(
    walletClient: ReownWalletClient
  ): WalletClient
}
