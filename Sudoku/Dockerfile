# Use official OpenJDK 17 slim image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy all files
COPY . .

# Compile Java files
RUN mkdir -p classes && javac -d classes src/main/java/*.java

# Expose port (Render uses PORT env var, but Dockerfile can declare for clarity)
EXPOSE 10000

# Run the server
CMD ["java", "-cp", "classes", "src.main.java.SudokuServer"]
