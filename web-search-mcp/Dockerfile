# Build context is the repository root: docker build -f web-search-mcp/Dockerfile .
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY conference-info-mcp/pom.xml conference-info-mcp/
COPY rag-search-mcp/pom.xml rag-search-mcp/
COPY chat-history-mcp/pom.xml chat-history-mcp/
COPY web-search-mcp/pom.xml web-search-mcp/
COPY assistant-api/pom.xml assistant-api/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -pl web-search-mcp -am dependency:go-offline
COPY web-search-mcp/src/ web-search-mcp/src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -pl web-search-mcp -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home --uid 1001 spring
USER spring
COPY --from=build /workspace/web-search-mcp/target/web-search-mcp-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
