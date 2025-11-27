public class SudokuSolverSimple {

    // Function to check if it is safe to place num at mat[row][col]
    static boolean isSafe(int[][] mat, int row, int col, int num, int n, int sqrt) {
        
        // Check Row
        for (int x = 0; x < n; x++)
            if (mat[row][x] == num)
                return false;

        // Check Column
        for (int x = 0; x < n; x++)
            if (mat[x][col] == num)
                return false;

        // Check Sub-matrix (Box)
        int startRow = row - (row % sqrt);
        int startCol = col - (col % sqrt);

        for (int i = 0; i < sqrt; i++)
            for (int j = 0; j < sqrt; j++)
                if (mat[i + startRow][j + startCol] == num)
                    return false;

        return true;
    }

    static boolean solveSudokuRec(int[][] mat, int row, int col, int n, int sqrt) {
        // Base case: Reached end of grid
        if (row == n - 1 && col == n)
            return true;

        // Move to next row
        if (col == n) {
            row++;
            col = 0;
        }

        // Skip filled cells
        if (mat[row][col] != 0)
            return solveSudokuRec(mat, row, col + 1, n, sqrt);

        for (int num = 1; num <= n; num++) {
            if (isSafe(mat, row, col, num, n, sqrt)) {
                mat[row][col] = num;
                if (solveSudokuRec(mat, row, col + 1, n, sqrt))
                    return true;
                mat[row][col] = 0; // Backtrack
            }
        }

        return false;
    }

    static long solveSudokuWithTime(int[][] puzzle) {
        int n = puzzle.length;
        int sqrt = (int) Math.sqrt(n);
        
        // Validation for perfect square sizes
        if (sqrt * sqrt != n) return -1;

        int[][] puzzleCopy = new int[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(puzzle[i], 0, puzzleCopy[i], 0, n);
        }
        
        long startTime = System.nanoTime();
        boolean solved = solveSudokuRec(puzzleCopy, 0, 0, n, sqrt);
        long endTime = System.nanoTime();
        
        if (solved) {
            for (int i = 0; i < n; i++) {
                System.arraycopy(puzzleCopy[i], 0, puzzle[i], 0, n);
            }
            return endTime - startTime;
        } else {
            return -1;
        }
    }
}