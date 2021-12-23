import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

group = "no.nav.syfo"
version = "1.0.0"

val githubUser: String by project
val githubPassword: String by project

val ktorVersion = "1.6.7"
val logbackVersion = "1.2.9"
val logstashEncoderVersion = "7.0.1"
val prometheusVersion = "0.14.1"
val jacksonVersion = "2.13.1"
val pale2CommonVersion = "1.a86680d"
val spekVersion = "2.0.17"
val kluentVersion = "1.68"
val mockkVersion = "1.12.1"
val jfairyVersion = "0.6.4"

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.jmailen.kotlinter") version "3.6.0"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pale-2-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}



dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("no.nav.syfo:pale-2-common-models:$pale2CommonVersion")
    implementation("no.nav.syfo:pale-2-common-networking:$pale2CommonVersion")
    implementation("no.nav.syfo:pale-2-common-rest-sts:$pale2CommonVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.devskiller:jfairy:$jfairyVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }

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
            includeEngines("spek2")
        }
        testLogging {
            showStandardStreams = true
        }
    }

    "check" {
        dependsOn("formatKotlin")
    }
}
