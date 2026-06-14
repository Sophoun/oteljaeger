plugins {
	java
	id("org.springframework.boot") version "2.6.0" apply false
	id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
}

group = "com.sophoun"
version = "0.0.1-SNAPSHOT"

allprojects {
	group = "com.sophoun"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}
}
