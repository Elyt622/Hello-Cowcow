# CowCow mainnet Recovery evidence

This file records historical successful mainnet transactions against the deployed CowCow staking contract. It is evidence for Recovery behavior; it is not a substitute for explicit user confirmation before any write transaction.

Contract:

`erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk`

## 1. Independent MOOVE reward claim

Transaction:

`0f2a6acd7f91cc87b2dc1872aa6ee26af05d081a33d8da84714310a4de735cff`

Observed mainnet execution on 2025-05-09 18:56:24 UTC:

- status: success
- method / input data: `claimRewards`
- value: `0 EGLD`
- smart-contract result: `ESDTTransfer`
- token: `MOOVE-875539`
- amount hex: `02ccfbfc0d4059771a00`
- decoded amount: `13226.02615914372 MOOVE`
- no COW NFT transfer in this transaction
- historical gas limit: `600000000`
- processing log reports gas used: `6854494`
- transaction fee shown by Explorer: `0.00606732 EGLD`

This verifies that MOOVE rewards can be claimed independently from the CowCow NFT exit flow.

## 2. CowCow unstake

Transaction:

`e9455f080ad8ca3604b503fea5204845c174031edf079780401d8a906b7b415a`

Observed mainnet execution on 2026-07-13 13:38:36 UTC:

- status: success
- method: `unstake`
- input data: `unstake@0788@0eb2@267e@0fde`
- value: `0 EGLD`
- smart-contract result: `ESDTTransfer`
- token: `MOOVE-875539`
- amount hex: `bba37beae3914c7800`
- decoded amount: `3461.32140929712 MOOVE`
- historical gas limit: `315000000`
- processing log reports gas used: `17854780`
- transaction fee shown by Explorer: `0.003239595 EGLD`

This verifies that `unstake` accepts the CowCow nonce list directly and also pays the rewards accrued at unstake time.

A second successful unstake transaction with 72 CowCow nonce arguments is recorded as:

`4d1b422519ece36f560994e1a71bedb520504c5e7427c1aef38a3565392d5be4`

That transaction returned `956.6720341902 MOOVE` and demonstrates that batched unstake is supported well beyond four NFTs.

## 3. Final CowCow claim after unbonding

Transaction:

`929157471e148b5d092a1e90d6b8638974b0ef5da6d012f97bf7e303cc1d9ee8`

Observed mainnet execution on 2026-07-20 14:25:24 UTC:

- status: success
- method: `claim`
- input data: `claim@0788@0eb2@267e@0fde`
- value: `0 EGLD`
- smart-contract result: `MultiESDTNFTTransfer`
- exactly the four matching `COW-cd463d` NFTs are transferred from the staking contract to the wallet
- historical gas limit: `270000000`
- processing log reports gas used: `11459521`
- transaction fee shown by Explorer: `0.002786625 EGLD`

The same four nonces were unstaked in the transaction above and then claimed successfully 7 days, 46 minutes and 48 seconds later.

The earliest claimable time used by Recovery is therefore modeled as:

`unstake timestamp + 7 days`

The historical successful claim happened 46 minutes and 48 seconds after that minimum boundary; this evidence does not imply that the extra 46 minutes were required.

## Verified Recovery sequence

The historical evidence supports this sequence:

1. `claimRewards` to recover the existing MOOVE reward balance independently;
2. optionally recover or swap temporary liquidity after that claim;
3. `unstake@<cow nonce>...` to move CowCows into the unbonding phase and receive any newly accrued MOOVE;
4. wait at least seven days;
5. `claim@<cow nonce>...` to transfer the CowCow NFTs back to the wallet.

Recovery reconstructs pending unbond batches from successful on-chain `unstake` and `claim` history so the waiting state does not depend only on local application storage.
