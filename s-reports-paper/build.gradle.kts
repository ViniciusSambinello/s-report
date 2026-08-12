plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
}

dependencies {
    api(project(":s-reports-common"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "s.reports.shade.hikari")
    relocate("com.mysql", "s.reports.shade.mysql")
    relocate("org.yaml.snakeyaml", "s.reports.shade.snakeyaml")
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("thin")
}
