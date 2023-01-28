package com.example.nft

import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.PrivateKey


data class Treasury(val treasuryKey: PrivateKey, val treasuryAccountId: AccountId)
