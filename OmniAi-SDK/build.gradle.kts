plugins {
    kotlin("multiplatform") version "2.3.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "org.omniai.sdk"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}


kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(project(":pipeline-engine"))
            api(project(":interceptors"))
            api(project(":inbound:openai"))
            api(project(":inbound:anthropic"))
            api(project(":inbound:gemini"))
            api(project(":contracts:openai"))
            api(project(":contracts:anthropic"))
            api(project(":contracts:gemini"))
            api(project(":http-client"))
            api(project(":outbound:openai"))
            api(project(":outbound:anthropic"))
            api(project(":outbound:gemini"))
        }

        jvmMain.dependencies {
            api(project(":dispatcher"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val tasksToAggregate = listOf("clean", "check", "assemble", "build")

tasksToAggregate.forEach { taskName ->
    tasks.named(taskName) {
        subprojects.forEach { sub ->
            dependsOn(sub.tasks.matching { it.name == taskName })
        }
    }
}
