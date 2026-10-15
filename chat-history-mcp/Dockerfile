# Build context is the repository root: docker build -f chat-history-mcp/Dockerfile .
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY conference-info-mcp/pom.xml conference-info-mcp/
COPY rag-search-mcp/pom.xml rag-search-mcp/
COPY chat-history-mcp/pom.xml chat-history-mcp/
COPY web-search-mcp/pom.xml web-search-mcp/
COPY assistant-api/pom.xml assistant-api/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -pl chat-history-mcp -am dependency:go-offline
COPY chat-history-mcp/src/ chat-history-mcp/src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -pl chat-history-mcp -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home --uid 1001 spring
USER spring
COPY --from=build /workspace/chat-history-mcp/target/chat-history-mcp-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
