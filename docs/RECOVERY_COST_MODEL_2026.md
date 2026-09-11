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

## EGLD loss model

For the temporary MOOVE amount, Recovery requests live xExchange quotes for:

1. fixed-output `EGLD -> MOOVE` buy cost;
2. fixed-input `MOOVE -> EGLD` expected sell-back;
3. minimum sell-back at the configured tolerance.

The UI separates:

- temporary EGLD capital;
- expected DEX friction;
- worst-case DEX friction;
- MultiversX top-up fee;
- MultiversX claim fee;
- expected claim-cycle loss;
- worst-case claim-cycle loss.

Claimed rewards are shown separately and are never counted as a recovery cost.

## Network fees

Top-up and claim fees use the official read-only MultiversX transaction-cost endpoint when available. Gas units are converted to EGLD with current network parameters and the gas-price modifier.

If live transaction-cost simulation is unavailable, the configured transaction gas limit is used as a conservative fallback and the UI labels the estimate accordingly.

The current claim-cycle estimate intentionally keeps the following separate until their exact transaction construction is part of Recovery:

- xExchange transaction gas;
- unstake network fee;
- unbond network fee.
