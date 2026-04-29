package org.omniai.sdk.auth

expect fun generateRsaPublicKeyBase64(modulusBase64Url: String, exponentBase64Url: String): String?
