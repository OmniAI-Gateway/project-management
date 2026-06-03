package org.omniai.sdk.ports.outbound.http

internal fun resolveUrlTemplate(
    rawUrl: String,
    pathParams: Map<String, String>,
): String {
    var resolvedUrl = rawUrl

    pathParams.forEach { (key, value) ->
        resolvedUrl = resolvedUrl.replace("{$key}", value)
    }

    require(!hasUnresolvedPlaceholders(resolvedUrl)) {
        "Unresolved path parameters in URL: $resolvedUrl"
    }

    return resolvedUrl
}

private fun hasUnresolvedPlaceholders(url: String): Boolean {
    val start = url.indexOf('{')
    val end = url.indexOf('}')
    return start != -1 && end != -1 && end > start
}