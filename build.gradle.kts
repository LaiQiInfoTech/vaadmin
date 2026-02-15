plugins {
    id("java-library")
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
    id("com.vaadin") version "24.9.10"
}


group = "dev.w0fv1"
version = "0.48.3"


val springBootVersion = "3.5.10"
val vaadinVersion = "24.9.10"
extra["vaadinVersion"] = vaadinVersion
vaadin {
//    productionMode = true
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}
repositories {
    mavenCentral()

    // Opt-in repositories to keep builds reproducible by default.
    val useMavenLocal = providers.gradleProperty("useMavenLocal").orNull == "true"
    if (useMavenLocal) {
        mavenLocal()
    }

    val usePreReleaseRepos = providers.gradleProperty("usePreReleaseRepos").orNull == "true"
    if (usePreReleaseRepos) {
        maven { url = uri("https://maven.vaadin.com/vaadin-prereleases") }
        maven { url = uri("https://repo.spring.io/milestone") }
    }
    maven { url = uri("https://maven.vaadin.com/vaadin-addons") }

    // GitHub Packages Maven repository. Note: GitHub often requires auth even for public Maven packages.
    // If you can resolve without credentials, leave them unset; otherwise provide GITHUB_TOKEN (or gpr.key).
    maven {
        url = uri("https://maven.pkg.github.com/LaiQiInfoTech/fmapper")
        val githubUser = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_USERNAME"))
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
            .orNull
        val githubToken = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
            .orNull
        if (!githubUser.isNullOrBlank() && !githubToken.isNullOrBlank()) {
            credentials {
                username = githubUser
                password = githubToken
            }
        }
    }
}


dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        mavenBom("com.vaadin:vaadin-bom:$vaadinVersion")
    }
}

dependencies {
    // https://mvnrepository.com/artifact/com.vaadin/vaadin-spring-boot-starter
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
// https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jsr310
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.reflections:reflections:0.10.2")
// https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-hibernate6
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate6")
// https://mvnrepository.com/artifact/org.hibernate.validator/hibernate-validator
    implementation("org.hibernate.validator:hibernate-validator")
    // CSV 解析
    implementation("org.apache.commons:commons-csv:1.14.1")

    // Excel (Apache POI) - 读取 .xlsx
    implementation("org.apache.poi:poi-ooxml:5.5.1")
    implementation("dev.w0fv1:fmapper:0.0.5")
    annotationProcessor("dev.w0fv1:fmapper:0.0.5")
    runtimeOnly("org.postgresql:postgresql:42.7.9")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

//    runtimeOnly("com.h2database:h2:2.3.230")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    exclude("dev/w0fv1/vaadmin/test/**") // 忽略单个类
}


//import org.gradle.plugins.signing.Sign

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.named("sourcesJar").get())
            artifact(tasks.named("javadocJar").get())
            pom {
                name.set("Vaadmin")
                description.set("Vaadmin is a back-end management framework based on Vaadin.")
                url.set("https://github.com/LaiQiInfoTech/vaadmin")

                developers {
                    developer {
                        id.set("w0fv1")
                        name.set("w0fv1")
                        email.set("hi@w0fv1.dev")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/LaiQiInfoTech/vaadmin.git")
                    developerConnection.set("scm:git:ssh://github.com/LaiQiInfoTech/vaadmin.git")
                    url.set("https://github.com/LaiQiInfoTech/vaadmin")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/LaiQiInfoTech/vaadmin")
            credentials {
                username = System.getenv("GITHUB_USERNAME") ?: System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
//        maven {
//            name = "local"
//            url = uri("${layout.buildDirectory}/repo")
//        }
    }
}

