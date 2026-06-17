package org.omniai.sdk.interceptors.mcpBroker

import io.modelcontextprotocol.kotlin.sdk.client.Client
import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult

class McpBrokerInterceptor(
    private val mcpClient: Client
) : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        // TODO: 1. Executar a pipeline com chain.proceed(context) para obter a resposta do LLM
        // TODO: 2. Verificar se a resposta contém pedidos de execução de ferramentas (tool_calls)
        // TODO: 3. Se NÃO contiver ferramentas, retornar imediatamente o resultado (é a resposta final de texto)
        // TODO: 4. Se contiver ferramentas, travar o fluxo e executá-las uma a uma usando o mcpClient
        // TODO: 5. Atualizar o histórico do contexto com os pedidos do modelo e os resultados técnicos obtidos do MCP
        return chain.proceed(context)
    }
}