# Claim-first Recovery cost model

The Recovery cost estimate must measure the cost of unlocking the recovery path, not treat the user's accrued MOOVE rewards as a loss.

## Liquidity model

Let:

- `R` = claimable MOOVE rewards
- `C` = current MOOVE balance of the CowCow staking contract
- `W` = MOOVE already held by the connected wallet

Then:

- contract claim liquidity gap = `max(R - C, 0)`
- temporary MOOVE that must be bought = `max(R - C - W, 0)`

Only the temporary MOOVE purchase is quoted as external capital on xExchange.

All MOOVE liquidity arithmetic keeps the token's full 18-decimal precision. Display formatting may round values for readability, but top-up construction and comparisons use the exact decoded amount so presentation rounding cannot leave the legacy contract fractionally underfunded.

## EGLD loss model

For the temporary MOOVE amount, Recovery requests live xExchange quotes for:

1. fixed-output `EGLD -> MOOVE` buy cost;
2. fixed-input `MOOVE -> EGLD` expected sell-back;
3. minimum sell-back at the configured tolerance.

The cost model separates:

- temporary EGLD capital;
- expected DEX friction;
- worst-case DEX friction;
- simulated MultiversX top-up / `claimRewards` fees;
- maximum fees implied by the gas limits actually signed;
- expected claim-cycle loss;
- worst-case claim-cycle loss.

Claimed rewards are shown separately and are never counted as a recovery cost.

## Network fees

Top-up and `claimRewards` use the official read-only MultiversX `transaction/cost` endpoint when available. The returned gas-unit estimate is converted to EGLD using the live network configuration and the official split between full-price movement/data gas and gas-price-modified contract execution.

Recovery keeps two network-fee bounds for every planned transaction:

- `feeEgld`: the simulated fee using `txGasUnits` from the read-only cost endpoint;
- `maxFeeEgld`: the fee corresponding to the transaction's configured `gasLimit` if that full limit is charged.

The expected Recovery loss uses the simulated network fees. The worst-case Recovery loss uses the gas-limit ceilings.

This distinction is grounded in the historical CowCow transactions supplied as protocol evidence. With a 1% execution gas-price modifier, the gas-limit ceiling formula exactly reproduces the Explorer fees for:

- final `claim@0788@0eb2@267e@0fde`, gas limit `270,000,000`: `0.002786625 EGLD`;
- `unstake@0788@0eb2@267e@0fde`, gas limit `315,000,000`: `0.003239595 EGLD`;
- historical `claimRewards`, gas limit `600,000,000`: `0.00606732 EGLD`.

If live transaction-cost simulation is unavailable, the configured gas limit becomes both the expected fallback and the maximum fee bound.

The current claim-cycle estimate intentionally keeps the following separate until their write builders are enabled in Recovery:

- xExchange transaction gas;
- `unstake` network fee;
- final CowCow `claim@<nonce>...` network fee.

## Known xExchange tolerance follow-up

The current worst-case sell-return calculation applies tolerance conservatively as `output * (1 - tolerance)`. The official `@multiversx/sdk-dapp-swap` implementation for a fixed-input swap uses `output / (1 + tolerance)` before rounding to atomic units. At a 1% tolerance the current implementation is slightly more pessimistic; it should be aligned with the official SDK before the DEX worst-case figure is considered exact.
