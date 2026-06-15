plugins {
	java
	id("org.springframework.boot") version "2.6.0"
	id("io.spring.dependency-management")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.0")
		mavenBom("io.opentelemetry:opentelemetry-bom:1.20.0")
	}
}

dependencies {
	implementation(fileTree("libs") { include("*.jar") })
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
