plugins {
    id("java")
}

group = "net.villagerzock"
version = "1.0-Beta"

val loaderSourceSets = listOf("paper", "bungeecord", "velocity").associateWith { loader ->
    sourceSets.create(loader) {
        java.setSrcDirs(listOf("src/$loader/java"))
        resources.setSrcDirs(listOf("src/$loader/resources"))
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += output + compileClasspath
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    "paperCompileOnly"("io.papermc.paper:paper-api:26.1.2.build.+")
    "bungeecordCompileOnly"("net.md-5:bungeecord-api:1.21-R0.4")
    "velocityCompileOnly"("com.velocitypowered:velocity-api:3.4.0")
    "velocityAnnotationProcessor"("com.velocitypowered:velocity-api:3.4.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ProcessResources>("processPaperResources") {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.named<ProcessResources>("processBungeecordResources") {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

val loaderJarTasks = loaderSourceSets.map { (loader, sourceSet) ->
    tasks.register<Jar>("${loader}Jar") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Assembles the $loader plugin jar."
        archiveBaseName.set(project.name)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set(loader)
        from(sourceSets.main.get().output)
        from(sourceSet.output)
    }
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(loaderJarTasks)
}
