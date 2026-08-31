
# Build Stage
FROM amazoncorretto:21-alpine AS builder

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon


# Runtime Stage
FROM amazoncorretto:21-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# 메타데이터 8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]