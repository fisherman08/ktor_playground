package com.github.fisherman08

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.sessions.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import io.ktor.features.*
import java.io.File

fun main(args: Array<String>): Unit = io.ktor.server.tomcat.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Sessions) {
        cookie<MySession>(
            "LSESSION_FEATURE_SESSION_ID",
             SessionStorageMemory()
            //directorySessionStorage(File(".sessions"), cached = true)
        ) {
            //transform(SessionTransportTransformerEncrypt(encryptionKey = "dcfdcedbd956398g".toByteArray(), signKey = "dcfdcedbd956398g".toByteArray()))
            cookie.path = "/" // Specify cookie's path '/' so it can be used in the whole site
        }

        cookie<CookieContainer>("container")
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))

            val cookies = call.sessions.get<CookieContainer>() ?: CookieContainer()
            cookies.data["hoge"] = "fuga"
            call.sessions.set(cookies)
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

data class MySession(val count: Int = 0)
data class CookieContainer(val data: HashMap<String, String> = hashMapOf())

