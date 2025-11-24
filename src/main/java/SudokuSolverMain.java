package src.main.java;
class SudokuSolverMain {

    // Function to check if it is safe to place num at mat[row][col]
    static boolean isSafe(int[][] mat, int row, int col, int num) {
        
        // Check if num exists in the row
        for (int x = 0; x < 9; x++)
            if (mat[row][x] == num)
                return false;

        // Check if num exists in the col
        for (int x = 0; x < 9; x++)
            if (mat[x][col] == num)
                return false;

        // Check if num exists in the 3x3 sub-matrix
        int startRow = row - (row % 3), startCol = col - (col % 3);

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (mat[i + startRow][j + startCol] == num)
                    return false;

        return true;
    }

    // Function to solve the Sudoku problem
    static boolean solveSudokuRec(int[][] mat, int row, int col) {
      
        // base case: Reached nth column of the last row
        if (row == 8 && col == 9)
            return true;

        // If last column of the row go to the next row
        if (col == 9) {
            row++;
            col = 0;
        }

        // If cell is already occupied then move forward
        if (mat[row][col] != 0)
            return solveSudokuRec(mat, row, col + 1);

        for (int num = 1; num <= 9; num++) {
          
            // If it is safe to place num at current position
            if (isSafe(mat, row, col, num)) {
                mat[row][col] = num;
                if (solveSudokuRec(mat, row, col + 1))
                    return true;
                mat[row][col] = 0;
            }
        }

        return false;
    }

    static void solveSudoku(int[][] mat) {
        solveSudokuRec(mat, 0, 0);
    }

    // Beautiful Sudoku grid printer
    public static void printSudokuGrid(int[][] grid) {
        System.out.println("+-------+-------+-------+");
        
        for (int i = 0; i < 9; i++) {
            System.out.print("│ ");
            for (int j = 0; j < 9; j++) {
                // Print number or dot for empty cells
                if (grid[i][j] == 0) {
                    System.out.print("• ");
                } else {
                    System.out.print(grid[i][j] + " ");
                }
                
                // Add vertical separators
                if (j == 2 || j == 5) {
                    System.out.print("│ ");
                }
            }
            System.out.println("│");
            
            // Add horizontal separators
            if (i == 2 || i == 5) {
                System.out.println("+-------+-------+-------+");
            }
        }
        
        System.out.println("+-------+-------+-------+");
    }

    // Alternative simpler version
    public static void printSudokuGridSimple(int[][] grid) {
        System.out.println("+-------+-------+-------+");
        
        for (int i = 0; i < 9; i++) {
            System.out.print("| ");
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] == 0) {
                    System.out.print(". ");
                } else {
                    System.out.print(grid[i][j] + " ");
                }
                
                if (j == 2 || j == 5) {
                    System.out.print("| ");
                }
            }
            System.out.println("|");
            
            if (i == 2 || i == 5) {
                System.out.println("+-------+-------+-------+");
            }
        }
        
        System.out.println("+-------+-------+-------+");
    }

    public static void main(String[] args) {
        int[][] mat = {
            {3, 0, 6, 5, 0, 8, 4, 0, 0},
            {5, 2, 0, 0, 0, 0, 0, 0, 0},
            {0, 8, 7, 0, 0, 0, 0, 3, 1},
            {0, 0, 3, 0, 1, 0, 0, 8, 0},
            {9, 0, 0, 8, 6, 3, 0, 0, 5},
            {0, 5, 0, 0, 9, 0, 6, 0, 0},
            {1, 3, 0, 0, 0, 0, 2, 5, 0},
            {0, 0, 0, 0, 0, 0, 0, 7, 4},
            {0, 0, 5, 2, 0, 6, 3, 0, 0}
        };

        System.out.println("Original Sudoku Puzzle:");
        printSudokuGrid(mat);

        solveSudoku(mat);

        // Optional: Also show the simple version
        System.out.println("\nSolved Sudoku Puzzle:");
        printSudokuGridSimple(mat);
    }
}