FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy all project files
COPY . .

# Compile all Java files inside src/main/java/ into classes/
RUN mkdir -p classes && javac $(find src/main/java -name "*.java") -d classes

EXPOSE 10000

# Run the server using the fully-qualified class name
CMD ["java", "-cp", "classes", "SudokuServer"]