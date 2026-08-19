plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	kotlin("plugin.jpa") version "2.2.21"
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "co.edu.poli"
version = "1.0.0"
description = "API REST CRUD de Producto - Arquitectura de Aplicaciones Web, Unidad 2"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Framework backend + capa web
	implementation("org.springframework.boot:spring-boot-starter-web")
	// ORM (Spring Data JPA sobre Hibernate)
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Validacion declarativa de los DTO de entrada
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// Documentacion y cliente HTTP embebido (Swagger UI)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")
	// Segundo adaptador de entrada: interfaz web renderizada en el servidor
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Motores de base de datos: H2 para el perfil local, PostgreSQL para Neon
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
	// La capa de aplicacion usa jakarta.transaction.Transactional en vez de la
	// anotacion de Spring, para no depender del framework. El plugin kotlin-spring
	// no la reconoce, asi que hay que abrirla aqui para que Spring pueda proxearla.
	annotation("jakarta.transaction.Transactional")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
