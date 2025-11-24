FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy all project files
COPY . .

# Compile all Java files inside src/main/java/
RUN mkdir -p classes && javac $(find src/main/java -name "*.java") -d classes

EXPOSE 10000

# Run the server (no package)
CMD ["java", "-cp", "classes", "SudokuServer"]
