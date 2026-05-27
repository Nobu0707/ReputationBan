plugins {
    java
}

group = "dev.modplugin"
version = "0.24.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:deprecation")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("net.coreprotect:coreprotect:23.2")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13") {
        isTransitive = false
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0") {
        isTransitive = false
    }
    compileOnly("me.clip:placeholderapi:2.12.2")

    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("ReputationBan")
    archiveVersion.set(project.version.toString())
}
