package org.omniai.sdk.gateway.client

import org.omniai.gateway.dispatcher.PipelineBackedDispatcher
import org.omniai.gateway.dispatcher.routingDispatcherFactory
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.application.pipeline.GatewayPipelineBuilder
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.interceptors.auth.AuthenticationInterceptor
import org.omniai.sdk.interceptors.auth.PolicyEnforcerInterceptor
import org.omniai.sdk.application.pipeline.getInterceptorPriority
import org.omniai.sdk.gateway.client.core.OmniAiConfig
import org.omniai.sdk.gateway.client.auth.SecurityConfig
import org.omniai.sdk.gateway.client.core.OmniAiRuntime
import org.omniai.sdk.gateway.client.core.ExecutionMode
import org.omniai.sdk.gateway.client.platform.addShutdownHook
import org.omniai.sdk.interceptors.auth.domain.AuthSetupConfig

suspend fun OmniAiConfig.assemble(
    httpClient: HttpTransportClient
): OmniAiRuntime {
    val dispatcher = resolveDispatcher(httpClient)
    return OmniAiRuntime(
        dispatcher = dispatcher,
        metadata = TypedMap()
    )
}

suspend fun OmniAiConfig.startServer(
    httpClient: HttpTransportClient,
    onStart: () -> Unit,
    onEnd: () -> Unit = {}
) {
    val runtime = assemble(httpClient)

    inbounds.setups.forEach { setup ->
        val adapter = setup.factory(runtime.dispatcher)
        setup.connect(adapter)
    }
    
    addShutdownHook {
        onEnd()
    }

    onStart()
}

private suspend fun OmniAiConfig.resolveDispatcher(httpClient: HttpTransportClient): DispatcherPort {
    return when (val selected = execution) {
        is ExecutionMode.CustomDispatcher -> selected.dispatcher
        is ExecutionMode.NativePipeline -> {
            val terminal = routingDispatcherFactory(selected.outbounds)
            val interceptorsList = buildInterceptors(selected.interceptors, httpClient)
            val pipeline = GatewayPipelineBuilder().apply {
                interceptorsList.forEach { install(it) }
                installDispatcher(terminal)
            }.build()
            PipelineBackedDispatcher(pipeline)
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