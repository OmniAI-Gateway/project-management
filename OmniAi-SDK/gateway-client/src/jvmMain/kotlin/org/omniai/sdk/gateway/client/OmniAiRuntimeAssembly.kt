package org.omniai.sdk.gateway.client

import org.omniai.gateway.services.PipelineBackedInferenceService
import org.omniai.gateway.services.routingInferenceServiceFactory
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.pipeline.GatewayPipelineBuilder
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter
import org.omniai.sdk.interceptors.auth.AuthenticationInterceptor
import org.omniai.sdk.interceptors.auth.PolicyEnforcerInterceptor
import org.omniai.sdk.core.pipeline.getInterceptorPriority
import org.omniai.sdk.gateway.client.core.OmniAiConfig
import org.omniai.sdk.gateway.client.auth.SecurityConfig
import org.omniai.sdk.gateway.client.core.OmniAiRuntime
import org.omniai.sdk.gateway.client.core.ExecutionMode
import org.omniai.sdk.interceptors.auth.domain.AuthSetupConfig

suspend fun OmniAiConfig.assemble(
    httpClient: HttpTransportClient
): OmniAiRuntime {
    val service = resolveService(httpClient)
    return OmniAiRuntime(
        service = service,
        metadata = TypedMap()
    )
}

suspend fun OmniAiConfig.startServer(
    httpClient: HttpTransportClient,
    serverLogic: () -> Unit
) {
    val runtime = assemble(httpClient)
    
    // Initialize Inbounds and pass them to their registered connectors
    inbounds.openAiConnector?.let { connector ->
        val adapter = OpenAiInboundAdapter(runtime.service)
        connector.connect(adapter)
    }
    
    inbounds.anthropicConnector?.let { connector ->
        val adapter = AnthropicInboundAdapter(runtime.service)
        connector.connect(adapter)
    }
    
    inbounds.geminiConnector?.let { connector ->
        val adapter = GeminiInboundAdapter(runtime.service)
        connector.connect(adapter)
    }
    
    inbounds.customFactories.forEach { (_, setup) ->
        val adapter = setup.factory(runtime.service)
        setup.connect(adapter)
    }
    
    // Once everything is connected and assembled, start the user's server
    serverLogic()
}

private suspend fun OmniAiConfig.resolveService(httpClient: HttpTransportClient): InferenceServicePort {
    return when (val selected = execution) {
        is ExecutionMode.CustomService -> selected.service
        is ExecutionMode.NativePipeline -> {
            val terminal = routingInferenceServiceFactory(selected.outbounds)
            val interceptorsList = buildInterceptors(selected.interceptors, httpClient)
            val pipeline = GatewayPipelineBuilder().apply {
                interceptorsList.forEach { install(it) }
                installService(terminal)
            }.build()
            PipelineBackedInferenceService(pipeline)
        }
    }
}

private suspend fun OmniAiConfig.buildInterceptors(
    configuredInterceptors: List<Interceptor>,
    httpClient: HttpTransportClient
): List<Interceptor> {
    val resolved = mutableListOf<Interceptor>()
    resolved.addAll(buildAuthorizationInterceptors(security, httpClient))
    resolved += configuredInterceptors

    resolved.sortByDescending { getInterceptorPriority(it) }

    return resolved
}

private suspend fun buildAuthorizationInterceptors(
    config: SecurityConfig,
    httpClient: HttpTransportClient
): List<Interceptor> {
    val authNInterceptor = when (val authConfig = config.authentication) {
        is AuthorizationServerConfig.None -> {
            AuthenticationInterceptor.build(setup = AuthSetupConfig.Off)
        }
        is AuthorizationServerConfig.Custom -> {
            AuthenticationInterceptor(authConfig.authenticator)
        }
        is AuthorizationServerConfig.Discovery -> {
            AuthenticationInterceptor.build(
                setup = AuthSetupConfig.Discovery(
                    discoveryUrl = authConfig.discoveryUrl,
                    httpClient = httpClient,
                    expectedAudience = authConfig.expectedAudience,
                    authClientId = authConfig.clientId ?: "",
                    authClientSecret = authConfig.clientSecret ?: ""
                ),
                introspectionCache = authConfig.introspectionCache,
                positiveCacheTtl = authConfig.positiveCacheTtl,
                negativeCacheTtl = authConfig.negativeCacheTtl,
            )
        }
    }
    
    val authZInterceptor = PolicyEnforcerInterceptor(
        inputProvider = config.authorization.inputProvider,
        pdp = config.authorization.pdp
    )

    return listOf(authNInterceptor, authZInterceptor)
}