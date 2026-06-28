package org.omniai.sdk.binders

interface IncomingContext {
    fun getHeader(key: String): String?

    fun getQueryParam(key: String): String?

    fun getPathParam(key: String): String?

    fun getProperty(key: String): String?

    fun getRaw(
        source: Source,
        key: String,
    ): String? =
        when (source) {
            Source.HEADER -> getHeader(key)
            Source.QUERY -> getQueryParam(key)
            Source.PATH -> getPathParam(key)
            Source.PROPERTY -> getProperty(key)
        }
}
