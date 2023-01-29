package com.example.nft

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

@Serializable
data class HttpReturnFileName(val IpfsHash: String)
fun Route.fsRoutes(){
    static("/"){
        staticRootFolder = File("src/main/resources/fs")
        files(".")
    }

    route("/upload"){
        post{
            val multipartData = call.receiveMultipart()
            multipartData.forEachPart {part ->
                when (part) {
                    is PartData.FormItem -> {
                        val fileName = UUID.randomUUID().toString().replace("-", "");
                        val fileContents = part.value
                        File("src/main/resources/fs/$fileName.json").writeText(fileContents)
                        call.respond(HttpReturnFileName("${fileName}.json"))
                    }
                    is PartData.FileItem -> {
                        val fileSuffix = part.originalFileName?.substringAfter(".") ?: "jpeg"
                        val fileName = UUID.randomUUID().toString().replace("-", "");
                        val fileBytes = part.streamProvider().readBytes()
                        File("src/main/resources/fs/${fileName}.${fileSuffix}").writeBytes(fileBytes)
                        call.respond(HttpReturnFileName("${fileName}.${fileSuffix}"))
                    }
                    else -> {}
                }
                part.dispose()
            }

        }
    }
}