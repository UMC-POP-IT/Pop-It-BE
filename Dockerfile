# 1. 빌드 스테이지: 소스를 컴파일해서 실행 가능한 jar를 만든다
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 프로젝트 전체 복사 후, 프로젝트에 포함된 Gradle Wrapper로 빌드
COPY . .

# Sentry 소스 컨텍스트 업로드용 토큰.
RUN --mount=type=secret,id=sentry_auth_token \
	chmod +x gradlew && \
	SENTRY_AUTH_TOKEN=$(cat /run/secrets/sentry_auth_token 2>/dev/null || true) ./gradlew clean bootJar --no-daemon && \
	rm -f build/libs/*-plain.jar

# 2. 실행 스테이지: 빌드 결과 jar만 가볍게 담아 실행한다
FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드 스테이지에서 만든 jar만 복사 (JDK·소스·Gradle 캐시는 안 넘어와서 이미지가 가벼워짐)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]