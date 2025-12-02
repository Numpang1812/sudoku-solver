In order to run this code, please run the following command in your terminal.

java -cp classes SudokuServer

In the case that it doesn't run, you may remove and recompile the classes 
and run the server again by pasting this command into the terminal

Remove-Item -Recurse -Force .\classes -ErrorAction SilentlyContinue
javac src\main\java\*.java -d classes
java -cp classes SudokuServer

The source code is in my GitHub repository with a link down below:

https://github.com/Numpang1812/sudoku-solver

The deployed website on Render is in the link down below:

https://sudoku-solver-f6gk.onrender.com

Thank you for using Sudoku Solver!!