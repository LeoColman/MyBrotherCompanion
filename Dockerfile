# Multi-stage build for BrotherQL700Companion

# 1) Build stage: use JDK and Gradle Wrapper to produce a fat (shadow) JAR
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy gradle wrapper and config first for better caching
COPY gradle /app/gradle
COPY gradlew /app/gradlew
COPY gradle.properties /app/gradle.properties
COPY settings.gradle.kts /app/settings.gradle.kts
COPY build.gradle.kts /app/build.gradle.kts
RUN chmod +x /app/gradlew

# Copy source
COPY src /app/src

# Build the shadow jar
RUN ./gradlew --no-daemon clean shadowJar


# 2) Runtime stage: minimal JRE + tools required by print pipeline
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install system tools used by the printing pipeline:
# - imagemagick: provides `convert`
# - cups-client: provides `lp`
# - py3-pip + brother_ql: provides `brother_ql_create`
# - font-dejavu: provides DejaVu-Sans font used by ImageMagick in ProverbPrinter
RUN apk add --no-cache \
    imagemagick \
    cups-client \
    py3-pip \
    ttf-dejavu \
    bash \
  && pip3 install --no-cache-dir brother_ql

# Copy fat jar from the build stage
COPY --from=build /app/build/libs/*-all.jar /app/app-all.jar

EXPOSE 8088

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app-all.jar"]
