FROM openjdk:17-jdk

WORKDIR /app

COPY . .

RUN mkdir -p classes && javac -d classes src/main/java/*.java

EXPOSE 10000

CMD ["java", "-cp", "classes", "src.main.java.SudokuServer"]
