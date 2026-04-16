package org.omniai.sk.gateway.telemetry


import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.omniai.gateway.metrics.TelemetryTracer

class JvmTelemetryTracer(
    private val openTelemetry: OpenTelemetry,
    instrumentationScopeName: String = "omniai-gateway-sdk"
) : TelemetryTracer {

    // Cria o tracer com o "carimbo" do seu SDK
    private val tracer = openTelemetry.getTracer(instrumentationScopeName)

    override suspend fun <T> withSpan(spanName: String, block: suspend () -> T): T {
        // 1. Cria e inicia o Span
        val span = tracer.spanBuilder(spanName).startSpan()

        return try {
            // 2. O TRUQUE MÁGICO: asContextElement()
            // Isso garante que qualquer coroutine filha dentro do bloco conheça este Span
            withContext(span.asContextElement()) {
                block() // Executa a pipeline ou o request
            }
        } catch (e: Throwable) {
            // 3. Se der erro, anota a Exception no Trace e pinta a barra de "Vermelho" no dashboard
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.message ?: "Erro desconhecido")
            throw e
        } finally {
            // 4. Finaliza o cronômetro do Span
            span.end()
        }
    }
}