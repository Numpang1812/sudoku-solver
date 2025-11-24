FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY . .

RUN mkdir -p classes && javac $(find Sudoku/src/main/java -name "*.java") -d classes

EXPOSE 10000

CMD ["java", "-cp", "classes", "SudokuServer"]
