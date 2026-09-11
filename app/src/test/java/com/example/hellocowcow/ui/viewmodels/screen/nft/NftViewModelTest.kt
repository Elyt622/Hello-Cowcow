package com.example.hellocowcow.ui.viewmodels.screen.nft

import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.domain.repositories.NftDetailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NftViewModelTest {

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
  fun `loads nft detail`() = runTest(dispatcher) {
    val viewModel = NftViewModel(
      FakeNftDetailRepository(
        result = Result.success(
          DomainNft(
            identifier = "COW-cd463d-01",
            name = "Cow #1"
          )
        )
      )
    )

    viewModel.load("COW-cd463d-01")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state is NftViewModel.UiState.Success)
    state as NftViewModel.UiState.Success
    assertEquals("COW-cd463d-01", state.nft.identifier)
  }

  @Test
  fun `exposes repository error`() = runTest(dispatcher) {
    val viewModel = NftViewModel(
      FakeNftDetailRepository(
        result = Result.failure(IllegalStateException("NFT unavailable"))
      )
    )

    viewModel.load("COW-cd463d-01")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state is NftViewModel.UiState.Error)
    assertEquals(
      "NFT unavailable",
      (state as NftViewModel.UiState.Error).message
    )
  }

  private class FakeNftDetailRepository(
    private val result: Result<DomainNft>
  ) : NftDetailRepository {
    override suspend fun getNft(identifier: String): DomainNft = result.getOrThrow()
  }
}
