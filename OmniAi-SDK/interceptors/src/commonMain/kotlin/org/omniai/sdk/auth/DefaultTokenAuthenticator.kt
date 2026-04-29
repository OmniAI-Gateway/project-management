package org.omniai.sdk.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.omniai.sdk.auth.domain.AuthToken
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.TokenKind
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.interfaces.AuthSecurityInfrastructure
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine
import org.omniai.sdk.auth.interfaces.TokenAuthenticator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DefaultTokenAuthenticator(
    private val infra: AuthSecurityInfrastructure,
    private val jwtVerificationEngine: JwtVerificationEngine,
) : TokenAuthenticator {

    override suspend fun authenticate(
        token: AuthToken,
        params: TokenValidationParams
    ): AuthenticationDecision {
        return try {
            if (token.kind == TokenKind.OPAQUE) {
                val result = infra.introspectToken(token.rawValue)
                if (result != null && result.active) {
                    val claimsMap = mapOf(
                        "sub" to (result.sub ?: ""),
                        "aud" to result.aud,
                        "iss" to (result.iss ?: "")
                    )
                    AuthenticationDecision.Allow(claimsMap)
                } else {
                    AuthenticationDecision.Deny("Token inativo ou inválido.")
                }
            } else {
                val kid = extractKidFromHeader(token.rawValue)
                    ?: return AuthenticationDecision.Deny("O JWT não contém um 'kid' (Key ID) no cabeçalho.")
                val publicKey = infra.getPublicKey(Kid(kid))
                    ?: return AuthenticationDecision.Deny("Chave pública não encontrada para o kid: $kid")
                jwtVerificationEngine.verify(token.rawValue, publicKey, params)
            }
        } catch (e: Exception) {
            AuthenticationDecision.Deny("Falha na autenticação: ${e.message}")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun extractKidFromHeader(jwt: String): String? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val headerBase64Url = parts[0]
            val padding = (4 - headerBase64Url.length % 4) % 4
            val paddedBase64 = headerBase64Url.padEnd(headerBase64Url.length + padding, '=')
            val headerBytes = Base64.UrlSafe.decode(paddedBase64)
            val headerJson = headerBytes.decodeToString()
            val jsonElement = Json.parseToJsonElement(headerJson)
            jsonElement.jsonObject["kid"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            println("Aviso: Falha ao tentar extrair o 'kid' do JWT. O token pode estar mal formatado. Erro: ${e.message}")
            null
        }
    }
}