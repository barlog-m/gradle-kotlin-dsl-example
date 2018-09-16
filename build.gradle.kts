import com.github.benmanes.gradle.versions.VersionsPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.palantir.gradle.docker.DockerExtension
import com.palantir.gradle.docker.PalantirDockerPlugin
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val versionPluginVer = "0.20.0"
    val shadowPluginVer = "2.0.4"
    val dockerPluginVer = "0.20.1"

    repositories {
        jcenter()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:$shadowPluginVer")
        // gradle dependencyUpdates -Drevision=release
        classpath("com.github.ben-manes:gradle-versions-plugin:$versionPluginVer")
        classpath("gradle.plugin.com.palantir.gradle.docker:gradle-docker:$dockerPluginVer")
    }
}

val kotlinLoggingVer = "1.6.10"
val logbackVer = "1.2.3"
val slf4jVer = "1.7.25"

val junitJupiterVer = "5.3.1"

plugins {
    application
    idea
    kotlin("jvm") version "1.2.70"
}

apply {
    apply<ShadowPlugin>()
    apply<PalantirDockerPlugin>()
    apply<VersionsPlugin>()
}

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("io.github.microutils:kotlin-logging:$kotlinLoggingVer")

    compile("org.slf4j:slf4j-api:$slf4jVer")
    runtime("ch.qos.logback:logback-classic:$logbackVer")

    testCompile(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVer")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVer")
}

application {
    mainClassName = "app.AppKt"
    applicationName = "app"
    version = "1.0-SNAPSHOT"
    group = "gradle.kotlin.dsl.example"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
    targetCompatibility = JavaVersion.VERSION_1_10
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_10)
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.isFork = true
    }

    tasks.withType<Test>().configureEach {
        maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)

        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }

    tasks.getByName<Wrapper>("wrapper") {
        gradleVersion = "4.10.1"
        distributionType = Wrapper.DistributionType.ALL
    }

    val shadowJar = tasks.getByName<ShadowJar>("shadowJar") {
        baseName = "app"
        classifier = ""
    }

    val build = tasks.getByName("build") {
        dependsOn(shadowJar)
    }

    configure<DockerExtension> {
        dependsOn(build)
        name = "${project.group}/${shadowJar.baseName}"
        files(shadowJar.outputs)
        setDockerfile(file("$projectDir/src/main/docker/Dockerfile"))
        buildArgs(mapOf(
            "JAR_FILE" to shadowJar.archiveName,
            "JAVA_OPTS" to "-XX:-TieredCompilation",
            "PORT" to "8080"
        ))
        pull(true)
    }

    task("stage") {
        dependsOn(build, tasks.getByName("clean"))
    }
}
