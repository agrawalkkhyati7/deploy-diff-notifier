# ---- Stage 1: Build ----
# Use a Maven + JDK 17 image to compile and package the app into a jar.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the files needed to resolve dependencies first (better build caching).
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
# Download dependencies (cached unless pom.xml changes).
RUN mvn -q -B dependency:go-offline

# Now copy the source and build the jar (skip tests here; CI runs them separately).
COPY src/ src/
RUN mvn -q -B clean package -DskipTests

# ---- Stage 2: Run ----
# Use a JRE-only image for the final runtime (smaller than the JDK, multi-arch).
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Create a non-root user to run the app (security best practice).
RUN groupadd -r app && useradd -r -g app app
USER app

# Copy just the built jar from the build stage.
COPY --from=build /app/target/deploy-diff-notifier-0.0.1-SNAPSHOT.jar app.jar

# The app listens on 8080.
EXPOSE 8080

# Start the app.
ENTRYPOINT ["java", "-jar", "app.jar"]
