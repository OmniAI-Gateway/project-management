package org.omniai.sdk.interceptors.auth.utils

expect fun GenerateRsaPublicKeyBase64(
    modulusBase64Url: String,
    exponentBase64Url: String,
): String?
