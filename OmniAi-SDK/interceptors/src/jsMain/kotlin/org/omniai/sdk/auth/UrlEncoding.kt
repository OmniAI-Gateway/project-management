package org.omniai.sdk.auth

external fun encodeURIComponent(uriComponent: String): String

actual fun urlEncode(text: String): String {
    return encodeURIComponent(text)
}

