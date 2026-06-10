package org.omniai.mcp.config.registry

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.omniai.mcp.config.schema.BodyMappingDefinition
import org.omniai.sdk.ports.outbound.http.HttpMethod

/**
 * Extracts `{placeholder}` names from a URL template and maps them to values
 * from the tool's [JsonObject] input arguments.
 *
 * For example, given template `"https://api.example.com/users/{userId}/posts/{postId}"`
 * and input `{"userId": "42", "postId": "7", "title": "Hello"}`,
 * returns `{"userId": "42", "postId": "7"}`.
 *
 * @param urlTemplate URL with `{paramName}` placeholders.
 * @param input Tool input arguments as a [JsonObject].
 * @return Map of placeholder name to resolved value.
 */
fun extractPathParams(urlTemplate: String, input: JsonObject): Map<String, String> {
    val placeholderRegex = Regex("""\{(\w+)}""")
    val placeholderNames = placeholderRegex.findAll(urlTemplate).map { it.groupValues[1] }.toSet()

    return placeholderNames.mapNotNull { name ->
        val value = input[name]?.jsonPrimitive?.content
        if (value != null) name to value else null
    }.toMap()
}

/**
 * Resolves `{placeholder}` tokens in a URL using the given params map.
 *
 * @param urlTemplate URL with `{paramName}` placeholders.
 * @param params Map of placeholder name to value.
 * @return Resolved URL string.
 */
fun resolveUrlWithParams(urlTemplate: String, params: Map<String, String>): String {
    var resolved = urlTemplate
    params.forEach { (key, value) ->
        resolved = resolved.replace("{$key}", value)
    }
    return resolved
}

/**
 * Builds the HTTP request body string from the tool input args,
 * according to the YAML bodyMapping rules.
 *
 * - If [mapping] is null, no body is sent.
 * - If [mapping] has no [BodyMappingDefinition.template], all input arguments are forwarded as JSON.
 * - If a template is defined, only the specified fields are mapped.
 *
 * @param mapping Optional body mapping definition from YAML.
 * @param input Tool input arguments as a [JsonObject].
 * @return JSON body string, or null if no body should be sent.
 */
fun buildRequestBody(mapping: BodyMappingDefinition?, input: JsonObject): String? {
    if (mapping == null) return null

    return if (mapping.template == null) {
        // Forward all input args as-is
        input.toString()
    } else {
        // Map only specified fields: targetField → value from input[sourceArgName]
        val mappedFields = mapping.template.fields.mapNotNull { (targetField, sourceArgName) ->
            val value = input[sourceArgName]
            if (value != null) targetField to value else null
        }.toMap()
        JsonObject(mappedFields).toString()
    }
}

/**
 * Parses a YAML method string into core's [HttpMethod] enum.
 *
 * @param method HTTP method string (case-insensitive): "GET", "POST", "PUT", "DELETE", "PATCH", etc.
 * @return Corresponding [HttpMethod] enum value.
 * @throws IllegalArgumentException if the method string is not recognized.
 */
fun parseHttpMethod(method: String): HttpMethod {
    return when (method.uppercase()) {
        "GET" -> HttpMethod.GET
        "POST" -> HttpMethod.POST
        "PUT" -> HttpMethod.PUT
        "DELETE" -> HttpMethod.DELETE
        "PATCH" -> HttpMethod.PATCH
        "HEAD" -> HttpMethod.HEAD
        "OPTIONS" -> HttpMethod.OPTIONS
        else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
    }
}

/**
 * Extracts parameter values from a concrete URI by matching against a URI template.
 *
 * For example:
 * - template: `"api://users/{userId}"`
 * - uri: `"api://users/42"`
 * - returns: `{"userId": "42"}`
 *
 * Uses a simple segment-by-segment matching approach.
 *
 * @param template URI template with `{paramName}` placeholders.
 * @param uri Concrete URI to extract values from.
 * @return Map of placeholder name to extracted value.
 */
fun extractUriParams(template: String, uri: String): Map<String, String> {
    val templateSegments = template.split("/")
    val uriSegments = uri.split("/")
    val params = mutableMapOf<String, String>()

    val placeholderRegex = Regex("""\{(\w+)}""")

    for (i in templateSegments.indices) {
        if (i >= uriSegments.size) break
        val match = placeholderRegex.matchEntire(templateSegments[i])
        if (match != null) {
            params[match.groupValues[1]] = uriSegments[i]
        }
    }

    return params
}
