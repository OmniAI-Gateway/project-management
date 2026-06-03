package org.omniai.sdk.application.pipeline

actual fun getInterceptorPriority(interceptor: Interceptor): Int {
    // JS does not support runtime annotation reflection.
    // Interceptors maintain their insertion order defined by the DSL.
    return 0
}
