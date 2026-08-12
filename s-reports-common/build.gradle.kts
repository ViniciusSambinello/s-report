plugins {
    id("java-library")
}

dependencies {
    api("com.zaxxer:HikariCP:7.1.0")
    api("com.mysql:mysql-connector-j:26.7.0")
    api("org.yaml:snakeyaml:2.6")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:junit-jupiter:2.0.5")
    testImplementation("org.testcontainers:mysql:2.0.5")
}
