description = "Inbound web module"

dependencies {
	implementation(platform("io.ktor:ktor-bom:3.2.3"))
	implementation("io.ktor:ktor-server-core")
	implementation("io.ktor:ktor-server-content-negotiation")
	implementation("io.ktor:ktor-serialization-kotlinx-json")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

