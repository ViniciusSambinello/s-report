plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
}

dependencies {
    api(project(":s-reports-common"))
    compileOnly("com.velocitypowered:velocity-api:4.0.0")
    annotationProcessor("com.velocitypowered:velocity-api:4.0.0")
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
