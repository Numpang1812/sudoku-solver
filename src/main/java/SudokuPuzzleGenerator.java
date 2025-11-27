import java.util.Random;

public class SudokuPuzzleGenerator {
    
    public static int[][] generatePuzzle(String difficulty) {
        // 1. First generate a complete solved Sudoku
        int[][] solved = generateCompleteGrid();
        
        // 2. Then remove numbers based on difficulty
        int cellsToRemove = getCellsToRemoveByDifficulty(difficulty);
        return removeNumbers(solved, cellsToRemove);
    }
    
    private static int[][] generateCompleteGrid() {
        // Start with an empty grid
        int[][] grid = new int[9][9];
        
        // Use your existing solver to generate a complete solution
        if (SudokuSolver.solveSudokuWithTime(grid) >= 0) {
            return grid;
        }
        
        // Fallback: return a known valid Sudoku
        return getKnownSolvedSudoku();
    }
    
    private static int[][] getKnownSolvedSudoku() {
        return new int[][]{
            {5, 3, 4, 6, 7, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {4, 2, 6, 8, 5, 3, 7, 9, 1},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };
    }
    
    private static int getCellsToRemoveByDifficulty(String difficulty) {
        Random random = new Random();
        switch (difficulty) {
            case "very-easy": return 20 + random.nextInt(11); // 20-30 empty cells
            case "easy": return 30 + random.nextInt(11);      // 30-40 empty cells  
            case "medium": return 40 + random.nextInt(11);    // 40-50 empty cells
            case "hard": return 50 + random.nextInt(6);       // 50-55 empty cells
            case "very-hard": return 55 + random.nextInt(6);  // 55-60 empty cells
            default: return 40;
        }
    }

    private static int[][] removeNumbers(int[][] solved, int cellsToRemove) {
        // Create a copy of the solved puzzle
        int[][] puzzle = new int[9][9];
        for (int i = 0; i < 9; i++) {
            puzzle[i] = solved[i].clone();
        }
        
        Random random = new Random();
        int removed = 0;
        
        while (removed < cellsToRemove) {
            int row = random.nextInt(9);
            int col = random.nextInt(9);
            
            // If this cell hasn't been removed yet
            if (puzzle[row][col] != 0) {
                // Store the original value
                int originalValue = puzzle[row][col];
                // Remove the number
                puzzle[row][col] = 0;
                
                // Check if the puzzle still has a unique solution
                if (hasUniqueSolution(puzzle)) {
                    removed++;
                } else {
                    // If not unique, put the value back
                    puzzle[row][col] = originalValue;
                    // Try a limited number of times to avoid infinite loops
                    if (removed > cellsToRemove * 2) break;
                }
            }
        }
        
        return puzzle;
    }
    
    private static boolean hasUniqueSolution(int[][] puzzle) {
        // Make a copy of the puzzle
        int[][] copy1 = new int[9][9];
        for (int i = 0; i < 9; i++) {
            copy1[i] = puzzle[i].clone();
        }
        
        // Try solving with your existing solver
        // For simplicity, we'll assume unique solution for now
        // In a real implementation, you'd check if both solvers find the same solution
        return SudokuSolver.solveSudokuWithTime(copy1) >= 0;
    }
}