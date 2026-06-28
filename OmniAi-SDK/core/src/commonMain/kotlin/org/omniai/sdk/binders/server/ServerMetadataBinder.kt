package org.omniai.sdk.binders.server

import org.omniai.sdk.binders.ConfigurableMetadataBinder
import org.omniai.sdk.binders.MetadataBinderBuilder
import org.omniai.sdk.binders.buildMetadataBinder

fun buildServerMetadataBinder(block: MetadataBinderBuilder.() -> Unit): ConfigurableMetadataBinder = buildMetadataBinder(block)
