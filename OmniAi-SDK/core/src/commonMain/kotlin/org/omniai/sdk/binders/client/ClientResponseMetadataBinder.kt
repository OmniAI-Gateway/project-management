package org.omniai.sdk.binders.client

import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.binders.buildMetadataBinder
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.key

fun bindClientResponseMetadata(
    context: IncomingContext,
    headerNames: Set<String>,
): TypedMap {
    val binder =
        buildMetadataBinder {
            headerNames.forEach { headerName ->
                header(headerName) bindTo key<String>("http.header.${headerName.lowercase()}")
            }
            property("statusCode").bindToInt(key<Int>("http.statusCode"))
            property("url") bindTo key<String>("http.url")
        }

    return binder.bind(context)
}
