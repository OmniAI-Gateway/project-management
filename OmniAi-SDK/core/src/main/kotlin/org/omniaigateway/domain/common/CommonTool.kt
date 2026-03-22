package org.omniaigateway.domain.common

import org.omniaigateway.domain.common.json.JsonObjectMap

data class CommonTool(
    val name: String,
    val description: String,
    val parametersSchema: JsonObjectMap
)

