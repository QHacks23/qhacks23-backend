package com.example.nft

import com.example.nft.query.QDCredit
import com.example.user.DUser
import com.google.gson.Gson
import com.hedera.hashgraph.sdk.AccountBalanceQuery
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.hedera.hashgraph.sdk.Hbar
import com.hedera.hashgraph.sdk.Mnemonic
import com.hedera.hashgraph.sdk.NftId
import com.hedera.hashgraph.sdk.PrivateKey
import com.hedera.hashgraph.sdk.ReceiptStatusException
import com.hedera.hashgraph.sdk.TokenAssociateTransaction
import com.hedera.hashgraph.sdk.TokenCreateTransaction
import com.hedera.hashgraph.sdk.TokenId
import com.hedera.hashgraph.sdk.TokenMintTransaction
import com.hedera.hashgraph.sdk.TokenNftInfoQuery
import com.hedera.hashgraph.sdk.TokenSupplyType
import com.hedera.hashgraph.sdk.TokenType
import com.hedera.hashgraph.sdk.TransferTransaction
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger { }
data class UserBalance(val cids: List<Nft>, val hbar: String)
data class Nft(val token: String, val count: Long, val body: String, val availability: Boolean)

data class NftQuery(val token: String, val body: String, val accountId: String, val time: Long)
class NftManagerImpl(val treasury: Treasury, val supply: Supply, val client: Client): NftManager {

    private val buyOrders = mutableMapOf<String, Pair<DUser, Mnemonic>>()
    override fun createBuyOrder(buyer: DUser, tokenId: String, buyerKey: Mnemonic): NftManager.Result {
        val nft = QDCredit().tokenId.eq(tokenId).findOne() ?: return NftManager.Result.Error.TokenNFS("This token does not exits")
        if(nft.owner == buyer) return NftManager.Result.Error.TokenNFS("This token is already owned by u!")
        if (nft.tokenId != tokenId || !nft.available) return NftManager.Result.Error.TokenNFS("This token is not for sale at the moment!")
        val assoc = TokenAssociateTransaction().setAccountId(AccountId.fromString(buyer.accountId))
            .setTokenIds(Collections.singletonList(TokenId.fromString(tokenId))).freezeWith(client)
            .sign(PrivateKey.fromMnemonic(buyerKey))
        val execAssoc = assoc.execute(client)
        try {
            execAssoc.getReceipt(client)
        }catch(e: ReceiptStatusException){
            logger.info("Already associated")
        }
        if (buyOrders.containsKey(tokenId))
            return NftManager.Result.Error.BuyOrderAlreadyExists("a buy order for this token already exists")
        buyOrders[tokenId] = Pair(buyer, buyerKey)
        return NftManager.Result.Success(tokenId)
    }

    override fun createNft(): TokenId {
        val nftCreate = TokenCreateTransaction()
            .setTokenName("CarbonCredit")
            .setTokenSymbol("CREDIT")
            .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
            .setDecimals(0)
            .setTreasuryAccountId(treasury.treasuryAccountId)
            .setSupplyType(TokenSupplyType.FINITE).setMaxSupply(1)
            .setSupplyKey(supply.supplyKey).freezeWith(client)

        val nftCreateTxSign = nftCreate.sign(treasury.treasuryKey) ?: throw Exception("Could not get sign for nft sign")
        val nftCreateSubmit = nftCreateTxSign.execute(client) ?: throw Exception("Could not submit for nft create")
        val nftCreateRx = nftCreateSubmit.getReceipt(client) ?: throw Exception("Could not get receipt for nft create")
        return nftCreateRx.tokenId ?: throw Exception("Could not get token id for nft create")
    }

    override fun mintNft(credit: String, tokenId: TokenId, user: DUser, userKey: String, cost: Long){
        val mintTx = TokenMintTransaction().setTokenId(tokenId).addMetadata(credit.toByteArray()).freezeWith(client)
        val mintTxSign = mintTx.sign(supply.supplyKey) ?: throw Exception("Could not get sign for nft mint")
        val mintTxSubmit = mintTxSign.execute(client) ?: throw Exception("Could not submit for nft mint")
        val mintRx = mintTxSubmit.getReceipt(client) ?: throw Exception("Could not get receipt for nft mint")
        logger.info("Created NFT $tokenId with serial: ${mintRx.serials}")

        val associateTx = TokenAssociateTransaction()
            .setAccountId(AccountId.fromString(user.accountId))
            .setTokenIds(Collections.singletonList(tokenId))
            .freezeWith(client)
            .sign(PrivateKey.fromMnemonic(Mnemonic.fromString(userKey)))

        val associateTxSubmit = associateTx.execute(client) ?: throw Exception("Could not submit for nft mint")
        val associateRx = associateTxSubmit.getReceipt(client) ?: throw Exception("Could not get receipt for nft mint")
        logger.info("NFT association with senders account: ${associateRx.status}")
        val tokenTransferTx = TransferTransaction()
            .addNftTransfer(NftId(tokenId, 1), treasury.treasuryAccountId, AccountId.fromString(user.accountId))
            .freezeWith(client).sign(treasury.treasuryKey)
        val execTransfer = tokenTransferTx.execute(client)
        execTransfer.getReceipt(client)
        val updatedCredit = DCredit(tokenId.toString(), user, true, 1)
        updatedCredit.save()
        user.credits.add(updatedCredit)
        logger.info("Created and transferred NFT to ${user.firstName}")
    }

    override fun checkTreasuryBalance(){
        val balance = AccountBalanceQuery().setAccountId(treasury.treasuryAccountId).execute(client)
        balance.tokens.forEach { tokenId ->
            val info = TokenNftInfoQuery().setNftId(NftId(tokenId.key, 1)).execute(client)
            info.forEach { it.metadata }
        }
    }

    override fun getMyBuyOrders(buyer: DUser): List<NftQuery>{
        return buyOrders.mapNotNull { order ->
            if(order.value.first == buyer) {
                val nftQuery = TokenNftInfoQuery().setNftId(NftId(TokenId.fromString(order.key), 1)).execute(client)
                nftQuery.map { NftQuery(order.key, it.metadata.decodeToString(), it.accountId.toString(), it.creationTime.epochSecond) }.first()
            }else {
                null
            }
        }
    }

    override fun getMyNft(user: DUser): List<NftQuery> {
        val myNft = QDCredit().owner.eq(user).findList()
        if (myNft.isEmpty()) return emptyList()
        return myNft.flatMap { credit ->
            val nftQuery = TokenNftInfoQuery().setNftId(NftId(TokenId.fromString(credit.tokenId), 1)).execute(client)
            nftQuery.map { NftQuery(credit.tokenId, it.metadata.decodeToString(), it.accountId.toString(), it.creationTime.epochSecond) }
        }
    }

    override fun getAllAvailable(): List<NftQuery> {
        val availableNfts = QDCredit().available.eq(true).findList()
        if (availableNfts.isEmpty()) return emptyList()
        return availableNfts.flatMap { credit ->
            val nftQuery = TokenNftInfoQuery().setNftId(NftId(TokenId.fromString(credit.tokenId), 1)).execute(client)
            nftQuery.map { NftQuery(credit.tokenId, it.metadata.decodeToString(), it.accountId.toString(), it.creationTime.epochSecond) }
        }

    }

    override fun getMyAvailableOrders(seller: DUser): List<NftQuery>{
        return seller.credits.mapNotNull {credit ->
            if(buyOrders.containsKey(credit.tokenId)){
                val nftQuery = TokenNftInfoQuery().setNftId(NftId(TokenId.fromString(credit.tokenId), 1)).execute(client)
                nftQuery.map { NftQuery(credit.tokenId, it.metadata.decodeToString(), it.accountId.toString(), it.creationTime.epochSecond) }.first()
            } else{
                null
            }
        }
    }

    override fun sellCredit(seller: DUser, tokenId: String, sellerKey: Mnemonic): NftManager.Result{
        val buyOrderMemonic = buyOrders[tokenId] ?: return NftManager.Result.Error.BuyOrderDNE("Buy order was not found for this token")
        val buyer = buyOrderMemonic.first
        val buyerMnemonic = buyOrderMemonic.second
        val nft = QDCredit().tokenId.eq(tokenId).findOne() ?: return NftManager.Result.Error.TokenDNE("Token could not be found")

        val tokenTransferTx = TransferTransaction()
            .addNftTransfer(NftId(TokenId.fromString(tokenId), 1), AccountId.fromString(seller.accountId), AccountId.fromString(buyer.accountId))
            .addHbarTransfer(AccountId.fromString(buyer.accountId), Hbar.fromTinybars(-nft.value))
            .addHbarTransfer(AccountId.fromString(seller.accountId), Hbar.fromTinybars(nft.value))
            .freezeWith(client).sign(PrivateKey.fromMnemonic(sellerKey)).sign(PrivateKey.fromMnemonic(buyerMnemonic))

        try {
            val tokenTransferSubmit = tokenTransferTx.execute(client)
            tokenTransferSubmit.getReceipt(client)
        }catch (e: Exception){
            throw Exception("Something went wrong with the transaction!")
        }

        logger.info("Token transfer from ${seller.firstName} to ${buyer.firstName}")
        nft.owner = buyer
        nft.available = false
        nft.save()
        seller.credits.remove(nft)
        seller.save()
        buyOrders.remove(tokenId)
        return NftManager.Result.Success(tokenId)
    }

    override fun setCreditAvailability(availability: Boolean, use: DUser, credit: DCredit) {
        credit.available = availability
        credit.save()
    }

    override fun checkBalance(user: DUser) : UserBalance{
        val balance = AccountBalanceQuery().setAccountId(AccountId.fromString(user.accountId)).execute(client)
        val cids = balance.tokens.flatMap { tokenId ->
            val info = TokenNftInfoQuery().setNftId(NftId(tokenId.key, 1)).execute(client)
            info.map {
                val tokenIdString = tokenId.toString().substringBefore('=')
                val count = tokenId.toString().substringAfter('=').toLong()
                val availability = QDCredit().tokenId.eq(tokenIdString).findOne()?.available ?: false
                Nft(tokenIdString, count, it.metadata.decodeToString(), availability)
            }
        }
        val hbar = AccountBalanceQuery().setAccountId(AccountId.fromString(user.accountId)).execute(client).hbars
        return UserBalance(cids, hbar.toString())
    }
}