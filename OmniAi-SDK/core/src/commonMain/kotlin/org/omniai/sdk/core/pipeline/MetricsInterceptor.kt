package org.omniai.sdk.core.pipeline

import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.commom.key
import kotlin.time.TimeSource
import org.omniai.sdk.domain.responses.CommonUsage
import org.omniai.sdk.domain.responses.ResponseErrored
import org.omniai.sdk.domain.responses.UsageReported

/**
 * Aggregated metrics for a provider/model pair.
 */
data class ProviderModelMetrics(
    val provider: String,
    val model: String,
    val totalRequests: Long,
    val successCount: Long,
    val errorCount: Long,
    val averageLatencyMs: Double,
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long,
    val successRate: Double
)

data class TokenStats(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

val TokenUsageKey = key<TokenStats>("token.usage")
val MetricsSnapshotKey = key<ProviderModelMetrics>("metrics.snapshot")

data class ProviderModelKey(
    val provider: String,
    val model: String
)

/**
 * Small in-memory metrics registry for quick inspection/debugging.
 */
object InMemoryMetricsRegistry {
    private val mutex = Mutex()
    private val countersByKey = mutableMapOf<String, MutableProviderModelMetrics>()

    suspend fun recordSuccess(key: ProviderModelKey, latencyMs: Long, tokens: TokenStats?): ProviderModelMetrics {
        return update(key) { bucket ->
            bucket.totalRequests += 1
            bucket.successCount += 1
            bucket.totalLatencyMs += latencyMs
            tokens?.let {
                bucket.promptTokens += it.promptTokens.toLong()
                bucket.completionTokens += it.completionTokens.toLong()
                bucket.totalTokens += it.totalTokens.toLong()
            }
        }
    }

    suspend fun recordError(key: ProviderModelKey, latencyMs: Long): ProviderModelMetrics {
        return update(key) { bucket ->
            bucket.totalRequests += 1
            bucket.errorCount += 1
            bucket.totalLatencyMs += latencyMs
        }
    }

    private suspend fun update(
        key: ProviderModelKey,
        mutate: (MutableProviderModelMetrics) -> Unit
    ): ProviderModelMetrics = mutex.withLock {
        val bucket = countersByKey.getOrPut(key.asMapKey()) { MutableProviderModelMetrics() }
        mutate(bucket)
        bucket.toSnapshot(key)
    }

    private fun ProviderModelKey.asMapKey(): String = "$provider::$model"
}

class MetricsInterceptor : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        val key = ProviderModelKey(
            provider = context.request.provider.value,
            model = context.request.model
        )
        val startedAt = TimeSource.Monotonic.markNow()

        val result = try {
            chain.proceed(context)
        } catch (t: Throwable) {
            recordError(context, key, startedAt)
            throw t
        }

        return when (result) {
            is PipelineResult.Unary -> handleUnary(context, key, startedAt, result)
            is PipelineResult.Stream -> handleStream(context, key, startedAt, result)
            is PipelineResult.NoResult -> PipelineResult.NoResult
        }
    }

    private suspend fun handleUnary(
        context: GatewayContext,
        key: ProviderModelKey,
        startedAt: TimeSource.Monotonic.ValueTimeMark,
        result: PipelineResult.Unary
    ): PipelineResult.Unary {
        val tokens = result.response.usage?.toTokenStats()
        tokens?.let { context.attributes.put(TokenUsageKey, it) }

        val snapshot = InMemoryMetricsRegistry.recordSuccess(
            key = key,
            latencyMs = startedAt.elapsedNow().inWholeMilliseconds,
            tokens = tokens
        )
        context.attributes.put(MetricsSnapshotKey, snapshot)
        return result
    }

    private fun handleStream(
        context: GatewayContext,
        key: ProviderModelKey,
        startedAt: TimeSource.Monotonic.ValueTimeMark,
        result: PipelineResult.Stream
    ): PipelineResult.Stream {
        var latestTokens: TokenStats? = null
        var terminalMetricRecorded = false

        val trackedFlow = result.eventFlow
            .onEach { event ->
                if (event is UsageReported) {
                    val tokens = event.usage.toTokenStats()
                    latestTokens = tokens
                    context.attributes.put(TokenUsageKey, tokens)
                }

                if (event is ResponseErrored && !terminalMetricRecorded) {
                    terminalMetricRecorded = true
                    recordError(context, key, startedAt)
                }
            }
            .onCompletion { cause ->
                if (terminalMetricRecorded) return@onCompletion

                val snapshot = if (cause == null) {
                    InMemoryMetricsRegistry.recordSuccess(
                        key = key,
                        latencyMs = startedAt.elapsedNow().inWholeMilliseconds,
                        tokens = latestTokens
                    )
                } else {
                    InMemoryMetricsRegistry.recordError(
                        key = key,
                        latencyMs = startedAt.elapsedNow().inWholeMilliseconds
                    )
                }
                context.attributes.put(MetricsSnapshotKey, snapshot)
            }

        return PipelineResult.Stream(trackedFlow)
    }

    private suspend fun recordError(
        context: GatewayContext,
        key: ProviderModelKey,
        startedAt: TimeSource.Monotonic.ValueTimeMark
    ) {
        val snapshot = InMemoryMetricsRegistry.recordError(
            key = key,
            latencyMs = startedAt.elapsedNow().inWholeMilliseconds
        )
        context.attributes.put(MetricsSnapshotKey, snapshot)
    }
}

private fun CommonUsage.toTokenStats(): TokenStats {
    val prompt = inputTokens ?: 0
    val completion = outputTokens ?: 0
    val total = totalTokens ?: (prompt + completion)
    return TokenStats(
        promptTokens = prompt,
        completionTokens = completion,
        totalTokens = total
    )
}

private data class MutableProviderModelMetrics(
    var totalRequests: Long = 0,
    var successCount: Long = 0,
    var errorCount: Long = 0,
    var totalLatencyMs: Long = 0,
    var promptTokens: Long = 0,
    var completionTokens: Long = 0,
    var totalTokens: Long = 0
) {
    fun toSnapshot(key: ProviderModelKey): ProviderModelMetrics {
        val avgLatency = if (totalRequests == 0L) 0.0 else totalLatencyMs.toDouble() / totalRequests.toDouble()
        val successRate = if (totalRequests == 0L) 0.0 else successCount.toDouble() / totalRequests.toDouble()
        return ProviderModelMetrics(
            provider = key.provider,
            model = key.model,
            totalRequests = totalRequests,
            successCount = successCount,
            errorCount = errorCount,
            averageLatencyMs = avgLatency,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            successRate = successRate
        )
    }
}
