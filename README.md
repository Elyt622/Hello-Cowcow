# Hello CowCow 2026

Hello CowCow is an Android companion app for the CowCow ecosystem on **MultiversX**.

The project started as a 2023/2024 NFT portfolio and WalletConnect experiment. The 2026 rebuild turns it into a modern, public-first CowCow dashboard with collection intelligence, xPortal-powered portfolio actions, NFT details, MOOVE rewards and a recovery path for holders still locked in the legacy staking contract.

> The original application is preserved unchanged on the [`legacy-2024`](../../tree/legacy-2024) branch.

## Product

### Explore without a wallet

The app opens directly on public CowCow data. A wallet connection is no longer required to browse the ecosystem.

- collection floor and market signals
- staking and listing metrics
- recent activity
- direct access to Collection and Portfolio

### Collection intelligence

Collection data is backed by the public **XOXNO API** instead of scraping XOXNO's Next.js internals.

- floor price
- holders and listings
- staking metrics
- ATH / volume / trades
- MOOVE ecosystem statistics

### xPortal portfolio

Connect xPortal only when personal wallet data is required.

- Owned / Staked / Listed CowCows
- instant search by name or NFT identifier
- rarity-rank sorting
- adaptive NFT grids
- MOOVE rewards and claim flow
- explicit wallet signing states

### NFT detail

Each CowCow has a dedicated asset page with:

- artwork / upgraded artwork
- identifier and rarity rank
- collection floor context
- sale status
- attributes and trait floors
- marketplace offers

### MOOVE rewards

The claim flow is one of the technical centerpieces of the project.

- typed `mvx_signTransaction` parsing
- Reown Sign isolated behind `WalletClient`
- guarded-account support
- explicit Awaiting signature / Broadcasting / Success / Error states
- MultiversX broadcast through coroutine-based repositories
- reward decoding isolated and tested

### Emergency Recovery — in progress

The 2026 app also contains a dedicated recovery workflow for CowCows still locked in the legacy staking contract.

Current implementation:

- reads the user's outstanding MOOVE rewards
- reads the user's MOOVE balance
- reads the staking contract MOOVE balance
- calculates how much MOOVE the wallet must acquire
- keeps the contract balance informational only — it is never assumed to be reserved for the current user
- builds the protocol-level `ESDTTransfer` used to fund the CowCow staking contract
- requires explicit confirmation and xPortal signature before broadcasting

Target recovery journey:

`EGLD → MOOVE → fund staking contract → unstake → 7-day unbonding → unbond CowCows → optional MOOVE → EGLD`

The CowCow-specific unstake/unbond calls remain deliberately locked until their exact historical calldata is verified. The app never guesses a smart-contract endpoint that moves user assets.

## 2026 Android stack

- Kotlin
- Jetpack Compose / Material 3
- Navigation 3
- Single Activity
- Hilt
- Coroutines / StateFlow
- Lifecycle-aware Compose state collection
- Retrofit
- Reown Sign / WalletConnect protocol
- MultiversX public API
- XOXNO public API
- Glide Compose
- JDK 17
- AGP 9.2 / Gradle 9.4
- compileSdk 37

Some legacy read-only features still use RxJava while they are migrated incrementally. Financial and wallet-critical flows are coroutine-native.

## Architecture

The rebuild intentionally avoids a multi-module architecture for its own sake. The app currently stays in one Gradle module with explicit package boundaries:

```text
app/          application + DI
core/         config, wallet and shared infrastructure
data/         network DTOs, API clients and repository implementations
domain/       app models, repository contracts and transaction builders
ui/           Navigation 3 shell, screens, Compose components and ViewModels
```

Key rules introduced in the 2026 rebuild:

- UI/ViewModels do not depend directly on Reown `SignClient`
- data repositories do not depend on UI schedulers
- Retrofit DTOs stay out of the modern transaction domain
- CowCow/MOOVE contract constants are centralized
- transaction builders are isolated and unit tested
- wallet side effects are explicit rather than triggered during composition
- wallet connection is a feature state, not an application launch requirement

## Responsive UI

Hello CowCow now adapts its navigation to the available width:

- compact devices: Material 3 bottom navigation
- expanded/tablet layouts: Navigation Rail
- NFT grids use adaptive columns instead of fixed phone-only dimensions

## Quality gates

GitHub Actions validates the project with:

```text
testDebugUnitTest
assembleDebug
lintDebug
```

Tests currently cover critical paths such as:

- XOXNO mappings
- wallet session transitions
- NFT detail loading
- guarded and non-guarded claim transactions
- xPortal transaction response parsing
- MOOVE reward decoding
- recovery funding calculations
- MOOVE `ESDTTransfer` transaction construction

## Branches

- `main` — Hello CowCow 2026
- `legacy-2024` — original application preserved unchanged
- `hello-cowcow-2026-recovery` — active emergency-recovery work

## Build

A WalletConnect/Reown project id can be supplied through `local.properties`:

```properties
wallet.connect.id=YOUR_PROJECT_ID
```

The project is also configured so CI can build without a developer-local `local.properties` file.

## Direction

Hello CowCow is no longer intended to be only an NFT viewer. The direction is a focused CowCow companion app that combines:

**public discovery → collection intelligence → portfolio → asset detail → rewards → safe on-chain recovery actions**.

---

Built as an Android / MultiversX engineering project around real wallet, marketplace and smart-contract constraints.
