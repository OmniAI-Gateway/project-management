package org.omniai.sdk.gateway.client

import org.omniai.gateway.services.gatewayServiceAssembler
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter
import org.omniai.sdk.interceptors.auth.AuthContextInterceptor
import org.omniai.sdk.interceptors.auth.ConfigSource
import org.omniai.sdk.interceptors.auth.HttpAuthSecurityClient
import org.omniai.sdk.interceptors.auth.JoseJwtTokenAuthenticator
import org.omniai.sdk.interceptors.auth.MapConfigSource
import org.omniai.sdk.interceptors.auth.OidcDiscovery
import org.omniai.sdk.interceptors.auth.PassThroughTokenAuthenticator

suspend fun GatewayDefinition.assemble(
    httpClient: HttpTransportClient
): GatewayRuntime {
    val service = resolveService(httpClient)

    val openAiInbound = if (inbounds.installOpenAi) OpenAiInboundAdapter(service) else null
    val anthropicInbound = if (inbounds.installAnthropic) AnthropicInboundAdapter(service) else null
    val geminiInbound = if (inbounds.installGemini) GeminiInboundAdapter(service) else null

    val customInbounds = inbounds.customFactories.mapValues { (_, factory) -> factory(service) }

    return GatewayRuntime(
        service = service,
        inbounds = GatewayInboundAdapters(
            openAi = openAiInbound,
            anthropic = anthropicInbound,
            gemini = geminiInbound,
            custom = customInbounds
        ),
        metadata = TypedMap()
    )
}

suspend fun GatewayDefinition.start(httpClient: HttpTransportClient): GatewayRuntime {
    val runtime = assemble(httpClient)
    networkAdapters.forEach { adapter ->
        adapter.connect(runtime)
    }
    return runtime
}

private suspend fun GatewayDefinition.resolveService(httpClient: HttpTransportClient): InferenceServicePort {
    return when (val selected = aiServices) {
        is AiServiceSelection.Custom -> selected.service
        AiServiceSelection.BuiltIn -> gatewayServiceAssembler(
            outbounds = outboundPorts,
            configSource = null,
            httpClient = null,
            interceptors = buildInterceptors(httpClient)
        )
    }
}

private suspend fun GatewayDefinition.buildInterceptors(httpClient: HttpTransportClient): List<Interceptor> {
    val resolved = mutableListOf<Interceptor>()

    resolved += buildAuthorizationInterceptor(authorizationServer, httpClient)

    if (metrics.enabled) {
        resolved += MetricsInterceptor()
        resolved += metrics.interceptors
    }

    resolved += interceptors.global
    interceptors.localByProvider.forEach { (provider, list) ->
        list.forEach { interceptor ->
            resolved += providerScoped(provider, interceptor)
        }
    }

    return resolved
}

private suspend fun buildAuthorizationInterceptor(
    config: AuthorizationServerConfig,
    httpClient: HttpTransportClient
): Interceptor {
    return when (config) {
        AuthorizationServerConfig.None -> AuthContextInterceptor(PassThroughTokenAuthenticator())
        is AuthorizationServerConfig.Custom -> AuthContextInterceptor(config.authenticator)
        is AuthorizationServerConfig.Discovery -> {
            val configSource = config.toConfigSource()
            val discovery = OidcDiscovery(httpClient)
            val metadata = when (val result = discovery.fetchMetadata(config.discoveryUrl)) {
                is HttpCallResult.Success -> result.data
                is HttpCallResult.ApiError -> error("Discovery failed with status ${result.code}: ${result.message}")
                is HttpCallResult.NetworkError -> error("Discovery network failure: ${result.exception.message}")
                is HttpCallResult.SerializationError -> error("Discovery parsing failure: ${result.exception.message}")
                is HttpCallResult.UnknownError -> error("Discovery unknown failure: ${result.exception.message}")
            }

            val infra = HttpAuthSecurityClient(
                httpClient = httpClient,
                jwksUri = metadata.jwksUri,
                tokenEndpoint = metadata.tokenEndpoint,
                configSource = configSource
            )

            val authenticator = JoseJwtTokenAuthenticator(
                infra = infra,
                expectedIssuer = metadata.issuer,
                expectedAudience = config.expectedAudience
            )

            AuthContextInterceptor(authenticator)
        }
    }
}

private fun AuthorizationServerConfig.Discovery.toConfigSource(): ConfigSource {
    val values = mutableMapOf<String, String>()
    clientId?.let { values["AUTH_CLIENT_ID"] = it }
    clientSecret?.let { values["AUTH_CLIENT_SECRET"] = it }
    return MapConfigSource(values)
}

private fun providerScoped(provider: Provider, delegate: Interceptor): Interceptor =
    Interceptor { context, chain ->
        if (context.request.provider.value == provider.value) {
            delegate.handle(context, chain)
        } else {
            chain.proceed(context)
        }
    }

