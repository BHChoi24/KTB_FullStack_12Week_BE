# 1단계: 프로젝트에 포함된 Gradle Wrapper로 Java 21 실행 JAR를 만듭니다.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# 빌드 스크립트를 먼저 복사해 Gradle 배포본과 의존성 레이어를 재사용합니다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# 2단계: 컴파일 도구를 제외한 Java 21 JRE에서 Spring Boot만 실행합니다.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# EC2 호스트 사용자와 UID/GID를 맞추면 bind mount의 H2 및 업로드 파일에 안전하게 쓸 수 있습니다.
ARG APP_UID=1000
ARG APP_GID=1000

# 애플리케이션은 root가 아닌 전용 사용자로 실행합니다.
RUN addgroup -S -g "${APP_GID}" spring && adduser -S -u "${APP_UID}" -G spring spring \
    && mkdir -p /data/h2 /data/uploads /data/recovery \
    && chown -R spring:spring /app /data

COPY --from=builder --chown=spring:spring /workspace/build/libs/Artifact-0.0.1-SNAPSHOT.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
