plugins {
	`java-library`
	id("io.spring.dependency-management")
	`maven-publish`
	signing
	id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.sophoun"
version = "0.0.10-SNAPSHOT"

java {
	withSourcesJar()
	withJavadocJar()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web:2.6.0")
	implementation("org.springframework.boot:spring-boot-autoconfigure:2.6.0")
	compileOnly("org.springframework.boot:spring-boot-starter-webflux:2.6.0")
	compileOnly("org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.0")
	compileOnly("org.springframework.kafka:spring-kafka:2.8.0")
	compileOnly("org.apache.kafka:kafka-clients:2.8.0")
	compileOnly("software.amazon.awssdk:s3:2.20.0")

	implementation("io.opentelemetry:opentelemetry-sdk:1.20.0")
	implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.20.0")
	implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:1.20.0-alpha")

	compileOnly("org.springframework.boot:spring-boot-configuration-processor:2.6.0")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:2.6.0")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
	archiveClassifier.set("")
	archiveBaseName.set("oteljaeger-spring-boot-starter")
	archiveVersion.set(project.version.toString())

	mergeServiceFiles()

	exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.named("build") {
	dependsOn("shadowJar")
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifact(tasks.named("shadowJar"))

			pom {
				name.set("oteljaeger-spring-boot-starter")
				description.set("OpenTelemetry + Jaeger tracing starter for Spring Boot")
				url.set("https://github.com/sophoun/oteljaeger")

				licenses {
					license {
						name.set("Apache License, Version 2.0")
						url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}

				developers {
					developer {
						id.set("sophoun")
						name.set("Sophoun NHEUM")
						email.set("sophoun.unix@gmail.com")
					}
				}

				scm {
					connection.set("scm:git:git://github.com/sophoun/oteljaeger.git")
					developerConnection.set("scm:git:ssh://github.com/sophoun/oteljaeger.git")
					url.set("https://github.com/sophoun/oteljaeger")
				}
			}
		}
	}

	repositories {
		mavenLocal()

		maven {
			name = "SelfHost"
			url = uri(findProperty("selfHostRepoUrl") as String? ?: "http://localhost:8081/repository/maven-releases/")
			isAllowInsecureProtocol = true
			credentials {
				username = findProperty("selfHostRepoUsername") as String? ?: ""
				password = findProperty("selfHostRepoPassword") as String? ?: ""
			}
		}

		maven {
			name = "OSSRH"
			url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
			credentials {
				username = findProperty("ossrhUsername") as String? ?: ""
				password = findProperty("ossrhPassword") as String? ?: ""
			}
		}
	}
}

signing {
	isRequired = findProperty("signing.keyId") != null
	sign(publishing.publications["mavenJava"])
}
