package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.RecoverySnapshot

interface RecoveryRepository {
  suspend fun getSnapshot(address: String): RecoverySnapshot
}
