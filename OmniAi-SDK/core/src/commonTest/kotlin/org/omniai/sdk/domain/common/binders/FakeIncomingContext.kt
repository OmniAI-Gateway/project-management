package org.omniai.sdk.domain.common.binders

import org.omniai.sdk.binders.IncomingContext

class FakeIncomingContext(
    private val headers: Map<String, String> = emptyMap(),
    private val queryParams: Map<String, String> = emptyMap(),
    private val pathParams: Map<String, String> = emptyMap(),
    private val properties: Map<String, String> = emptyMap()
) : IncomingContext {
    override fun getHeader(key: String): String? = headers[key]
    override fun getQueryParam(key: String): String? = queryParams[key]
    override fun getPathParam(key: String): String? = pathParams[key]
    override fun getProperty(key: String): String? = properties[key]
}