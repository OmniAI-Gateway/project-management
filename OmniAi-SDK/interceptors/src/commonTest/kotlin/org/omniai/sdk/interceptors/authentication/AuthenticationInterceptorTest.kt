package org.omniai.sdk.interceptors.authentication

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.AUTH_RESULT_KEY
import org.omniai.sdk.interceptors.auth.AUTH_TOKEN_KIND_KEY
import org.omniai.sdk.interceptors.auth.AuthenticationInterceptor
import org.omniai.sdk.interceptors.auth.domain.AuthToken
import org.omniai.sdk.interceptors.auth.domain.AuthValidationResult
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.TokenAuthenticator
import org.omniai.sdk.interceptors.helper.CapturingChain
import org.omniai.sdk.interceptors.helper.fakeContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FakeTokenAuthenticator(
    private val decision: AuthenticationDecision,
) : TokenAuthenticator {
    override suspend fun authenticate(
        token: AuthToken,
        params: TokenValidationParams?,
    ): AuthenticationDecision = decision
}

class AuthenticationInterceptorTest {
    @Test
    fun `missing token returns Error`() =
        runTest {
            val interceptor = AuthenticationInterceptor(FakeTokenAuthenticator(AuthenticationDecision.Deny("test")))
            val context = fakeContext() // no token
            val chain = CapturingChain()

            val result = interceptor.handle(context, chain)

            assertIs<PipelineResult.Error>(result)
            assertIs<InvalidRequest>(result.error)
            assertEquals("Authentication failed: Token missing", result.error.message)
        }

    @Test
    fun `deny decision returns Error`() =
        runTest {
            val interceptor =
                AuthenticationInterceptor(FakeTokenAuthenticator(AuthenticationDecision.Deny("Invalid token")))
            val context = fakeContext()
            context.request.providerOptions.put(AUTH_BEARER_TOKEN_KEY, "some-token")
            val chain = CapturingChain()

            val result = interceptor.handle(context, chain)

            assertIs<PipelineResult.Error>(result)
            assertIs<InvalidRequest>(result.error)
            assertEquals("Authentication failed: Invalid token", result.error.message)
        }

    @Test
    fun `allow decision proceeds chain and sets context attributes`() =
        runTest {
            val validationResult = AuthValidationResult.Opaque(IntrospectionResult(active = true))
            val interceptor =
                AuthenticationInterceptor(FakeTokenAuthenticator(AuthenticationDecision.Allow(validationResult)))
            val context = fakeContext()
            context.request.providerOptions.put(AUTH_BEARER_TOKEN_KEY, "valid-token")
            val chain = CapturingChain()

            val result = interceptor.handle(context, chain)

            assertIs<PipelineResult.Unary>(result)
            val ctx = chain.lastContext
            assertNotNull(ctx)
            assertEquals(validationResult, ctx.attributes[AUTH_RESULT_KEY])
            assertEquals("OPAQUE", ctx.attributes[AUTH_TOKEN_KIND_KEY])
            assertNotNull(ctx.attributes[AuthenticationInterceptor.AUTH_TOKEN_KEY])
        }
}
