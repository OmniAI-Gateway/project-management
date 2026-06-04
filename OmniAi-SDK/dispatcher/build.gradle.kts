plugins {
	kotlin("jvm")
}

description = "SDK services"
group = "org.omniai.sdk.services"


dependencies {
	implementation(project(":core"))
	implementation(project(":pipeline-engine"))
	implementation(project(":interceptors"))
}

kotlin {
	jvmToolchain(22)
}

