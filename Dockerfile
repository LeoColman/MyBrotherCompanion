# Multi-stage build for MyBrotherCompanion

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

# Optional override for where to fetch the Symbola font. You can pass
#   --build-arg SYMBOLA_URL=https://your.mirror/Symbola.ttf
# during docker build / compose to use a custom location.
ARG SYMBOLA_URL=""
ENV SYMBOLA_URL=${SYMBOLA_URL}

# Install system tools used by the printing pipeline:
# - imagemagick: provides `convert`
# - cups-client: provides `lp`
# - py3-pip + brother_ql: provides `brother_ql_create`
# - Noto fonts (including emoji) to ensure emojis render via ImageMagick/fontconfig
#   We keep DejaVu as a general fallback as well
RUN set -eux; \
  apk add --no-cache \
    imagemagick \
    cups-client \
    py3-pip \
    wget \
    ca-certificates \
    unzip \
    font-dejavu \
    font-noto \
    font-noto-emoji \
    fontconfig \
    bash; \
  update-ca-certificates; \
  mkdir -p /usr/share/fonts/TTF /tmp/symbola; \
  found_url=""; \
  for url in \
    "$SYMBOLA_URL" \
    "https://github.com/aldur/ttf-symbola/raw/master/Symbola.ttf" \
    "https://github.com/stefanotravelli/ttf-symbola/raw/master/Symbola.ttf" \
    "https://wspr.io/assets/Symbola.ttf" \
    "https://dn-works.com/wp-content/uploads/2020/UFAS-Units/Symbola.zip" \
  ; do \
    if [ -z "$url" ]; then continue; fi; \
    echo "Attempting to download Symbola from: $url"; \
    if wget -q -O /tmp/symbola/payload "$url"; then \
      found_url="$url"; \
      break; \
    fi; \
  done; \
  if [ -z "$found_url" ]; then \
    echo >&2 "WARNING: Could not download Symbola font from any known mirror. Proceeding without Symbola. You can pass --build-arg SYMBOLA_URL=<url> to provide a source."; \
  else \
    if unzip -t /tmp/symbola/payload >/dev/null 2>&1; then \
      unzip -jo /tmp/symbola/payload -d /usr/share/fonts/TTF; \
    else \
      mv /tmp/symbola/payload /usr/share/fonts/TTF/Symbola.ttf; \
    fi; \
  fi; \
  rm -rf /tmp/symbola; \
  fc-cache -f; \
  pip3 install --no-cache-dir brother_ql

# Copy fat jar from the build stage
COPY --from=build /app/build/libs/*-all.jar /app/app-all.jar

EXPOSE 8088

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app-all.jar"]
