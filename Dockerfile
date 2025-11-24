# Use a verified working OpenJDK 17 image
FROM openjdk:17

WORKDIR /app

COPY . .

RUN mkdir -p classes && javac -d classes src/main/java/*.java

EXPOSE 10000

CMD ["java", "-cp", "classes", "src.main.java.SudokuServer"]
