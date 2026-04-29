package org.omniai.sdk.auth

import java.net.URLEncoder

actual fun urlEncode(text: String): String {
    return URLEncoder.encode(text, "UTF-8")
}

