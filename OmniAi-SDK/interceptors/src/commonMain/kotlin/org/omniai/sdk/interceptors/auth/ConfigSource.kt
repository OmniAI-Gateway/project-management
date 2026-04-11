package org.omniai.sdk.interceptors.auth

fun interface ConfigSource {
	fun get(name: String): String?
}

class MapConfigSource(
	private val values: Map<String, String>
) : ConfigSource {
	override fun get(name: String): String? = values[name]
}

