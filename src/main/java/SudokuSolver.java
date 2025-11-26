import java.util.Arrays;
public class SudokuSolver {
    private static int[] rowMask = new int[9];
    private static int[] colMask = new int[9];
    private static int[] boxMask = new int[9];
    
    // Function to check if it is safe to place num at mat[row][col] using bitmasks
    public static boolean isSafe(int[][] mat, int row, int col, int num) {
        int bit = 1 << num;
        int boxIndex = (row / 3) * 3 + (col / 3);
        
        // Check if num exists in row, column, or box using bitwise AND
        return (rowMask[row] & bit) == 0 && 
               (colMask[col] & bit) == 0 && 
               (boxMask[boxIndex] & bit) == 0;
    }

    public static boolean solveSudokuRec(int[][] mat, int row, int col) {
        if (row == 8 && col == 9) return true;
        if (col == 9) { row++; col = 0; }
        if (mat[row][col] != 0) return solveSudokuRec(mat, row, col + 1);
        
        int boxIndex = (row / 3) * 3 + (col / 3);
        
        for (int num = 1; num <= 9; num++) {
            if (isSafe(mat, row, col, num)) {
                int bit = 1 << num;
                
                // Place number and update masks
                mat[row][col] = num;
                rowMask[row] |= bit;
                colMask[col] |= bit;
                boxMask[boxIndex] |= bit;
                
                if (solveSudokuRec(mat, row, col + 1)) return true;
                
                // Backtrack: remove number and clear masks
                mat[row][col] = 0;
                rowMask[row] &= ~bit;
                colMask[col] &= ~bit;
                boxMask[boxIndex] &= ~bit;
            }
        }
        return false;
    }

    public static long solveSudokuWithTime(int[][] grid) {
        // Initialize bitmasks from the initial grid
        initializeMasks(grid);
        
        long start = System.nanoTime();
        boolean solved = solveSudoku(grid);
        long end = System.nanoTime();
        
        return solved ? (end - start) : -1;
    }
    
    private static void initializeMasks(int[][] grid) {
        // Reset masks
        Arrays.fill(rowMask, 0);
        Arrays.fill(colMask, 0);
        Arrays.fill(boxMask, 0);
        
        // Initialize masks with pre-filled numbers
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] != 0) {
                    int num = grid[i][j];
                    int bit = 1 << num;
                    int boxIndex = (i / 3) * 3 + (j / 3);
                    
                    rowMask[i] |= bit;
                    colMask[j] |= bit;
                    boxMask[boxIndex] |= bit;
                }
            }
        }
    }
    
    public static boolean solveSudoku(int[][] mat) {
        return solveSudokuRec(mat, 0, 0);
    }
}