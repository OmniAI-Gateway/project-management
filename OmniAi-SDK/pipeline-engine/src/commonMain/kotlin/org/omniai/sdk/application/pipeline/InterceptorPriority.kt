package org.omniai.sdk.application.pipeline

/**
 * Defines the priority of an interceptor. Interceptors with higher priority values are executed first in the pipeline.
 * If an interceptor does not have this annotation, it is assumed to have a default priority of 0.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InterceptorPriority(
    val value: Int,
)
