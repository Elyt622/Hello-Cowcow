package com.example.hellocowcow.domain.repositories

import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.recovery.CowCowUnstakePreview

interface CowCowUnstakePreviewRepository {
  suspend fun preview(
    account: DomainAccount,
    cowNonces: List<String>
  ): CowCowUnstakePreview
}
