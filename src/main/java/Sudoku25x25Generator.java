import java.util.Random;

public class Sudoku25x25Generator {
    private static final int SIZE = 25;
    private static final int SUBGRID = 5;
    
    public static int[][] generatePuzzle(String difficulty) {
        int[][] solved = generateCompleteGrid();
        int cellsToRemove = getCellsToRemoveByDifficulty(difficulty);
        return removeNumbers(solved, cellsToRemove);
    }
    
    private static int[][] generateCompleteGrid() {
        int[][] grid = new int[SIZE][SIZE];
        
        // Fill the diagonal subgrids
        fillDiagonalSubgrids(grid);
        
        // Solve the puzzle to get a complete grid
        if (!SudokuSolver.solveSudoku(grid)) {
            throw new RuntimeException("Failed to generate valid 25x25 Sudoku grid");
        }
        
        return grid;
    }
    
    private static void fillDiagonalSubgrids(int[][] grid) {
        // Fill the diagonal subgrids with random numbers
        for (int box = 0; box < SIZE; box += SUBGRID) {
            fillBox(grid, box, box);
        }
    }
    
    private static void fillBox(int[][] grid, int row, int col) {
        int[] nums = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            nums[i] = i + 1;
        }
        
        // Shuffle the numbers
        Random random = new Random();
        for (int i = 0; i < SIZE; i++) {
            int j = random.nextInt(SIZE);
            int temp = nums[i];
            nums[i] = nums[j];
            nums[j] = temp;
        }
        
        // Fill the subgrid with shuffled numbers
        int index = 0;
        for (int i = 0; i < SUBGRID; i++) {
            for (int j = 0; j < SUBGRID; j++) {
                grid[row + i][col + j] = nums[index++];
            }
        }
    }
    
    private static int getCellsToRemoveByDifficulty(String difficulty) {
        Random random = new Random();
        // Adjust numbers for 25x25 (625 total cells)
        switch (difficulty) {
            case "very-easy": return 200 + random.nextInt(101);   // 200-300
            case "easy": return 300 + random.nextInt(101);        // 300-400
            case "medium": return 400 + random.nextInt(101);      // 400-500
            case "hard": return 500 + random.nextInt(76);         // 500-575
            case "very-hard": return 575 + random.nextInt(51);    // 575-625
            default: return 400;
        }
    }
    
    private static int[][] removeNumbers(int[][] solved, int cellsToRemove) {
        // Create a copy of the solved puzzle
        int[][] puzzle = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            puzzle[i] = solved[i].clone();
        }
        
        Random random = new Random();
        int removed = 0;
        
        while (removed < cellsToRemove) {
            int row = random.nextInt(SIZE);
            int col = random.nextInt(SIZE);
            
            // If this cell hasn't been removed yet
            if (puzzle[row][col] != 0) {
                puzzle[row][col] = 0;
                removed++;
            }
        }
        
        return puzzle;
    }
}