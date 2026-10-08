FROM eclipse-temurin:25-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# BuildKit cache for .m2 repository
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl :${MODULE} -am package -DskipTests \
    && cp services/${MODULE}/target/${MODULE}-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
# curl is used by the compose healthchecks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
