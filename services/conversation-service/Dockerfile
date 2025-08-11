# Simple Dockerfile - build the project locally first
FROM eclipse-temurin:21-jre-jammy

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -s /bin/false appuser

# Set working directory
WORKDIR /app

# Copy the pre-built JAR (build locally first with: ./gradlew :auth-service:bootJar)
COPY build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# JVM options for containerized environments
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
