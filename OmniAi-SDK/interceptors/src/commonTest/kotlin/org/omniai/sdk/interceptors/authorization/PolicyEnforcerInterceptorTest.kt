package org.omniai.sdk.interceptors.authorization

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.AuthorizationDecision
import org.omniai.sdk.interceptors.auth.AuthorizationInput
import org.omniai.sdk.interceptors.auth.AuthorizationInputProvider
import org.omniai.sdk.interceptors.auth.PolicyEnforcerInterceptor
import org.omniai.sdk.interceptors.auth.Subject
import org.omniai.sdk.interceptors.auth.Action
import org.omniai.sdk.interceptors.auth.Resource
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort
import org.omniai.sdk.interceptors.helper.CapturingChain
import org.omniai.sdk.interceptors.helper.fakeContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FakeAuthorizationInputProvider : AuthorizationInputProvider {
    override fun getAuthorizationInput(context: GatewayContext): AuthorizationInput {
        return AuthorizationInput(
            subject = Subject("user1"),
            action = Action("read"),
            resource = Resource("file", "1")
        )
    }
}

class FakePolicyDecisionPoint(private val decision: AuthorizationDecision) : PolicyDecisionPointPort {
    var lastInput: AuthorizationInput? = null
    override suspend fun decide(context: AuthorizationInput): AuthorizationDecision {
        lastInput = context
        return decision
    }
}

class PolicyEnforcerInterceptorTest {

    @Test
    fun `deny decision returns Error`() = runTest {
        val pdp = FakePolicyDecisionPoint(AuthorizationDecision.Deny("Not authorized"))
        val interceptor = PolicyEnforcerInterceptor(FakeAuthorizationInputProvider(), pdp)
        val context = fakeContext()
        val chain = CapturingChain()

        val result = interceptor.handle(context, chain)

        assertIs<PipelineResult.Error>(result)
        assertIs<InvalidRequest>(result.error)
        assertEquals("Authorization failed: Not authorized", result.error.message)
    }

    @Test
    fun `allow decision proceeds chain`() = runTest {
        val pdp = FakePolicyDecisionPoint(AuthorizationDecision.Allow)
        val interceptor = PolicyEnforcerInterceptor(FakeAuthorizationInputProvider(), pdp)
        val context = fakeContext()
        val chain = CapturingChain()

        val result = interceptor.handle(context, chain)

        assertIs<PipelineResult.Unary>(result)
        val input = pdp.lastInput
        assertNotNull(input)
        assertEquals("user1", input.subject.id)
        assertNotNull(chain.lastContext)
    }
}
