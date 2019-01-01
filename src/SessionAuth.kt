package com.github.fisherman08

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.sessions.get
import io.ktor.sessions.sessions


class SessionAuthenticationProvider(name: String?) : AuthenticationProvider(name) {
    internal var authenticationFunction: suspend ApplicationCall.(MySession) -> Principal? = { null }

    /**
     * A response to send back if authentication failed
     */
    var challenge: SessionAuthChallenge = SessionAuthChallenge.Unauthorized

    /**
     * Sets a validation function that will check given [UserPasswordCredential] instance and return [Principal],
     * or null if credential does not correspond to an authenticated principal
     */
    fun validate(body: suspend ApplicationCall.(MySession) -> Principal?) {
        authenticationFunction = body
    }
}

/**
 * Installs Form Authentication mechanism
 */
fun Authentication.Configuration.session(name: String? = null, configure: SessionAuthenticationProvider.() -> Unit) {
    val provider = SessionAuthenticationProvider(name).apply(configure)
    val validate = provider.authenticationFunction
    val challenge = provider.challenge

    provider.pipeline.intercept(AuthenticationPipeline.CheckAuthentication) { context ->

        val session = call.sessions.get<MySession>() ?: MySession()
        val principal = session?.let { validate(call, it) }

        if (principal != null) {
            context.principal(principal)
        } else {
            val cause = if (session == null) AuthenticationFailedCause.NoCredentials else AuthenticationFailedCause.InvalidCredentials
            context.challenge("session_auth", cause) {
                when (challenge) {
                    SessionAuthChallenge.Unauthorized -> call.respond(HttpStatusCode.Unauthorized)
                    is SessionAuthChallenge.Redirect -> call.respondRedirect(challenge.url(call, session))

                }
                it.complete()
            }
        }
    }
    register(provider)
}


sealed class SessionAuthChallenge {
    /**
     * Redirect to an URL provided by the given function.
     * @property url is a function receiving [ApplicationCall] and [UserPasswordCredential] and returning an URL to redirect to.
     */
    class Redirect(val url: ApplicationCall.(MySession?) -> String) : SessionAuthChallenge()

    /**
     * Respond with [HttpStatusCode.Unauthorized].
     */
    object Unauthorized : SessionAuthChallenge()
}

