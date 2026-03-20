package org.omniaigateway.domain.common

data class CommonTool(
    val name: String,
    val description: String,
    val parametersSchema: Map<String, Any?>
)

