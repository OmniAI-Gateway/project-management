package org.omniai.sdk.inbound.openai

import kotlin.js.Date

internal actual fun currentTimeMillis(): Long = Date.now().toLong()

