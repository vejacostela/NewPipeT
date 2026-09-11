/* SPDX-License-Identifier: GPL-3.0-or-later */
plugins {
    `java-library`
    id("com.google.protobuf") version "0.10.0"
}

group = "io.github.vejacostela.newpipet"
version = file("VERSION").readText().trim()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

dependencies {
    implementation(libs.newpipe.nanojson)
    implementation(libs.jsoup)
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.protobuf:protobuf-javalite:4.36.1")
    implementation("org.mozilla:rhino:1.8.1")
    implementation("org.mozilla:rhino-engine:1.8.1")
    testImplementation(libs.junit)
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.36.1" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins { named("java") { option("lite") } }
        }
    }
}

tasks.jar {
    exclude("**/*.proto")
    includeEmptyDirs = false
}
