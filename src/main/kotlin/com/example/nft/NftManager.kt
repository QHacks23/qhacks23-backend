package com.example.nft

import com.example.user.DUser
import com.hedera.hashgraph.sdk.Mnemonic
import com.hedera.hashgraph.sdk.PrivateKey
import com.hedera.hashgraph.sdk.TokenId

interface NftManager {

    sealed class Result {
        data class Success(val tokenId: String) : Result()
        sealed class Error(val message: String) : Result() {
            class BuyOrderAlreadyExists(message: String) : Error(message)
            class TokenNFS(message: String) : Error(message)
            class TokenDNE(message: String) : Error(message)
            class BuyOrderDNE(message: String) : Error(message)
        }
    }
    fun getMyNft(user: DUser): List<NftQuery>
    fun getAllAvailable(): List<NftQuery>
    fun getMyAvailableOrders(seller: DUser): List<NftQuery>
    fun getMyBuyOrders(buyer: DUser): List<NftQuery>
    fun createBuyOrder(buyer: DUser, tokenId: String, buyerKey: Mnemonic): Result
    fun createNft(): TokenId
    fun mintNft(credit: String, tokenId: TokenId, user: DUser, userKey: String, cost: Long)
    fun checkBalance(user: DUser): UserBalance
    fun checkTreasuryBalance()
    fun setCreditAvailability(availability: Boolean, use: DUser, credit: DCredit)
    fun sellCredit(seller: DUser, tokenId: String, sellerKey: Mnemonic): NftManager.Result
}