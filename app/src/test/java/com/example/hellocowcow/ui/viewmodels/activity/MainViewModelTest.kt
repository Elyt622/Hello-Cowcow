package com.example.hellocowcow.ui.viewmodels.activity

import com.example.hellocowcow.core.wallet.WalletClient
import com.example.hellocowcow.core.wallet.WalletEvent
import com.example.hellocowcow.core.wallet.WalletSession
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.repositories.AccountRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `starts disconnected when there is no active wallet session`() = runTest(dispatcher) {
    val wallet = FakeWalletClient()
    val viewModel = MainViewModel(FakeAccountRepository(), wallet)

    advanceUntilIdle()

    assertEquals(MainViewModel.WalletUiState.Disconnected, viewModel.walletState.value)
  }

  @Test
  fun `restores an existing wallet session and account`() = runTest(dispatcher) {
    val wallet = FakeWalletClient().apply {
      activeSession = WalletSession(ADDRESS, TOPIC)
    }
    val viewModel = MainViewModel(FakeAccountRepository(), wallet)

    advanceUntilIdle()

    val state = viewModel.walletState.value
    assertTrue(state is MainViewModel.WalletUiState.Connected)
    state as MainViewModel.WalletUiState.Connected
    assertEquals(ADDRESS, state.account.address)
    assertEquals(TOPIC, state.topic)
  }

  @Test
  fun `connect request exposes the pairing uri for xPortal`() = runTest(dispatcher) {
    val wallet = FakeWalletClient()
    val viewModel = MainViewModel(FakeAccountRepository(), wallet)
    advanceUntilIdle()

    val effect = async(start = CoroutineStart.UNDISPATCHED) {
      viewModel.effects.first()
    }

    viewModel.connectWallet()
    assertEquals(MainViewModel.WalletUiState.Connecting, viewModel.walletState.value)

    wallet.completeConnection("wc:test-pairing")

    assertEquals(
      MainViewModel.UiEffect.OpenXPortal("wc:test-pairing"),
      effect.await()
    )
  }

  @Test
  fun `approved xPortal session loads the connected account`() = runTest(dispatcher) {
    val wallet = FakeWalletClient()
    val viewModel = MainViewModel(FakeAccountRepository(), wallet)
    advanceUntilIdle()

    wallet.emit(WalletEvent.SessionApproved(WalletSession(ADDRESS, TOPIC)))
    advanceUntilIdle()

    val state = viewModel.walletState.value
    assertTrue(state is MainViewModel.WalletUiState.Connected)
    state as MainViewModel.WalletUiState.Connected
    assertEquals(ADDRESS, state.account.address)
    assertEquals(TOPIC, state.topic)
  }

  @Test
  fun `wallet session deletion disconnects the portfolio`() = runTest(dispatcher) {
    val wallet = FakeWalletClient().apply {
      activeSession = WalletSession(ADDRESS, TOPIC)
    }
    val viewModel = MainViewModel(FakeAccountRepository(), wallet)
    advanceUntilIdle()

    wallet.emit(WalletEvent.SessionDisconnected(TOPIC))
    advanceUntilIdle()

    assertEquals(MainViewModel.WalletUiState.Disconnected, viewModel.walletState.value)
  }

  private class FakeAccountRepository : AccountRepository {
    override suspend fun getAccount(address: String): DomainAccount = DomainAccount(
      address = address,
      username = "cow.elrond"
    )
  }

  private class FakeWalletClient : WalletClient {
    private val mutableEvents = MutableSharedFlow<WalletEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<WalletEvent> = mutableEvents.asSharedFlow()

    var activeSession: WalletSession? = null
    private var connectSuccess: ((String) -> Unit)? = null

    override suspend fun getActiveSession(): WalletSession? = activeSession

    override fun connect(
      pairingTopicPosition: Int,
      sessionExpiryEpochSeconds: Long,
      onSuccess: (String) -> Unit,
      onError: (String) -> Unit
    ) {
      connectSuccess = onSuccess
    }

    override fun requestTransactionSignature(
      sessionTopic: String,
      paramsJson: String,
      onSent: (Long) -> Unit,
      onError: (String) -> Unit
    ) = Unit

    fun completeConnection(uri: String) {
      connectSuccess?.invoke(uri)
    }

    suspend fun emit(event: WalletEvent) {
      mutableEvents.emit(event)
    }
  }

  private companion object {
    const val ADDRESS = "erd1cowcowtest"
    const val TOPIC = "wallet-session-topic"
  }
}
