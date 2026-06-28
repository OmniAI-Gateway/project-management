package org.omniai.sdk.interceptors.auth.utils

import java.net.URLEncoder

actual fun urlEncode(text: String): String = URLEncoder.encode(text, "UTF-8")
