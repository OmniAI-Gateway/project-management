package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerToolUsage(
    @SerialName("web_search_requests")
    val webSearchRequests: Int,
)
