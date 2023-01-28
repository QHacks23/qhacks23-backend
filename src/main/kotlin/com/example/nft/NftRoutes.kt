package com.example.nft

import com.example.nft.query.QDCredit
import com.example.user.UserRepository
import com.hedera.hashgraph.sdk.Mnemonic
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HttpGetBalance(val firstName: String, val lastName: String, val cids: List<HttpGetCID>, val hbar : String)
@Serializable
data class HttpGetCID(val tokenId: String, val count: Long, val body: String, val available: Boolean)
@Serializable
data class HttpPostSellAvailability(val userId: String, val bool: Boolean, val tokenId: String)
@Serializable
data class HttpPostCredit(val userId: String, val credit: String, val mnemonic: String, val cost: Long)
@Serializable
data class HttpPostSell(val userId : String, val tokenId: String,  val mnemonic: String)
@Serializable
data class HttpPostBuyOrder(val userId: String, val tokenId: String,  val mnemonic: String)
@Serializable
data class HttpGetSales(val tokenIds: List<String>)
@Serializable
data class HttpGetBuyOrders(val tokenIds: List<String>)
@Serializable
data class HttpGetBuyAndSell(val buyOrders: HttpGetBuyOrders, val sellOpportunities: HttpGetSales)
@Serializable
data class HttpNftQuerys(val nft: List<HttpNftQuery>)
@Serializable
data class HttpNftQuery(val token: String, val body: String, val accountId: String)
fun Route.nftRoutes(nftManager: NftManager, userRepository: UserRepository) {
    route("/credit") {
        post("/create"){
            val credit = call.receive<HttpPostCredit>()
            val tokenId = nftManager.createNft()
            val user = userRepository.getByFirebaseId(credit.userId) ?: return@post call.respondText("User could not be resolved from id", status = HttpStatusCode.BadRequest)
            nftManager.mintNft(credit.credit, tokenId, user, credit.mnemonic, credit.cost)
            call.respondText("Nft was successfully created for ${user.firstName}", status = HttpStatusCode.OK)
        }

        post("/sell"){
            val sell = call.receive<HttpPostSellAvailability>()
            val user = userRepository.getByFirebaseId(sell.userId) ?: return@post call.respondText("User could not be resolved from id", status = HttpStatusCode.BadRequest)
            val credit = QDCredit().tokenId.eq(sell.tokenId).findOne() ?:  return@post call.respondText("Could not find token", status = HttpStatusCode.BadRequest)
            nftManager.setCreditAvailability(sell.bool, user, credit)
            call.respondText("Nft was successfully changed to for ${sell.bool}", status = HttpStatusCode.OK)
        }

        get("/balance"){
            val id = call.request.queryParameters["id"] ?: return@get call.respondText("User id was null", status = HttpStatusCode.BadRequest)
            val user = userRepository.getByFirebaseId(id) ?: return@get call.respondText("User could not be resolved from id", status = HttpStatusCode.BadRequest)
            val balance = nftManager.checkBalance(user)
            call.respond(HttpGetBalance(user.firstName, user.lastName, balance.cids.map { HttpGetCID(it.token, it.count, it.body, it.availability)}, balance.hbar))
        }

        post("/buyorder"){
            val buyOrder = call.receive<HttpPostBuyOrder>()
            val user = userRepository.getByFirebaseId(buyOrder.userId) ?: return@post call.respondText("User could not be resolved from id: ${buyOrder.userId}", status = HttpStatusCode.BadRequest)
            val order = nftManager.createBuyOrder(user, buyOrder.tokenId, Mnemonic.fromString(buyOrder.mnemonic))
            when(order){
                is NftManager.Result.Success -> call.respondText("Buy order was created for ${order.tokenId}", status = HttpStatusCode.OK)
                else -> call.respondText("Error", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/orders"){
            val id = call.request.queryParameters["id"] ?: return@get call.respondText("User id was null", status = HttpStatusCode.BadRequest)
            val user = userRepository.getByFirebaseId(id) ?: return@get call.respondText("User could not be resolved from id: ${id}", status = HttpStatusCode.BadRequest)
            val buyOrders = nftManager.getMyBuyOrders(user)
            val availableSales = nftManager.getMyAvailableOrders(user)
            call.respond(HttpGetBuyAndSell(HttpGetBuyOrders(buyOrders), HttpGetSales(availableSales)))
        }

        post("/sell"){
            val sell = call.receive<HttpPostSell>()
            val seller = userRepository.getByFirebaseId(sell.userId) ?: return@post call.respondText("User could not be resolved from id", status = HttpStatusCode.BadRequest)
            nftManager.sellCredit(seller, sell.tokenId, Mnemonic.fromString(sell.mnemonic))
            call.respondText("Nft was sold from ${seller.firstName} to X", status = HttpStatusCode.OK)
        }

        get("/available"){
           val available = nftManager.getAllAvailable()
            call.respond(HttpNftQuerys(available.map { HttpNftQuery(it.token, it.body, it.accountId) }))
        }
    }
}