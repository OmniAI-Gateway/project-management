package org.omniai.sdk.domain.common

import org.omniai.sdk.domain.common.json.JsonObjectMap

data class CommonTool(
    val name: String,
    val description: String,
    val parametersSchema: JsonObjectMap,
)
