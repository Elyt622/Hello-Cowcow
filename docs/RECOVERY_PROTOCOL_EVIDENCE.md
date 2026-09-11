# CowCow Recovery protocol evidence

This document separates facts that are verified from the legacy Hello CowCow app from recovery behavior that is still unverified. Recovery code must not invent endpoint names, argument encoding, gas limits or timing.

## Verified contract

CowCow staking / rewards contract:

`erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk`

The same address is used throughout the legacy Android app for staking data and MOOVE rewards.

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

## Verified 2026 Recovery behavior

The current Recovery implementation can safely:

1. read the user's CowCow/MOOVE recovery position;
2. read wallet and contract MOOVE balances;
3. calculate missing MOOVE and the recommended top-up;
4. obtain a live xExchange EGLD -> MOOVE -> EGLD round-trip quote;
5. show expected and worst-case DEX loss without pretending network fees are known;
6. build a standard `ESDTTransfer` top-up to the CowCow contract;
7. request explicit xPortal signing;
8. broadcast the signed transaction;
9. track MultiversX process status to confirmed / failed / timeout.

## NOT verified yet

No version of `StakeViewModel` found in the 2023/2024 Git history contains a known-working unstake or unbond transaction builder.

The following must therefore remain locked until supported by known-working evidence:

- exact unstake endpoint name;
- exact unstake argument order and binary/hex encoding;
- whether rewards are returned as a direct argument, ESDT payment or another contract-side mechanism;
- exact unbond endpoint name;
- exact unbond arguments;
- exact gas limits for unstake and unbond;
- authoritative unbonding duration for this deployed contract;
- any per-NFT vs batched transaction semantics.

Acceptable evidence includes one of:

- verified CowCow smart-contract source / ABI;
- original staking dapp source or bundle with unambiguous transaction construction;
- historical successful MultiversX transactions against this exact contract whose calldata and effects can be inspected.

## Safety rule

Do not unlock Recovery unstake/unbond actions based only on generic MultiversX staking conventions, endpoint names from an unrelated contract, UI copy, or assumptions about the historical CowCow dapp.
