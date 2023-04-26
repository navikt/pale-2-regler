import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import java.io.ByteArrayOutputStream

group = "no.nav.syfo"
version = "1.0.0"

val githubUser: String by project
val githubPassword: String by project

val ktorVersion = "2.3.0"
val logbackVersion = "1.4.6"
val logstashEncoderVersion = "7.3"
val prometheusVersion = "0.16.0"
val jacksonVersion = "2.15.0"
val pale2CommonVersion = "1.ad083ef"
val mockkVersion = "1.13.5"
val kotlinVersion = "1.8.21"
val junitJupiterVersion = "5.9.2"
val commonsTextVersion = "1.10.0"
val commonsCodecVersion = "1.15"

plugins {
    kotlin("jvm") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jmailen.kotlinter") version "3.14.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pale-2-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}



dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("commons-codec:commons-codec:$commonsCodecVersion")// override transient version 1.10

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("no.nav.syfo:pale-2-common-models:$pale2CommonVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("io.mockk:mockk:$mockkVersion")

    testImplementation("org.apache.commons:commons-text:$commonsTextVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.BootstrapKt"
    }
    create("printVersion") {
        doLast {
            println(project.version)
        }
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    register<JavaExec>("generateRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.common.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()

            val starterTag = "<!-- RULE_MARKER_START -->"
            val endTag = "<!-- RULE_MARKER_END -->"

            val start = lines.indexOfFirst { it.contains(starterTag) }
            val end = lines.indexOfFirst { it.contains(endTag) }

            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            starterTag,
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            endTag,
                        ) +
                        lines.subList(end + 1, lines.size)
            readme.writeText(newLines.joinToString("\n"))
        }
    }

    "check" {
        dependsOn("formatKotlin")
        dependsOn("generateRuleMermaid")
    }
}
