package org.omniai.sdk.interceptors.auth.utils

external fun encodeURIComponent(uriComponent: String): String

actual fun urlEncode(text: String): String = encodeURIComponent(text)
