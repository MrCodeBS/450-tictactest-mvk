FROM eclipse-temurin:25-jdk-alpine

# Install basic tools needed to fetch/unpack Gradle, plus bash (Alpine uses ash by default)
RUN apk add --no-cache bash curl unzip git ca-certificates

# --- Install Gradle ---
ARG GRADLE_VERSION=8.14
RUN curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# --- Create user with UID:GID 1000:1000 ---
RUN addgroup -g 1000 devuser \
    && adduser -D -u 1000 -G devuser -s /bin/bash devuser

USER devuser
WORKDIR /home/devuser/workspace
