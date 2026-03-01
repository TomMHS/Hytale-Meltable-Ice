import org.gradle.api.GradleException
import org.gradle.jvm.tasks.Jar
import java.nio.file.Paths

plugins {
    id("java")
}

group = "de.tommhs"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

fun defaultHytaleServerJar(): String {
    // Default Windows path for the launcher "latest" server jar.
    val home = System.getProperty("user.home")
    return Paths.get(
        home,
        "AppData",
        "Roaming",
        "Hytale",
        "install",
        "release",
        "package",
        "game",
        "latest",
        "Server",
        "HytaleServer.jar"
    ).toString()
}

fun resolveHytaleServerJarPath(): String {
    val jarPath = (project.findProperty("hytaleServerJar") as String?)?.trim().takeUnless { it.isNullOrEmpty() }
    if (jarPath != null) {
        return jarPath
    }

    val jarDir = (project.findProperty("hytaleServerJarDir") as String?)?.trim().takeUnless { it.isNullOrEmpty() }
    if (jarDir != null) {
        return Paths.get(jarDir, "HytaleServer.jar").toString()
    }

    return defaultHytaleServerJar()
}

val hytaleServerJarPath = resolveHytaleServerJarPath()
val hytaleServerJarFile = file(hytaleServerJarPath)

if (!hytaleServerJarFile.exists()) {
    throw GradleException(
        "HytaleServer.jar not found at: $hytaleServerJarPath\n" +
                "Set -PhytaleServerJar=... (full path) or -PhytaleServerJarDir=... (directory) " +
                "or define hytaleServerJar/hytaleServerJarDir in a local gradle.properties."
    )
}

dependencies {
    compileOnly(files(hytaleServerJarFile))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.named<Jar>("jar") {
    archiveFileName.set("Tommhs_Meltable_Ice.jar")
}

val deployToServer = tasks.register<Copy>("deployToServer") {
    dependsOn(tasks.named("jar"))

    val modsDir = (project.findProperty("hytaleModsDir") as String?)?.trim()
        ?: throw GradleException(
            "Missing property 'hytaleModsDir'. Set it via -PhytaleModsDir=... or in local gradle.properties."
        )

    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(file(modsDir))
}

// Optional auto-deploy: only if the property is set.
tasks.named("build") {
    doLast {
        val modsDir = (project.findProperty("hytaleModsDir") as String?)?.trim()
        if (!modsDir.isNullOrEmpty()) {
            tasks.named("deployToServer").get().actions.forEach { it.execute(tasks.named("deployToServer").get()) }
        }
    }
}