# CowCow Recovery protocol evidence

This document separates facts that are verified from the legacy Hello CowCow app and historical successful MultiversX transactions from recovery behavior that is still unverified. Recovery code must not invent endpoint names, argument encoding, gas limits or timing.

## Verified contract

CowCow staking / rewards contract:

`erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk`

The same address is used throughout the legacy Android app for staking data and MOOVE rewards and is the receiver of the verified historical unstake transaction below.

## Verified read paths

### `getAllDataForUser`

Legacy source:

`app/src/main/java/com/example/hellocowcow/ui/viewmodels/screen/profile/StakeViewModel.kt`

The legacy app queries `getAllDataForUser` against the CowCow contract and decodes the returned data to recover the user's staked Cow identifiers.

The same view is also used by the legacy Profile flow to decode claimable MOOVE for the connected address.

### `getTotalRewardsToCollect`

Legacy source:

`app/src/main/java/com/example/hellocowcow/ui/viewmodels/screen/stats/TokenViewModel.kt`

The legacy Stats flow queries `getTotalRewardsToCollect` against the same contract.

## Verified write path: `claimRewards`

Legacy source:

`app/src/main/java/com/example/hellocowcow/ui/viewmodels/screen/profile/ProfileViewModel.kt`

The legacy app builds and signs a transaction with:

- receiver: CowCow staking contract
- value: `0`
- gas price: `1_000_000_000`
- gas limit: `30_000_000`
- chain ID: `1`
- data: `Y2xhaW1SZXdhcmRz`

`Y2xhaW1SZXdhcmRz` is the Base64 representation of `claimRewards`.

The 2026 `ClaimTransactionFactory` preserves this known legacy call while fixing signing, guarded-account and transaction-state handling.

## Verified write path: `unstake`

Historical successful mainnet transaction:

`4d1b422519ece36f560994e1a71bedb520504c5e7427c1aef38a3565392d5be4`

Observed on MultiversX Explorer on 2026-07-20:

- status: success
- receiver: CowCow staking contract
- value: `0 EGLD`
- method: `unstake`
- gas limit: `600_000_000`
- gas price: `1_000_000_000`
- payload shape: `unstake@<cow nonce hex>@<cow nonce hex>...`
- the verified transaction contains 72 CowCow nonce arguments
- nonce encoding is preserved as even-length hexadecimal, including leading zeroes such as `0181`, `0282` and `0c71`

The exact payload of the verified transaction starts with:

`unstake@0c71@114f@1261@1cd1@1ce8@15d8...`

and ends with:

`...@0643@0c34@1aa8@1eca@0c0e@0738@0372`

### Verified unstake effects

The transaction returned MOOVE directly from the CowCow staking contract to the user via an `ESDTTransfer` smart-contract result:

- token identifier hex: `4d4f4f56452d383735353339` = `MOOVE-875539`
- amount hex: `33dc7dd26e2939ce00`
- decoded amount: `956.6720341902 MOOVE`

This proves that the unstake call itself triggers the accrued MOOVE payout. The caller does **not** pass the reward amount as an unstake argument.

This is important for Recovery: if the legacy staking contract no longer holds enough MOOVE to satisfy the payout, prefunding the contract can be necessary before calling the otherwise-correct historical `unstake` endpoint.

The same transaction also produced an internal call whose additional data decodes to `setCowsAtStake`, confirming that the unstake updates CowCow staking state in another contract-side component.

### Gas note

The historical transaction used a `600_000_000` gas limit and succeeded, but emitted a `too much gas provided for processing` log. Therefore `600_000_000` is accepted as a known-working upper bound, not as a claim that this is the optimal gas limit or the actual fee consumption. Gas optimisation should use simulation or additional known-working transactions before lowering it.

## Verified 2026 Recovery behavior

The current Recovery implementation can safely:

1. read the user's CowCow/MOOVE recovery position;
2. read wallet and contract MOOVE balances;
3. calculate missing MOOVE and the recommended top-up;
4. obtain a live xExchange EGLD -> MOOVE -> EGLD round-trip quote;
5. show expected and worst-case DEX loss without pretending all network fees are known;
6. build a standard `ESDTTransfer` top-up to the CowCow contract;
7. request explicit xPortal signing;
8. broadcast the signed transaction;
9. track MultiversX process status to confirmed / failed / timeout.

The historical transaction above now additionally verifies the CowCow unstake endpoint and argument encoding.

## NOT verified yet

The following must remain locked until supported by known-working evidence:

- exact unbond endpoint name;
- exact unbond arguments and encoding;
- exact gas limit for unbond;
- authoritative unbonding duration for this deployed contract;
- how the contract exposes pending-unbond state and readiness;
- whether unbond is batched using the same nonce list or a different identifier/index scheme.

Acceptable evidence includes one of:

- verified CowCow smart-contract source / ABI;
- original staking dapp source or bundle with unambiguous transaction construction;
- historical successful MultiversX transactions against this exact contract whose calldata and effects can be inspected.

## Safety rule

Do not unlock the Recovery unbond action based only on generic MultiversX staking conventions, endpoint names from an unrelated contract, UI copy, or assumptions about the historical CowCow dapp.
