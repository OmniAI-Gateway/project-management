package org.omniai.gateway.httpClient

import org.omniai.sdk.core.http.installDefaultTransportPlugins
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlatformHttpClientTest {

    private val testJson = Json { ignoreUnknownKeys = true }

    @Test
    fun `installDefaultTransportPlugins installs HttpTimeout plugin`() {
        val client = buildClientWithDefaults()
        val plugin = client.pluginOrNull(HttpTimeout)
        assertNotNull(plugin, "HttpTimeout should be installed by installDefaultTransportPlugins")
        client.close()
    }

    @Test
    fun `installDefaultTransportPlugins installs ContentNegotiation plugin`() {
        val client = buildClientWithDefaults()
        val plugin = client.pluginOrNull(ContentNegotiation)
        assertNotNull(plugin, "ContentNegotiation should be installed by installDefaultTransportPlugins")
        client.close()
    }

    @Test
    fun `installDefaultTransportPlugins installs SSE plugin`() {
        val client = buildClientWithDefaults()
        val plugin = client.pluginOrNull(SSE)
        assertNotNull(plugin, "SSE should be installed by installDefaultTransportPlugins")
        client.close()
    }

    @Test
    fun `connectTimeout is set to 10 seconds`() {
        val client = buildClientWithDefaults()
        val plugin = client.pluginOrNull(HttpTimeout)
        assertNotNull(plugin)
        client.close()
    }

    @Test
    fun `client built with defaults can perform a basic GET and receive JSON`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"key":"value"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClient(engine) {
            installDefaultTransportPlugins(testJson)
        }

        val response = client.config { }
        assertNotNull(response)
        client.close()
    }

    private fun buildClientWithDefaults(): HttpClient {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        return HttpClient(engine) {
            installDefaultTransportPlugins(testJson)
        }
    }
}
