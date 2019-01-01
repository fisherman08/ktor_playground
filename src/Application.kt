package com.github.fisherman08

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.sessions.*
import com.fasterxml.jackson.databind.*
import io.ktor.auth.*
import io.ktor.jackson.*
import io.ktor.features.*
import io.ktor.html.respondHtml
import kotlinx.html.*
import java.io.File
import java.util.*

import com.github.fisherman08.session

fun main(args: Array<String>): Unit = io.ktor.server.tomcat.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Sessions) {
        cookie<MySession>(
            "SESSION_FEATURE_SESSION_ID",
            SessionStorageMemory()
            //directorySessionStorage(File(".sessions"), cached = true)
        ) {
            cookie.path = "/" // Specify cookie's path '/' so it can be used in the whole site
        }

    }

    install(Authentication) {

        form(name = "myauth") {
            userParamName = "email"
            passwordParamName = "password"
            challenge = FormAuthChallenge.Redirect {
                "/login"
            }
            skipWhen { call ->
                val session = call.sessions.get<MySession>() ?: MySession()
                val user_id = session.user_id
                (user_id != null)
            }
            validate { credentials ->
                if (credentials.name == "admin" && credentials.password == "admin") {
                    UserPrincipal(id = 11)
                } else {
                    null
                }
            }
        }

        session(name = "session_check"){
            challenge = SessionAuthChallenge.Redirect{
                val params = listOf(
                    "m" to "ログインしてけろ"
                ).formUrlEncode()
                "/login?$params"
            }
            validate{ session ->
                if(session.user_id != null) {
                    UserPrincipal(id = 11)
                } else {
                    null
                }
            }
        }
    }


    routing {
        intercept(ApplicationCallPipeline.Setup) {
            // リクエストがあった時点でセッションを貼る
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session)
        }

        get("/") {
            // 通常はセッションにユーザーIDがセットされていなくてもOK
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }


        authenticate("session_check") {
            route("/admin") {

                route("index") {

                    get {
                        call.respondHtml {
                            body {
                                div {
                                    +"admin!"
                                }
                            }
                        }
                    }
                }


            }

        }


        route("/login") {
            get {
                val params = call.parameters
                val message = params["m"]
                call.respondHtml {
                    body {
                        form(action = "/login", method = FormMethod.post) {
                            if(message != null){
                                p{
                                    +message
                                }
                            }

                            p {
                                +"Email: "
                                textInput(name = "email") { value = "" }
                            }
                            p {
                                +"Password: "
                                passwordInput(name = "password") { value = "" }
                            }
                            p {
                                submitInput { value = "Login" }
                            }
                        }
                    }
                }
            }

            authenticate("myauth") {
                post {
                    val principal = call.principal<UserPrincipal>()
                    if (principal == null) {
                        call.respondRedirect("/login")
                        return@post
                    }

                    val session = call.sessions.get<MySession>() ?: MySession()
                    call.sessions.set(session.copy(user_id = principal!!.id))

                    call.respondRedirect("/admin/index")
                }
            }
        }
    }
}

data class MySession(
    val count: Int = 0,
    val updated_on: Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis,
    val user_id: Int? = null
)

data class UserPrincipal(val id: Int) : Principal