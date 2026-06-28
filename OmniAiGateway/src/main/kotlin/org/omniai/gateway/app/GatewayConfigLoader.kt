package org.omniai.gateway.app

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

fun loadGatewayConfig(): GatewayConfig =
    ConfigFactory.load().let { appConfig ->
        val providers =
            ProviderKind.entries.mapNotNull { kind ->
                appConfig.loadProviderConfig(kind)
            }

        check(providers.isNotEmpty()) {
            "No gateway providers configured. Add at least one provider block under gateway.providers.*"
        }

        GatewayConfig(
            port = System.getenv("PORT")?.toIntOrNull() ?: appConfig.safeInt("gateway.port", 1900),
            providers = providers,
            telemetryEnabled =
                appConfig.firstBoolean(
                    paths = listOf("gateway.telemetryEnabled", "gateway.metrics.enabled"),
                    defaultValue = true,
                ),
            otelEnabled =
                appConfig.firstBoolean(
                    paths = listOf("gateway.otelEnabled", "gateway.metrics.otel.enabled"),
                    defaultValue = false,
                ),
            otelCollectorEndpoint =
                appConfig
                    .firstString(
                        paths = listOf("gateway.otelCollectorEndpoint", "gateway.metrics.otel.collectorEndpoint"),
                        defaultValue = "",
                    ).ifBlank { null },
            authConfig = appConfig.loadAuthConfig(),
        )
    }

private fun Config.loadAuthConfig(): AuthorizationServerGatewayConfig {
    val discoveryUrl =
        System.getenv("AUTH_DISCOVERY_URL")
            ?: firstString(listOf("gateway.auth.discoveryUrl"), "")

    if (discoveryUrl.isBlank()) return AuthorizationServerGatewayConfig.None

    val audience =
        System.getenv("AUTH_AUDIENCE")
            ?: firstString(listOf("gateway.auth.audience"), "")
    require(audience.isNotBlank()) {
        "AUTH_AUDIENCE (or gateway.auth.audience) is required when AUTH_DISCOVERY_URL is set."
    }

    val clientId =
        System.getenv("AUTH_CLIENT_ID")
            ?: firstString(listOf("gateway.auth.clientId"), "").ifBlank { null }

    val clientSecret =
        System.getenv("AUTH_CLIENT_SECRET")
            ?: firstString(listOf("gateway.auth.clientSecret"), "").ifBlank { null }

    return AuthorizationServerGatewayConfig.Oidc(
        discoveryUrl = discoveryUrl,
        audience = audience,
        clientId = clientId,
        clientSecret = clientSecret,
    )
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: throw IllegalStateException("Environment variable '$name' is required")

private fun Config.safeString(
    path: String,
    defaultValue: String,
): String = runCatching { getString(path) }.getOrDefault(defaultValue)

private fun Config.safeInt(
    path: String,
    defaultValue: Int,
): Int = runCatching { getInt(path) }.getOrDefault(defaultValue)

private fun Config.safeBoolean(
    path: String,
    defaultValue: Boolean,
): Boolean = runCatching { getBoolean(path) }.getOrDefault(defaultValue)

private fun Config.firstString(
    paths: List<String>,
    defaultValue: String,
): String =
    paths.firstNotNullOfOrNull { path ->
        if (hasPath(path)) runCatching { getString(path) }.getOrNull() else null
    } ?: defaultValue

private fun Config.firstBoolean(
    paths: List<String>,
    defaultValue: Boolean,
): Boolean =
    paths.firstNotNullOfOrNull { path ->
        if (hasPath(path)) runCatching { getBoolean(path) }.getOrNull() else null
    } ?: defaultValue

private fun Config.loadProviderConfig(kind: ProviderKind): ProviderConfig? {
    val pathPrefix = "gateway.providers.${kind.configKey}"
    if (!hasPath(pathPrefix)) return null

    val apiKey = requireEnv(kind.apiKeyEnv)
    val baseUrl =
        System.getenv(kind.baseUrlEnv)
            ?: safeString("$pathPrefix.baseUrl", "")
    require(baseUrl.isNotBlank()) {
        "Missing baseUrl for provider '${kind.configKey}' at '$pathPrefix.baseUrl'"
    }

    val modelsFromEnv =
        System
            .getenv(kind.modelsEnv)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    val modelsFromConfigList =
        runCatching {
            getStringList("$pathPrefix.models")
        }.getOrElse {
            emptyList()
        }.map { it.trim() }
            .filter { it.isNotEmpty() }

    val modelsFromConfigModelKey = readStringValues("$pathPrefix.model")

    val models =
        when {
            modelsFromEnv.isNotEmpty() -> modelsFromEnv
            modelsFromConfigList.isNotEmpty() -> modelsFromConfigList
            modelsFromConfigModelKey.isNotEmpty() -> modelsFromConfigModelKey
            else -> {
                val singleModel = System.getenv(kind.modelEnv) ?: safeString("$pathPrefix.model", "")
                if (singleModel.isBlank()) emptyList() else listOf(singleModel.trim())
            }
        }

    require(models.isNotEmpty()) {
        "Provider '${kind.configKey}' is configured but no model was defined. " +
            "Set '$pathPrefix.model', '$pathPrefix.models', or env '${kind.modelsEnv}'."
    }

    return ProviderConfig(
        provider = kind,
        models = models,
        apiKey = apiKey,
        baseUrl = baseUrl,
    )
}

private fun Config.readStringValues(path: String): List<String> {
    val listValue = runCatching { getStringList(path) }.getOrNull()
    if (listValue != null) {
        return listValue.map { it.trim() }.filter { it.isNotEmpty() }
    }

    val singleValue = runCatching { getString(path) }.getOrNull()
    return singleValue
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::listOf)
        .orEmpty()
}
