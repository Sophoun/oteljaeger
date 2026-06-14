plugins {
	`java-library`
	id("io.spring.dependency-management")
	`maven-publish`
}

group = "com.sophoun"
version = "0.0.1-SNAPSHOT"

java {
	withSourcesJar()
	withJavadocJar()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web:2.6.0")
	implementation("org.springframework.boot:spring-boot-autoconfigure:2.6.0")

	implementation("io.opentelemetry:opentelemetry-sdk:1.20.0")
	implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.20.0")

	compileOnly("org.springframework.boot:spring-boot-configuration-processor:2.6.0")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:2.6.0")
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
			artifactId = "oteljaeger-spring-boot-starter"

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
					url.set("https://github.com/sophoun/oteljaeger")
				}
			}
		}
	}

	repositories {
		mavenLocal()
	}
}
