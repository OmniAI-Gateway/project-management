package org.omniai.mcp.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.omniai.mcp.capabilities.tool.ToolContent
import org.omniai.mcp.config.parsing.YamlConfigParser
import org.omniai.mcp.config.registry.DefaultConfigMapper
import org.omniai.mcp.config.registry.DynamicMcpTool
import org.omniai.mcp.config.registry.DynamicToolRegistry
import org.omniai.mcp.config.registry.buildRequestBody
import org.omniai.mcp.config.registry.extractPathParams
import org.omniai.mcp.config.registry.extractUriParams
import org.omniai.mcp.config.registry.parseHttpMethod
import org.omniai.mcp.config.registry.resolveUrlWithParams
import org.omniai.mcp.config.schema.BodyMappingDefinition
import org.omniai.mcp.config.schema.BodyTemplate
import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.HttpMethod
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.http.RequestConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ──────────────────────────────────────────────────────────────────────
// Mock HttpTransportClient for testing without actual HTTP calls.
// Records the last request and returns a configurable response.
// ──────────────────────────────────────────────────────────────────────

class MockHttpTransportClient : HttpTransportClient {
    var lastConfig: RequestConfig<*>? = null
    var responseToReturn: HttpCallResult<String> = HttpCallResult.Success("{}")

    override fun bindResponseMetadata(context: IncomingContext, headerNames: Set<String>): TypedMap {
        return TypedMap()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T, V> execute(
        config: RequestConfig<V>,
        responseSerializer: KSerializer<T>
    ): HttpCallResult<T> {
        lastConfig = config
        return responseToReturn as HttpCallResult<T>
    }

    override fun <T, V> listen(
        config: RequestConfig<V>,
        eventName: String?,
        responseSerializer: KSerializer<T>
    ): Flow<HttpCallResult<T>> {
        throw UnsupportedOperationException("Not used in dynamic tool tests")
    }

    override fun <E : Any, V> listenMany(
        config: RequestConfig<V>,
        serializersByEvent: Map<String, KSerializer<out E>>
    ): Flow<HttpCallResult<E>> {
        throw UnsupportedOperationException("Not used in dynamic tool tests")
    }
}

// ──────────────────────────────────────────────────────────────────────
// Test YAML fixtures
// ──────────────────────────────────────────────────────────────────────

private val SIMPLE_TOOL_YAML = """
tools:
  - name: get_weather
    description: "Get current weather for a city"
    inputSchema:
      properties:
        city:
          type: string
          description: "City name"
        units:
          type: string
          description: "Temperature units"
      required:
        - city
    http:
      url: "https://api.weather.com/v1/current/{city}"
      method: GET
      headers:
        X-Api-Key: "test-key"
""".trimIndent()

private val TOOL_WITH_BODY_YAML = """
tools:
  - name: create_issue
    description: "Create a GitHub issue"
    inputSchema:
      properties:
        title:
          type: string
          description: "Issue title"
        body:
          type: string
          description: "Issue body"
      required:
        - title
    http:
      url: "https://api.github.com/repos/owner/repo/issues"
      method: POST
      headers:
        Authorization: "Bearer ghp_test"
      bodyMapping:
        contentType: "application/json"
""".trimIndent()

private val NESTED_SCHEMA_YAML = """
tools:
  - name: complex_tool
    description: "Tool with nested schema"
    inputSchema:
      properties:
        name:
          type: string
        tags:
          type: array
          items:
            type: string
        address:
          type: object
          properties:
            street:
              type: string
            city:
              type: string
          required:
            - street
      required:
        - name
    http:
      url: "https://api.example.com/items"
      method: POST
      bodyMapping:
        contentType: "application/json"
""".trimIndent()

private val RESOURCE_YAML = """
resources:
  - name: user_profile
    uriTemplate: "api://users/{userId}"
    description: "Fetch a user profile by ID"
    mimeType: "application/json"
    fetch:
      url: "https://api.example.com/users/{userId}"
      method: GET
      headers:
        Authorization: "Bearer token123"
""".trimIndent()

private val FULL_CONFIG_YAML = """
tools:
  - name: get_weather
    description: "Get weather"
    inputSchema:
      properties:
        city:
          type: string
      required:
        - city
    http:
      url: "https://api.weather.com/{city}"
      method: GET
  - name: post_data
    description: "Post some data"
    inputSchema:
      properties:
        payload:
          type: string
    http:
      url: "https://api.example.com/data"
      method: POST
      bodyMapping:
        contentType: "application/json"
resources:
  - name: user_profile
    uriTemplate: "api://users/{userId}"
    description: "User profile"
    mimeType: "application/json"
    fetch:
      url: "https://api.example.com/users/{userId}"
      method: GET
""".trimIndent()

// ──────────────────────────────────────────────────────────────────────
// 1. YAML Parsing Tests
// ──────────────────────────────────────────────────────────────────────

class YamlParsingTest {

    private val parser = YamlConfigParser()

    @Test
    fun parseSimpleTool() {
        val config = parser.parse(SIMPLE_TOOL_YAML)

        assertEquals(1, config.tools.size)
        assertEquals(0, config.resources.size)

        val tool = config.tools[0]
        assertEquals("get_weather", tool.name)
        assertEquals("Get current weather for a city", tool.description)
        assertEquals(2, tool.inputSchema.properties.size)
        assertEquals(listOf("city"), tool.inputSchema.required)
        assertEquals("https://api.weather.com/v1/current/{city}", tool.http.url)
        assertEquals("GET", tool.http.method)
        assertEquals("test-key", tool.http.headers["X-Api-Key"])
    }

    @Test
    fun parseToolWithBody() {
        val config = parser.parse(TOOL_WITH_BODY_YAML)

        val tool = config.tools[0]
        assertEquals("create_issue", tool.name)
        assertEquals("POST", tool.http.method)
        assertNotNull(tool.http.bodyMapping)
        assertEquals("application/json", tool.http.bodyMapping!!.contentType)
        assertNull(tool.http.bodyMapping!!.template)
        assertEquals("Bearer ghp_test", tool.http.headers["Authorization"])
    }

    @Test
    fun parseNestedSchema() {
        val config = parser.parse(NESTED_SCHEMA_YAML)

        val tool = config.tools[0]
        val props = tool.inputSchema.properties

        assertEquals("string", props["name"]?.type)

        val tags = props["tags"]
        assertEquals("array", tags?.type)
        assertEquals("string", tags?.items?.type)

        val address = props["address"]
        assertEquals("object", address?.type)
        assertNotNull(address?.properties)
        assertEquals("string", address?.properties?.get("street")?.type)
        assertEquals("string", address?.properties?.get("city")?.type)
        assertEquals(listOf("street"), address?.required)
    }

    @Test
    fun parseResource() {
        val config = parser.parse(RESOURCE_YAML)

        assertEquals(0, config.tools.size)
        assertEquals(1, config.resources.size)

        val resource = config.resources[0]
        assertEquals("user_profile", resource.name)
        assertEquals("api://users/{userId}", resource.uriTemplate)
        assertEquals("Fetch a user profile by ID", resource.description)
        assertEquals("application/json", resource.mimeType)
        assertEquals("https://api.example.com/users/{userId}", resource.fetch.url)
        assertEquals("GET", resource.fetch.method)
        assertEquals("Bearer token123", resource.fetch.headers["Authorization"])
    }

    @Test
    fun parseFullConfig() {
        val config = parser.parse(FULL_CONFIG_YAML)

        assertEquals(2, config.tools.size)
        assertEquals(1, config.resources.size)
        assertEquals("get_weather", config.tools[0].name)
        assertEquals("post_data", config.tools[1].name)
        assertEquals("user_profile", config.resources[0].name)
    }

    @Test
    fun parseEmptyConfig() {
        val config = parser.parse("tools: []\nresources: []")
        assertEquals(0, config.tools.size)
        assertEquals(0, config.resources.size)
    }
}

// ──────────────────────────────────────────────────────────────────────
// 2. Schema Mapping Tests
// ──────────────────────────────────────────────────────────────────────

class SchemaMappingTest {

    private val parser = YamlConfigParser()
    private val mapper = DefaultConfigMapper()

    @Test
    fun mapSimpleToolSchema() {
        val config = parser.parse(SIMPLE_TOOL_YAML)
        val schema = mapper.mapToolSchema(config.tools[0].inputSchema)

        assertNotNull(schema.properties)
        val props = schema.properties!!
        assertTrue(props.containsKey("city"))
        assertTrue(props.containsKey("units"))

        // Check city property has type and description
        val cityProp = props["city"] as JsonObject
        assertEquals("string", cityProp["type"]?.jsonPrimitive?.content)
        assertEquals("City name", cityProp["description"]?.jsonPrimitive?.content)

        // Check required
        assertEquals(listOf("city"), schema.required)
    }

    @Test
    fun mapNestedObjectSchema() {
        val config = parser.parse(NESTED_SCHEMA_YAML)
        val schema = mapper.mapToolSchema(config.tools[0].inputSchema)

        val props = schema.properties!!

        // Check array type with items
        val tagsProp = props["tags"] as JsonObject
        assertEquals("array", tagsProp["type"]?.jsonPrimitive?.content)
        val items = tagsProp["items"] as JsonObject
        assertEquals("string", items["type"]?.jsonPrimitive?.content)

        // Check object type with nested properties
        val addressProp = props["address"] as JsonObject
        assertEquals("object", addressProp["type"]?.jsonPrimitive?.content)
        val nestedProps = addressProp["properties"] as JsonObject
        assertTrue(nestedProps.containsKey("street"))
        assertTrue(nestedProps.containsKey("city"))
    }

    @Test
    fun mapToolSchemaWithNoRequired() {
        val yaml = """
tools:
  - name: optional_tool
    description: "All optional"
    inputSchema:
      properties:
        optionalField:
          type: string
    http:
      url: "https://api.example.com"
      method: GET
""".trimIndent()
        val config = parser.parse(yaml)
        val schema = mapper.mapToolSchema(config.tools[0].inputSchema)

        assertNull(schema.required)
    }
}

// ──────────────────────────────────────────────────────────────────────
// 3. URL Resolution Tests
// ──────────────────────────────────────────────────────────────────────

class UrlResolutionTest {

    @Test
    fun extractSinglePathParam() {
        val input = JsonObject(mapOf("userId" to JsonPrimitive("42")))
        val params = extractPathParams("https://api.example.com/users/{userId}", input)

        assertEquals(mapOf("userId" to "42"), params)
    }

    @Test
    fun extractMultiplePathParams() {
        val input = JsonObject(mapOf(
            "userId" to JsonPrimitive("42"),
            "postId" to JsonPrimitive("7"),
            "extra" to JsonPrimitive("ignored")
        ))
        val params = extractPathParams(
            "https://api.example.com/users/{userId}/posts/{postId}",
            input
        )

        assertEquals(mapOf("userId" to "42", "postId" to "7"), params)
    }

    @Test
    fun extractPathParamsIgnoresMissingArgs() {
        val input = JsonObject(mapOf("otherField" to JsonPrimitive("value")))
        val params = extractPathParams("https://api.example.com/users/{userId}", input)

        assertTrue(params.isEmpty())
    }

    @Test
    fun resolveUrlWithSingleParam() {
        val resolved = resolveUrlWithParams(
            "https://api.example.com/users/{userId}",
            mapOf("userId" to "42")
        )
        assertEquals("https://api.example.com/users/42", resolved)
    }

    @Test
    fun resolveUrlWithMultipleParams() {
        val resolved = resolveUrlWithParams(
            "https://api.example.com/users/{userId}/posts/{postId}",
            mapOf("userId" to "42", "postId" to "7")
        )
        assertEquals("https://api.example.com/users/42/posts/7", resolved)
    }

    @Test
    fun resolveUrlWithNoParams() {
        val resolved = resolveUrlWithParams(
            "https://api.example.com/items",
            emptyMap()
        )
        assertEquals("https://api.example.com/items", resolved)
    }
}

// ──────────────────────────────────────────────────────────────────────
// 4. Body Builder Tests
// ──────────────────────────────────────────────────────────────────────

class BodyBuilderTest {

    @Test
    fun nullMappingReturnsNull() {
        val input = JsonObject(mapOf("key" to JsonPrimitive("value")))
        val result = buildRequestBody(null, input)
        assertNull(result)
    }

    @Test
    fun noTemplateForwardsAllArgs() {
        val input = JsonObject(mapOf(
            "title" to JsonPrimitive("Hello"),
            "body" to JsonPrimitive("World")
        ))
        val mapping = BodyMappingDefinition(contentType = "application/json", template = null)
        val result = buildRequestBody(mapping, input)

        assertNotNull(result)
        val parsed = Json.decodeFromString(JsonObject.serializer(), result)
        assertEquals("Hello", parsed["title"]?.jsonPrimitive?.content)
        assertEquals("World", parsed["body"]?.jsonPrimitive?.content)
    }

    @Test
    fun templateMapsSpecificFields() {
        val input = JsonObject(mapOf(
            "issueTitle" to JsonPrimitive("Bug"),
            "issueBody" to JsonPrimitive("Details"),
            "extraField" to JsonPrimitive("Ignored")
        ))
        val mapping = BodyMappingDefinition(
            template = BodyTemplate(
                fields = mapOf(
                    "title" to "issueTitle",
                    "body" to "issueBody"
                )
            )
        )
        val result = buildRequestBody(mapping, input)

        assertNotNull(result)
        val parsed = Json.decodeFromString(JsonObject.serializer(), result)
        assertEquals("Bug", parsed["title"]?.jsonPrimitive?.content)
        assertEquals("Details", parsed["body"]?.jsonPrimitive?.content)
        assertNull(parsed["extraField"])
    }

    @Test
    fun templateSkipsMissingSourceFields() {
        val input = JsonObject(mapOf("existing" to JsonPrimitive("value")))
        val mapping = BodyMappingDefinition(
            template = BodyTemplate(
                fields = mapOf(
                    "target1" to "existing",
                    "target2" to "missing"
                )
            )
        )
        val result = buildRequestBody(mapping, input)

        assertNotNull(result)
        val parsed = Json.decodeFromString(JsonObject.serializer(), result)
        assertEquals("value", parsed["target1"]?.jsonPrimitive?.content)
        assertNull(parsed["target2"])
    }
}

// ──────────────────────────────────────────────────────────────────────
// 5. HTTP Method Parsing Tests
// ──────────────────────────────────────────────────────────────────────

class HttpMethodParsingTest {

    @Test
    fun parsesAllMethods() {
        assertEquals(HttpMethod.GET, parseHttpMethod("GET"))
        assertEquals(HttpMethod.POST, parseHttpMethod("POST"))
        assertEquals(HttpMethod.PUT, parseHttpMethod("PUT"))
        assertEquals(HttpMethod.DELETE, parseHttpMethod("DELETE"))
        assertEquals(HttpMethod.PATCH, parseHttpMethod("PATCH"))
        assertEquals(HttpMethod.HEAD, parseHttpMethod("HEAD"))
        assertEquals(HttpMethod.OPTIONS, parseHttpMethod("OPTIONS"))
    }

    @Test
    fun caseInsensitiveParsing() {
        assertEquals(HttpMethod.GET, parseHttpMethod("get"))
        assertEquals(HttpMethod.POST, parseHttpMethod("post"))
        assertEquals(HttpMethod.PUT, parseHttpMethod("Put"))
    }

    @Test
    fun throwsOnUnknownMethod() {
        try {
            parseHttpMethod("INVALID")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// 6. URI Param Extraction Tests
// ──────────────────────────────────────────────────────────────────────

class UriParamExtractionTest {

    @Test
    fun extractsSingleParam() {
        val params = extractUriParams("api://users/{userId}", "api://users/42")
        assertEquals(mapOf("userId" to "42"), params)
    }

    @Test
    fun extractsMultipleParams() {
        val params = extractUriParams(
            "api://users/{userId}/posts/{postId}",
            "api://users/42/posts/7"
        )
        assertEquals(mapOf("userId" to "42", "postId" to "7"), params)
    }

    @Test
    fun handlesNoPlaceholders() {
        val params = extractUriParams("api://items", "api://items")
        assertTrue(params.isEmpty())
    }
}

// ──────────────────────────────────────────────────────────────────────
// 7. DynamicMcpTool Integration Tests (with mock HTTP)
// ──────────────────────────────────────────────────────────────────────

class DynamicMcpToolTest {

    @Test
    fun executeToolWithPathParamAndHeaders() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.Success("""{"temp": 22, "unit": "C"}""")

        val parser = YamlConfigParser()
        val mapper = DefaultConfigMapper()
        val config = parser.parse(SIMPLE_TOOL_YAML)
        val toolDef = config.tools[0]
        val schema = mapper.mapToolSchema(toolDef.inputSchema)

        val tool = DynamicMcpTool(
            name = toolDef.name,
            description = toolDef.description,
            toolDefinition = toolDef,
            httpClient = mockClient,
            schemaDefinition = schema
        )

        val input = JsonObject(mapOf(
            "city" to JsonPrimitive("London"),
            "units" to JsonPrimitive("metric")
        ))

        val result = tool.execute(input)

        // Verify the tool result
        assertEquals(false, result.isError)
        assertEquals(1, result.content.size)
        val textContent = result.content[0] as ToolContent.Text
        assertTrue(textContent.text.contains("temp"))

        // Verify the HTTP request was correctly constructed
        val sentConfig = mockClient.lastConfig!!
        assertEquals("https://api.weather.com/v1/current/London", sentConfig.url)
        assertEquals(HttpMethod.GET, sentConfig.method)
        assertEquals(listOf("test-key"), sentConfig.headers["X-Api-Key"])
    }

    @Test
    fun executeToolWithPostBody() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.Success("""{"id": 1}""")

        val parser = YamlConfigParser()
        val mapper = DefaultConfigMapper()
        val config = parser.parse(TOOL_WITH_BODY_YAML)
        val toolDef = config.tools[0]
        val schema = mapper.mapToolSchema(toolDef.inputSchema)

        val tool = DynamicMcpTool(
            name = toolDef.name,
            description = toolDef.description,
            toolDefinition = toolDef,
            httpClient = mockClient,
            schemaDefinition = schema
        )

        val input = JsonObject(mapOf(
            "title" to JsonPrimitive("Bug report"),
            "body" to JsonPrimitive("Something is broken")
        ))

        val result = tool.execute(input)

        assertEquals(false, result.isError)

        // Verify POST was used with body
        val sentConfig = mockClient.lastConfig!!
        assertEquals("https://api.github.com/repos/owner/repo/issues", sentConfig.url)
        assertEquals(HttpMethod.POST, sentConfig.method)
        assertEquals(listOf("Bearer ghp_test"), sentConfig.headers["Authorization"])
        assertNotNull(sentConfig.body)

        // Verify body contains input args
        val body = Json.decodeFromString(JsonObject.serializer(), sentConfig.body as String)
        assertEquals("Bug report", body["title"]?.jsonPrimitive?.content)
        assertEquals("Something is broken", body["body"]?.jsonPrimitive?.content)
    }

    @Test
    fun executeToolHandlesApiError() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.ApiError(404, "Not Found")

        val parser = YamlConfigParser()
        val mapper = DefaultConfigMapper()
        val config = parser.parse(SIMPLE_TOOL_YAML)
        val toolDef = config.tools[0]
        val schema = mapper.mapToolSchema(toolDef.inputSchema)

        val tool = DynamicMcpTool(
            name = toolDef.name,
            description = toolDef.description,
            toolDefinition = toolDef,
            httpClient = mockClient,
            schemaDefinition = schema
        )

        val input = JsonObject(mapOf("city" to JsonPrimitive("Unknown")))
        val result = tool.execute(input)

        assertTrue(result.isError)
        val text = (result.content[0] as ToolContent.Text).text
        assertTrue(text.contains("404"))
        assertTrue(text.contains("Not Found"))
    }

    @Test
    fun executeToolHandlesNetworkError() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.NetworkError(Exception("Connection refused"))

        val parser = YamlConfigParser()
        val mapper = DefaultConfigMapper()
        val config = parser.parse(SIMPLE_TOOL_YAML)
        val toolDef = config.tools[0]
        val schema = mapper.mapToolSchema(toolDef.inputSchema)

        val tool = DynamicMcpTool(
            name = toolDef.name,
            description = toolDef.description,
            toolDefinition = toolDef,
            httpClient = mockClient,
            schemaDefinition = schema
        )

        val input = JsonObject(mapOf("city" to JsonPrimitive("London")))
        val result = tool.execute(input)

        assertTrue(result.isError)
        val text = (result.content[0] as ToolContent.Text).text
        assertTrue(text.contains("Network error"))
        assertTrue(text.contains("Connection refused"))
    }
}

// ──────────────────────────────────────────────────────────────────────
// 8. Full Pipeline Test
// ──────────────────────────────────────────────────────────────────────

class FullPipelineTest {

    @Test
    fun fullPipelineParseRegisterAndExecute() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.Success("""{"forecast": "sunny"}""")

        val registry = DynamicToolRegistry(
            parser = YamlConfigParser(),
            configMapper = DefaultConfigMapper(),
            httpClient = mockClient
        )

        val result = registry.loadFromYaml(FULL_CONFIG_YAML)

        // Verify correct number of tools and resources were created
        assertEquals(2, result.tools.size)
        assertEquals(1, result.resources.size)

        // Verify tool properties
        assertEquals("get_weather", result.tools[0].name)
        assertEquals("post_data", result.tools[1].name)
        assertEquals("user_profile", result.resources[0].name)

        // Execute the first tool (cast from McpTool<*> to DynamicMcpTool to avoid star projection)
        val weatherTool = result.tools[0] as DynamicMcpTool
        val input = JsonObject(mapOf("city" to JsonPrimitive("Paris")))
        val toolResult = weatherTool.execute(input)

        assertEquals(false, toolResult.isError)
        val text = (toolResult.content[0] as ToolContent.Text).text
        assertTrue(text.contains("forecast"))

        // Verify the request was constructed correctly
        val sentConfig = mockClient.lastConfig!!
        assertEquals("https://api.weather.com/Paris", sentConfig.url)
        assertEquals(HttpMethod.GET, sentConfig.method)
    }

    @Test
    fun fullPipelineResourceHandler() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.Success("""{"name": "John", "id": 42}""")

        val registry = DynamicToolRegistry(
            parser = YamlConfigParser(),
            configMapper = DefaultConfigMapper(),
            httpClient = mockClient
        )

        val result = registry.loadFromYaml(RESOURCE_YAML)
        assertEquals(1, result.resources.size)

        val resource = result.resources[0]
        assertEquals("api://users/{userId}", resource.uri)
        assertEquals("user_profile", resource.name)

        // Invoke the resource handler with a concrete URI
        val content = resource.handler("api://users/42")

        assertEquals("api://users/42", content.uri)
        assertEquals("application/json", content.mimeType)
        assertNotNull(content.text)
        assertTrue(content.text!!.contains("John"))

        // Verify the HTTP request was correct
        val sentConfig = mockClient.lastConfig!!
        assertEquals("https://api.example.com/users/42", sentConfig.url)
        assertEquals(HttpMethod.GET, sentConfig.method)
        assertEquals(listOf("Bearer token123"), sentConfig.headers["Authorization"])
    }

    @Test
    fun fullPipelineResourceHandlerHandlesError() = runTest {
        val mockClient = MockHttpTransportClient()
        mockClient.responseToReturn = HttpCallResult.ApiError(500, "Internal Server Error")

        val registry = DynamicToolRegistry(
            parser = YamlConfigParser(),
            configMapper = DefaultConfigMapper(),
            httpClient = mockClient
        )

        val result = registry.loadFromYaml(RESOURCE_YAML)
        val resource = result.resources[0]

        val content = resource.handler("api://users/999")

        assertEquals("text/plain", content.mimeType)
        assertNotNull(content.text)
        assertTrue(content.text!!.contains("500"))
    }
}
