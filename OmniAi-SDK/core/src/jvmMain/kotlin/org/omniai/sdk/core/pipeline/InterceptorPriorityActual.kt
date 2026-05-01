package org.omniai.sdk.core.pipeline

actual fun getInterceptorPriority(interceptor: Interceptor): Int {
    val annotation = interceptor::class.java.getAnnotation(InterceptorPriority::class.java)
    return annotation?.value ?: 0
}
