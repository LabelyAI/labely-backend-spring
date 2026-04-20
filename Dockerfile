# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

ARG DB_URL
ARG JWT_SECRET
ARG CLOUDFLARE_R2_ACCESS_KEY
ARG CLOUDFLARE_R2_SECRET_KEY
ARG SAM3_BASE_URL=http://localhost:8000
ARG DEFECTX_BASE_URL=http://localhost:8100

ENV DB_URL=${DB_URL}
ENV JWT_SECRET=${JWT_SECRET}
ENV CLOUDFLARE_R2_ACCESS_KEY=${CLOUDFLARE_R2_ACCESS_KEY}
ENV CLOUDFLARE_R2_SECRET_KEY=${CLOUDFLARE_R2_SECRET_KEY}
ENV SAM3_BASE_URL=${SAM3_BASE_URL}
ENV DEFECTX_BASE_URL=${DEFECTX_BASE_URL}

# Run as non-root user for better container security.
RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
USER spring:spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
