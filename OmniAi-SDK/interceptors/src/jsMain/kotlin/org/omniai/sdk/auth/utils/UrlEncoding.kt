package org.omniai.sdk.auth.utils

external fun encodeURIComponent(uriComponent: String): String

actual fun urlEncode(text: String): String {
    return encodeURIComponent(text)
}

