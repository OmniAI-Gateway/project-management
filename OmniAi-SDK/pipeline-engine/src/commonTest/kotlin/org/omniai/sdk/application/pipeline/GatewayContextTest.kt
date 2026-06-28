package org.omniai.sdk.application.pipeline

import org.omniai.sdk.common.TypedMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GatewayContextTest {
    @Test
    fun `default mode is UNARY`() {
        val ctx = GatewayContext(request = fakeRequest())
        assertEquals(RequestMode.UNARY, ctx.mode)
    }

    @Test
    fun `default res is NoResult`() {
        val ctx = GatewayContext(request = fakeRequest())
        assertEquals(PipelineResult.NoResult, ctx.res)
    }

    @Test
    fun `default attributes map is empty`() {
        val ctx = GatewayContext(request = fakeRequest())
        assertEquals(0, ctx.attributes.size())
    }

    @Test
    fun `copy preserves request and changes mode`() {
        val ctx = fakeContext(mode = RequestMode.UNARY)
        val stream = ctx.copy(mode = RequestMode.STREAM)

        assertSame(ctx.request, stream.request)
        assertEquals(RequestMode.STREAM, stream.mode)
    }

    @Test
    fun `copy with new res does not affect original`() {
        val ctx = fakeContext(res = PipelineResult.NoResult)
        val error = PipelineResult.Error(fakeError())
        val modified = ctx.copy(res = error)

        assertEquals(PipelineResult.NoResult, ctx.res)
        assertEquals(error, modified.res)
    }

    @Test
    fun `attributes TypedMap can be updated independently per copy`() {
        val ctx = fakeContext()
        val attrs1 = TypedMap().also { it.put("key", "value-1") }
        val attrs2 = TypedMap().also { it.put("key", "value-2") }

        val c1 = ctx.copy(attributes = attrs1)
        val c2 = ctx.copy(attributes = attrs2)

        assertEquals("value-1", c1.attributes.get<String>("key"))
        assertEquals("value-2", c2.attributes.get<String>("key"))
    }

    @Test
    fun `two contexts sharing the same attributes instance are structurally equal`() {
        val req = fakeRequest()
        // TypedMap has no structural equals(), so we must share the SAME instance
        // for GatewayContext data-class equality to hold.
        val sharedAttrs = TypedMap()
        val ctx1 =
            GatewayContext(
                request = req,
                mode = RequestMode.UNARY,
                res = PipelineResult.NoResult,
                attributes = sharedAttrs,
            )
        val ctx2 =
            GatewayContext(
                request = req,
                mode = RequestMode.UNARY,
                res = PipelineResult.NoResult,
                attributes = sharedAttrs,
            )
        assertEquals(ctx1, ctx2)
    }

    @Test
    fun `a context is always equal to itself`() {
        val ctx = fakeContext()
        assertEquals(ctx, ctx)
    }
}
