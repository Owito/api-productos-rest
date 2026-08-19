# Imagen de la API de Productos.
#
# Dos etapas: la primera compila con el wrapper de Gradle, la segunda solo
# lleva el JRE y el jar. La imagen final no contiene el codigo fuente, ni
# Gradle, ni el compilador de Kotlin.

# ---------- Etapa 1: compilacion ----------
FROM eclipse-temurin:17-jdk AS constructor

WORKDIR /origen

# Primero solo los descriptores de compilacion: mientras no cambien, Docker
# reutiliza la capa de dependencias y las compilaciones siguientes son rapidas.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --quiet dependencies || true

COPY src src

# Las pruebas ya corrieron antes de llegar aqui: la imagen no debe depender de
# una base de datos para poder construirse.
RUN ./gradlew --no-daemon --quiet bootJar -x test \
	&& cp build/libs/*.jar aplicacion.jar

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:17-jre-alpine AS ejecucion

# curl para el chequeo de salud del contenedor.
RUN apk add --no-cache curl \
	&& addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=constructor --chown=app:app /origen/aplicacion.jar aplicacion.jar

USER app

# La plataforma inyecta PORT; 8080 es solo el valor de respaldo local.
ENV PORT=8080
# MaxRAMPercentage en vez de -Xmx: la JVM se ajusta sola al limite de memoria
# del contenedor, que en la capa gratuita es pequeno. SerialGC es el recolector
# correcto para un heap chico y un solo nucleo. TieredStopAtLevel=1 recorta el
# trabajo del compilador JIT: cambia rendimiento pico por un arranque mucho mas
# rapido, que es el intercambio correcto en una instancia de demostracion con
# una decima de vCPU.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
	CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/aplicacion.jar"]
