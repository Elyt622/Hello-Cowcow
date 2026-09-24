# CowCow reward decoding: ABI and real read-only evidence

## Evidence provenance

The original staking application uses this contract:
`erd1qqqqqqqqqqqqqpgqqgzzsl0re9e3u0t3mhv3jwg6zu63zssd7yqs3uu9jk`.

The `VarAprStake` ABI was extracted as JSON data (not executed as JavaScript) from:
https://staking.cowcow.io/static/js/main.19e7a970.js

The same bundle explicitly configures the contract address above and uses an ABI-aware
`ResultsParser` on `getAllDataForUser`. Its claim display reads the amounts in
`rewards_accumulated_for_user`, not arbitrary integers from the binary response.
The relevant ABI subset is retained in `docs/abi/cowcow-data-out.abi.json`.
This is frontend ABI evidence, not a claim that the deployed contract source was rebuilt or audited.

Independent read-only query and generic ABI decoding run:
https://github.com/Elyt622/Hello-Cowcow/actions/runs/36066912007

At 2026-09-24 22:21:02 UTC, `POST https://api.multiversx.com/query` with
`funcName=getAllDataForUser`, an explicit caller, `args=[]`, and `value=0` returned
`returnCode=ok`. No transaction was signed or broadcast. Both results were consumed
completely using the recovered ABI, without trailing bytes or guessed offsets.

Observed contract code hash (Base64):
`ia5NN2V/ZGg4Qu6FC2ROQmNd8MAzqI95IyKR+BVs8FA=`.

## DataOut field order

| Field | Wire type |
| --- | --- |
| staked_nfts | List<u64> |
| unstaked_nfts | List<UnstakedNft { nonce: u64, unlock_time: u64 }> |
| total_value_locked | BigUint |
| reward_per_block | BigUint |
| one_share | BigUint |
| min_stake_amount | BigUint |
| min_reward_amount | BigUint |
| unlock_period | u64 |
| rewards_accumulated_for_user | List<EsdtTokenPayment> |
| shares | BigUint |

Each payment is `token_identifier: TokenIdentifier`, `token_nonce: u64`,
then `amount: BigUint`. A nested list starts with a four-byte unsigned count.
A nested token identifier or BigUint starts with a four-byte unsigned byte length.
Each u64 takes exactly eight bytes. These are big-endian encodings.
References:
https://docs.multiversx.com/developers/data/simple-values/
https://docs.multiversx.com/developers/data/composite-values/

The decoder follows this field order and selects only the `MOOVE-875539` payment
with fungible nonce zero. It converts atomic amounts to MOOVE with 18 decimal places,
without a floating-point conversion. Multiple matching payments are summed.
Neither shares nor global reward settings are user rewards.

## Recorded real fixtures

The captured, complete Base64 return values are in `app/src/test/resources/cowcow/`.
They are regression fixtures, not hardcoded balances used by the app.

### mainnet-104-cows.base64

- Complete DataOut size: 934 bytes.
- Staked NFT count: 104.
- First nonce: 696 (`02b8`); last nonce: 5861 (`16e5`).
- Payment identifier: MOOVE-875539; nonce: 0.
- Exact payment amount in atomic units: `107243504303649480000000`.
- Exact human-readable amount at capture: `107243.50430364948 MOOVE`.
- Payment amount length prefix: bytes 914..917; length 10.
- Payment amount bytes: 918..927 (zero-based offsets).
- Shares: 31200, separately encoded after the reward list.

Rewards are time-sensitive: this captured value is not a promise of the amount
that will be returned by a future query or transaction.

### mainnet-empty.base64

- Complete DataOut size: 62 bytes.
- Staked and unstaked lists: empty.
- Reward payment list: empty, so claimable MOOVE is exactly zero.
- Global total_value_locked: 128400; unlock_period: 604800.
- Shares: zero.

## Root cause of the previous patches

Searching the Base64 tail with a regex did not identify an ABI field. The later
scanner was also incorrect: it treated four bytes at every position as a possible
BigUint length and chose the largest candidate. On the real 104-NFT response,
bytes inside u64 NFT nonces at offsets 79, 103, 687 and 727 were mistaken for four
different 38-byte BigUints, producing the ambiguity error. None is a reward field.
The correct MOOVE amount is a ten-byte BigUint inside its named payment entry.
On the real empty response, that scanner even interpreted part of unlock_period
as a nine-byte amount and returned 1079.134528312008769536 fictitious MOOVE.

A synthetic fixture designed around the scanner's assumptions did not validate
the deployed protocol. The regression suite now includes the two real responses
as well as ABI-conformant synthetic adversarial cases.

## Safety and scope

Malformed Base64, impossible lengths, truncated fields, zero/duplicate staked NFT
nonces, invalid token identifiers and unexpected trailing data fail closed.
A malformed response is never converted into zero rewards or funded liquidity.
A valid empty reward list is a legitimate zero. Large unrelated reward payments
and large global metadata do not influence the selected MOOVE amount.

No claim/unstake payload, gas setting, live simulation, xPortal confirmation,
transaction broadcast logic, local.properties or project credential is changed.
The existing top-up margin controls are retained. The same ABI parser now reads
staked nonces and user reward payments, eliminating the previous zero-chunk heuristic.
