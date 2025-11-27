import java.util.Arrays;

public class SudokuSolver {
    private static int[] rowMask;
    private static int[] colMask;
    private static int[] boxMask;
    private static int N; // Grid Size
    private static int SQRT; // Box Size

    public static boolean isSafe(int row, int col, int num) {
        int bit = 1 << num;
        int boxIndex = (row / SQRT) * SQRT + (col / SQRT);
        
        return (rowMask[row] & bit) == 0 && 
               (colMask[col] & bit) == 0 && 
               (boxMask[boxIndex] & bit) == 0;
    }

    public static boolean solveSudokuRec(int[][] mat, int row, int col) {
        if (row == N - 1 && col == N) return true;
        if (col == N) { row++; col = 0; }
        
        if (mat[row][col] != 0) return solveSudokuRec(mat, row, col + 1);
        
        int boxIndex = (row / SQRT) * SQRT + (col / SQRT);
        
        for (int num = 1; num <= N; num++) {
            if (isSafe(row, col, num)) {
                int bit = 1 << num;
                
                mat[row][col] = num;
                rowMask[row] |= bit;
                colMask[col] |= bit;
                boxMask[boxIndex] |= bit;
                
                if (solveSudokuRec(mat, row, col + 1)) return true;
                
                mat[row][col] = 0;
                rowMask[row] &= ~bit;
                colMask[col] &= ~bit;
                boxMask[boxIndex] &= ~bit;
            }
        }
        return false;
    }

    public static long solveSudokuWithTime(int[][] grid) {
        N = grid.length;
        SQRT = (int) Math.sqrt(N);
        if (SQRT * SQRT != N) return -1; // Basic validation

        rowMask = new int[N];
        colMask = new int[N];
        boxMask = new int[N];

        initializeMasks(grid);
        
        long start = System.nanoTime();
        boolean solved = solveSudokuRec(grid, 0, 0);
        long end = System.nanoTime();
        
        return solved ? (end - start) : -1;
    }
    
    private static void initializeMasks(int[][] grid) {
        Arrays.fill(rowMask, 0);
        Arrays.fill(colMask, 0);
        Arrays.fill(boxMask, 0);
        
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] != 0) {
                    int num = grid[i][j];
                    int bit = 1 << num;
                    int boxIndex = (i / SQRT) * SQRT + (j / SQRT);
                    
                    rowMask[i] |= bit;
                    colMask[j] |= bit;
                    boxMask[boxIndex] |= bit;
                }
            }
        }
    }
}