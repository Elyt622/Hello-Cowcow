package com.example.hellocowcow.domain.recovery

object RecoveryEvidence {

  /**
   * Conservative evidence threshold, not the deployed contract's proven minimum.
   *
   * The same four CowCow nonces were unstaked at 2026-07-13 13:38:36 UTC and
   * successfully final-claimed at 2026-07-20 14:25:24 UTC: exactly
   * 7 days + 46 minutes + 48 seconds later.
   *
   * Until the deployed ABI/source or a tighter successful transaction proves the
   * exact minimum, Recovery waits at least this observed successful delay before
   * presenting a pending batch as historically safe to claim.
   */
  const val OBSERVED_SUCCESSFUL_FINAL_CLAIM_DELAY_SECONDS =
    7L * 24L * 60L * 60L + 46L * 60L + 48L
}
