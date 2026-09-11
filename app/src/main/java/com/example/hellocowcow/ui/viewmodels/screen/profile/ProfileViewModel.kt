package com.example.hellocowcow.ui.viewmodels.screen.profile

import androidx.lifecycle.viewModelScope
import com.example.hellocowcow.app.module.BaseViewModel
import com.example.hellocowcow.core.config.CowCowConfig
import com.example.hellocowcow.core.wallet.MvxSignTransactionResultParser
import com.example.hellocowcow.core.wallet.WalletClient
import com.example.hellocowcow.core.wallet.WalletEvent
import com.example.hellocowcow.data.retrofit.mvxApi.request.Reward
import com.example.hellocowcow.data.retrofit.mvxApi.request.Transaction
import com.example.hellocowcow.data.transaction.ClaimTransactionFactory
import com.example.hellocowcow.data.transaction.withWalletResult
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.DomainTransaction
import com.example.hellocowcow.domain.repositories.NftRepository
import com.example.hellocowcow.domain.repositories.TransactionRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reown.util.bytesToHex
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ipfs.multibase.binary.Base64
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val nftRepository: NftRepository,
  private val transactionRepository: TransactionRepository,
  private val walletClient: WalletClient
) : BaseViewModel() {

  private var address: String = ""
  private var pendingClaimTransaction: Transaction? = null
  private var pendingClaimRequestId: Long? = null

  private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

  private val _uiStateTx: MutableStateFlow<UiStateTx> = MutableStateFlow(UiStateTx.NoData)
  val uiStateTx: StateFlow<UiStateTx> = _uiStateTx

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
  val uiState: StateFlow<UiState> = _uiState

  sealed class UiStateTx {
    data object NoData : UiStateTx()
    data object AwaitingSignature : UiStateTx()
    data object Broadcasting : UiStateTx()
    data class Send(val tx: DomainTransaction) : UiStateTx()
    data class Error(val error: String) : UiStateTx()
  }

  sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val error: String) : UiState()
  }

  init {
    observeWalletEvents()
  }

  fun load(address: String) {
    this.address = address
    getUnclaimedMooveForUser()
  }

  fun requestClaimRewards(
    account: DomainAccount,
    topic: String
  ) {
    val transaction = runCatching {
      ClaimTransactionFactory.create(account)
    }.getOrElse { error ->
      _uiStateTx.value = UiStateTx.Error(error.message ?: "Unable to build claim transaction")
      return
    }

    pendingClaimTransaction = transaction
    pendingClaimRequestId = null
    _uiStateTx.value = UiStateTx.AwaitingSignature

    walletClient.requestTransactionSignature(
      sessionTopic = topic,
      paramsJson = gson.toJson(mapOf("transaction" to transaction)),
      onSent = { requestId ->
        pendingClaimRequestId = requestId
      },
      onError = { message ->
        failClaim(message)
      }
    )
  }

  private fun observeWalletEvents() {
    viewModelScope.launch {
      walletClient.events.collect { event ->
        when (event) {
          is WalletEvent.TransactionSignatureResult -> {
            handleTransactionSignatureResult(event)
          }

          is WalletEvent.TransactionSignatureError -> {
            if (matchesPendingRequest(event.requestId)) {
              failClaim("xPortal rejected the transaction: ${event.message}")
            }
          }

          is WalletEvent.RequestExpired -> {
            if (matchesPendingRequest(event.requestId)) {
              failClaim("The xPortal signing request expired")
            }
          }

          is WalletEvent.ConnectionError -> {
            if (pendingClaimTransaction != null) {
              failClaim(event.message)
            }
          }
        }
      }
    }
  }

  private fun handleTransactionSignatureResult(
    event: WalletEvent.TransactionSignatureResult
  ) {
    val transaction = pendingClaimTransaction ?: return
    if (!matchesPendingRequest(event.requestId)) return

    MvxSignTransactionResultParser.parse(event.payload)
      .onSuccess { walletResult ->
        broadcast(transaction.withWalletResult(walletResult))
      }
      .onFailure { error ->
        failClaim(error.message ?: "Invalid response from xPortal")
      }
  }

  private fun matchesPendingRequest(requestId: Long): Boolean {
    val expectedRequestId = pendingClaimRequestId
    return pendingClaimTransaction != null &&
        (expectedRequestId == null || expectedRequestId == requestId)
  }

  private fun broadcast(transaction: Transaction) {
    if (transaction.signature.isNullOrBlank()) {
      failClaim("xPortal did not return a transaction signature")
      return
    }

    _uiStateTx.value = UiStateTx.Broadcasting

    viewModelScope.launch {
      runCatching {
        transactionRepository.sendTransaction(transaction)
      }.onSuccess { tx ->
        clearPendingClaim()
        _uiStateTx.value = UiStateTx.Send(tx)
      }.onFailure { error ->
        failClaim(error.message ?: "Unable to broadcast transaction")
      }
    }
  }

  private fun failClaim(message: String) {
    clearPendingClaim()
    _uiStateTx.value = UiStateTx.Error(message)
  }

  private fun clearPendingClaim() {
    pendingClaimTransaction = null
    pendingClaimRequestId = null
  }

  fun getUnclaimedMooveForUser() {
    getAllDataForUser()
      .map { base64Data -> extractData(base64Data) }
      .map { hex -> hex.toBigInteger(16).toBigDecimal(18) }
      .map { decimalValue -> decimalValue.setScale(4, RoundingMode.HALF_UP) }
      .subscribeBy(
        onNext = { data ->
          _uiState.value = UiState.Success(data.toEngineeringString())
        },
        onError = { error ->
          _uiState.value = UiState.Error(error.message.toString())
        }
      ).addTo(disposable)
  }

  private fun extractData(
    base64Data: String
  ): String {
    var matchedValue = ""
    val regex =
      "B[0-9w-z+/][A-Za-z0-9+/]{8}A|C[A-P][A-Za-z0-9+/]{9}AA|C[Q-Za-f][A-Za-z0-9+/]{10}AA|C[g-v][A-Za-z0-9+/]{11}AA"
    val dataLength = base64Data.length
    val matches = Pattern
      .compile(regex)
      .matcher(base64Data.substring(dataLength - 35, dataLength))
    if (matches.find()) {
      matchedValue = matches.group()
    }
    return Base64
      .decodeBase64(matchedValue)
      .bytesToHex()
      .substring(2)
  }

  private fun getAllDataForUser(): Observable<String> =
    nftRepository.getAllDataUsers(
      Reward(
        CowCowConfig.REWARDS_CONTRACT,
        "getAllDataForUser",
        "0",
        arrayListOf(),
        address
      )
    ).map { it.returnData[0] }
}
