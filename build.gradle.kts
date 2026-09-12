plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	kotlin("plugin.jpa") version "2.2.21"
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
	// Compila los .proto de src/main/proto y genera los stubs Java del cuarto
	// adaptador de entrada (gRPC). Los generados van a build/generated y no
	// se versionan: el contrato es el .proto.
	id("com.google.protobuf") version "0.9.4"
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

// Spring gRPC fija las versiones de grpc-java y protobuf-java en su BOM. La
// linea 0.12.x es la ultima construida sobre Spring Boot 3.5; la 1.x exige
// Boot 4, que este proyecto no adopta (ver la seccion de trampas conocidas de la guia del repositorio).
val springGrpcVersion = "0.12.0"

dependencyManagement {
	imports {
		mavenBom("org.springframework.grpc:spring-grpc-dependencies:$springGrpcVersion")
	}
}

dependencies {
	// Framework backend + capa web
	implementation("org.springframework.boot:spring-boot-starter-web")
	// ORM (Spring Data JPA sobre Hibernate)
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Validacion declarativa de los DTO de entrada
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// Chequeo de salud para la plataforma de despliegue
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// Generacion del documento OpenAPI a partir de los controladores. Es el
	// artefacto "-api": produce la especificacion en /v3/api-docs y no trae
	// interfaz, porque la que se usa es Scalar.
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.17")
	// Interfaz de documentacion y cliente HTTP embebido que consume ese
	// documento. El paquete trae su propio bundle de JavaScript, asi que la
	// pagina no depende de ninguna CDN externa.
	implementation("com.scalar.maven:scalar-webmvc:0.6.65")
	// Segundo adaptador de entrada: interfaz web renderizada en el servidor
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	// Tercer adaptador de entrada: API GraphQL. El starter trae GraphQL Java
	// (el motor que analiza y ejecuta las consultas), Spring for GraphQL (el
	// puente con el contenedor de Spring y las anotaciones @QueryMapping y
	// @MutationMapping) y el transporte HTTP que publica el endpoint /graphql.
	// La version la fija el BOM de Spring Boot, por eso no se declara aqui.
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	// Cuarto adaptador de entrada: servicio gRPC. El starter trae grpc-java con
	// el servidor Netty, registra como servicios los beans que extienden los
	// stubs generados y publica el puerto de spring.grpc.server.port.
	implementation("org.springframework.grpc:spring-grpc-server-spring-boot-starter")
	// Reflexion del servidor: permite que grpcurl y Postman descubran el
	// servicio y sus operaciones sin tener el .proto a mano. Es el analogo de
	// /graphql/schema y de /v3/api-docs.
	implementation("io.grpc:grpc-services")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Migraciones de esquema. El esquema deja de generarlo el ORM: lo declaran
	// los scripts de db/migration y Hibernate solo lo valida al arrancar.
	implementation("org.flywaydb:flyway-core")
	// Flyway 10 saco el soporte de cada motor del nucleo a su propio modulo, asi
	// que hay que declarar los dos que usa el proyecto.
	runtimeOnly("org.flywaydb:flyway-database-postgresql")

	// Motores de base de datos: H2 para el perfil local, PostgreSQL para Neon
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	// GraphQlTester: ejecuta consultas contra el esquema sin levantar servidor
	testImplementation("org.springframework.graphql:spring-graphql-test")
	// Transporte in-process para las pruebas del adaptador gRPC: el servidor
	// y el cliente hablan en memoria, sin abrir un puerto de red.
	testImplementation("org.springframework.grpc:spring-grpc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Generacion de los stubs gRPC. protoc y el plugin de grpc-java se toman en
// las mismas versiones que importa el BOM de Spring gRPC, para que el codigo
// generado y la libreria en tiempo de ejecucion no se desalineen.
protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:${dependencyManagement.importedProperties["protobuf-java.version"]}"
	}
	plugins {
		create("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:${dependencyManagement.importedProperties["grpc.version"]}"
		}
	}
	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
				create("grpc") {
					// Sin la anotacion @Generated, que exige javax.annotation en el classpath
					option("@generated=omit")
				}
			}
		}
	}
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

// El jar "plain" no se usa: el ejecutable es el que produce bootJar. Apagarlo
// deja un unico artefacto en build/libs y evita ambiguedades al construir la
// imagen de Docker.
tasks.named<Jar>("jar") {
	enabled = false
}
