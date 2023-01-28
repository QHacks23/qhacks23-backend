package com.example.user

import com.example.login.DLogin
import com.example.login.LoginRepository
import com.example.nft.DCredit
import java.math.BigInteger
import java.security.MessageDigest
import com.hedera.hashgraph.sdk.PrivateKey
import com.hedera.hashgraph.sdk.AccountCreateTransaction
import com.hedera.hashgraph.sdk.Client
import com.hedera.hashgraph.sdk.Hbar
import com.hedera.hashgraph.sdk.Mnemonic

class UserManagerImpl(val dLoginRepository: LoginRepository, val dUserRepository: UserRepository, val client: Client): UserManager {
    override fun createUser(
        id: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): UserManager.Result {

        val existing = dLoginRepository.getByEmail(email)
        if (existing != null) {
            return UserManager.Result.Error.Invalid("Email already exists")
        }

        val login = DLogin(email, hashPassword(password))

        val mnemonic = Mnemonic.generate12()

        val privateKey = PrivateKey.fromMnemonic(mnemonic)
        val publicKey = privateKey.publicKey
        val newAccount = AccountCreateTransaction()
            .setKey(publicKey)
            .setInitialBalance(Hbar.fromTinybars(1000))
            .execute(client)


        val user = DUser(
            id,
            firstName,
            lastName,
            login,
            newAccount.getReceipt(client).accountId.toString(),
            publicKey.toString(),
            mutableListOf()
        )
        login.save()
        user.save()
        login.user = user
        login.save()
        return UserManager.Result.Success(user, mnemonic.toString())
    }

    override fun loginUser(email: String, password: String): UserManager.Result {
        val login = dLoginRepository.getByEmail(email)
            ?: return UserManager.Result.Error.NotFound("Login with email $email not found")

        if (login.password != hashPassword(password)) {
            return UserManager.Result.Error.Invalid("Invalid password")
        }

        val employee = dUserRepository.getByLogin(login)
            ?: return UserManager.Result.Error.NotFound("User with login $email not found")

        return UserManager.Result.Success(employee, null)
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(password.toByteArray())).toString(16).padStart(32, '0')
    }
}
