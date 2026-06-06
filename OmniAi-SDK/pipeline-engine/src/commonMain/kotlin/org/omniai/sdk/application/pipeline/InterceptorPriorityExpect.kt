package org.omniai.sdk.application.pipeline

/**
 * Returns the priority of the interceptor.Higher values execute first.
 */
expect fun getInterceptorPriority(interceptor: Interceptor): Int
