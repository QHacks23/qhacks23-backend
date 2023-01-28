package com.example

import com.example.config.Config
import com.example.config.ConfigLoaderImpl
import com.example.login.LoginRepositoryImpl
import com.example.nft.NftManagerImpl
import com.example.nft.Supply
import com.example.nft.Treasury
import io.ktor.server.plugins.cors.*
import com.example.nft.nftRoutes
import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.example.user.UserManager
import com.example.user.UserManagerImpl
import com.example.user.UserRepositoryImpl
import com.example.user.userRoutes
import com.hedera.hashgraph.sdk.AccountCreateTransaction
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.hedera.hashgraph.sdk.Hbar
import com.hedera.hashgraph.sdk.PrivateKey
import com.hedera.hashgraph.sdk.PublicKey
import io.ebean.DB
import io.ebean.annotation.Platform
import io.ebean.dbmigration.DbMigration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import mu.KotlinLogging
import org.slf4j.event.Level


private val logger = KotlinLogging.logger { }

lateinit var cfgPath: String

fun main(commandLineArgs: Array<String>) {
    logger.info { "Starting...." }
    cfgPath = commandLineArgs[0].ifBlank { throw Exception("No config file found") }
    setDatabase()
    DbMigration.create().apply {
        setPlatform(Platform.POSTGRES)
    }.generateMigration()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.AccessControlAllowMethods)
        allowHeader(HttpHeaders.AccessControlAllowCredentials)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    install(CallLogging) {
        level = Level.INFO
    }
    val config = ConfigLoaderImpl().loadConfig(cfgPath)

    val myAccountId = AccountId.fromString(config.accountId)
    val myPrivateKey = PrivateKey.fromString(config.privateKey)
    val client = Client.forTestnet()
    client.setOperator(myAccountId, myPrivateKey)
    configureSerialization()
    configureRouting(config, client)
}

fun Application.configureRouting(config : Config, client: Client) {
    val treasuryKey = PrivateKey.generateED25519()
    val treasuryPublicKey = treasuryKey.publicKey

    val treasuryAccount = AccountCreateTransaction().setKey(treasuryPublicKey).setInitialBalance(Hbar(100)).execute(client)
    val treasuryAccountId = treasuryAccount.getReceipt(client).accountId ?: throw Exception("Could not get treasury account id")
    val treasury = Treasury(treasuryKey, treasuryAccountId)

    val supplyKey = PrivateKey.generateED25519()
    val supply = Supply(supplyKey)

    val userRepository = UserRepositoryImpl()
    val loginRepository = LoginRepositoryImpl()
    val userManager = UserManagerImpl(loginRepository, userRepository, client)
    val nftManager = NftManagerImpl(treasury, supply, client)
    routing {
        get("/health") {
            call.respondText("OK", status = HttpStatusCode.OK)
        }
        userRoutes(userManager, userRepository)
        nftRoutes(nftManager, userRepository)
    }
}


private fun setDatabase() {
    Class.forName("org.postgresql.Driver")
    DB.getDefault()
    logger.info { "Connected to database" }
}
