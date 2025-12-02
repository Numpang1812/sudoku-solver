In order to run this code, please run the following command in your terminal.

java -cp classes SudokuServer

In the case that it doesn't run, you may remove and recompile the classes 
and run the server again by pasting this command into the terminal

Remove-Item -Recurse -Force .\classes -ErrorAction SilentlyContinue
javac src\main\java\*.java -d classes
java -cp classes SudokuServer