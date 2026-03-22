package org.omniaigateway.domain.common

@JvmInline
value class Provider(val value: String) {
    companion object {
        val OPENAI = Provider("openai")
        val GEMINI = Provider("gemini")
        val ANTHROPIC = Provider("anthropic")
    }
}
