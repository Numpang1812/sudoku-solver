import java.util.*;

public class SudokuDLX {
    private int N;
    private int SQRT;
    private int COLS;
    private DLXNode header;
    private ColumnNode[] columns;

    static class DLXNode {
        DLXNode L, R, U, D;
        ColumnNode C;
        int rowID; // Encodes (r, c, val)
        DLXNode() { L = R = U = D = this; }
    }

    static class ColumnNode extends DLXNode {
        int size = 0;
    }

    public SudokuDLX(int size) {
        this.N = size;
        this.SQRT = (int) Math.sqrt(N);
        // 4 constraints: Cell defined, Row contains N, Col contains N, Box contains N
        // Total cols = 4 * N * N
        this.COLS = 4 * N * N; 
        buildMatrix();
    }

    void buildMatrix() {
        header = new DLXNode();
        columns = new ColumnNode[COLS];
        DLXNode prev = header;

        for (int i = 0; i < COLS; i++) {
            ColumnNode col = new ColumnNode();
            col.C = col;
            prev.R = col;
            col.L = prev;
            prev = col;
            columns[i] = col;
        }
        prev.R = header;
        header.L = prev;
        
        // We add rows dynamically in solve() rather than pre-building all possibilities
        // to save memory for larger grids, OR we can pre-build. 
        // For standard Sudoku sizes, pre-building is fine.
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                for (int n = 0; n < N; n++)
                    addRow(r, c, n);
    }

    void cover(ColumnNode c) {
        c.R.L = c.L;
        c.L.R = c.R;
        for (DLXNode i = c.D; i != c; i = i.D) {
            for (DLXNode j = i.R; j != i; j = j.R) {
                j.D.U = j.U;
                j.U.D = j.D;
                j.C.size--;
            }
        }
    }

    void uncover(ColumnNode c) {
        for (DLXNode i = c.U; i != c; i = i.U) {
            for (DLXNode j = i.L; j != i; j = j.L) {
                j.C.size++;
                j.D.U = j;
                j.U.D = j;
            }
        }
        c.R.L = c;
        c.L.R = c;
    }

    boolean search(List<Integer> solution) {
        if (header.R == header) return true;
        
        ColumnNode c = chooseColumn();
        cover(c);
        
        for (DLXNode r = c.D; r != c; r = r.D) {
            solution.add(r.rowID);
            for (DLXNode j = r.R; j != r; j = j.R) cover(j.C);
            
            if (search(solution)) return true;
            
            solution.remove(solution.size() - 1);
            for (DLXNode j = r.L; j != r; j = j.L) uncover(j.C);
        }
        uncover(c);
        return false;
    }

    ColumnNode chooseColumn() {
        ColumnNode c = null;
        int minSize = Integer.MAX_VALUE;
        for (DLXNode j = header.R; j != header; j = j.R) {
            ColumnNode col = (ColumnNode) j;
            if (col.size < minSize) {
                minSize = col.size;
                c = col;
            }
        }
        return c;
    }

    void addRow(int r, int c, int n) {
        // rowID formula: r * N^2 + c * N + n
        int rowID = r * (N * N) + c * N + n;
        
        // Constraint mapping:
        // 1. Cell Constraint (0 to N^2 - 1): Cell (r,c) has a value
        int idx1 = r * N + c;
        
        // 2. Row Constraint (N^2 to 2*N^2 - 1): Row r has value n
        int idx2 = (N * N) + (r * N + n);
        
        // 3. Col Constraint (2*N^2 to 3*N^2 - 1): Col c has value n
        int idx3 = (2 * N * N) + (c * N + n);
        
        // 4. Box Constraint (3*N^2 to 4*N^2 - 1): Box b has value n
        int boxRow = r / SQRT;
        int boxCol = c / SQRT;
        int boxIdx = boxRow * SQRT + boxCol;
        int idx4 = (3 * N * N) + (boxIdx * N + n);

        int[] colIndices = {idx1, idx2, idx3, idx4};
        
        DLXNode prev = null;
        DLXNode first = null;

        for (int idx : colIndices) {
            DLXNode node = new DLXNode();
            node.rowID = rowID;
            addNodeToColumn(idx, node);
            if (first == null) first = node;
            if (prev != null) {
                prev.R = node;
                node.L = prev;
            }
            prev = node;
        }
        prev.R = first;
        first.L = prev;
    }

    void addNodeToColumn(int colIdx, DLXNode node) {
        ColumnNode col = columns[colIdx];
        node.C = col;
        node.U = col.U;
        node.D = col;
        col.U.D = node;
        col.U = node;
        col.size++;
    }

    public boolean solve(int[][] grid) {
        List<Integer> initial = new ArrayList<>();
        
        // Pre-process grid
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) {
                    int n = grid[r][c] - 1; // 0-indexed value
                    
                    // We need to cover the columns corresponding to this existing number
                    // Formula must match addRow logic
                    int idx1 = r * N + c;
                    int idx2 = (N * N) + (r * N + n);
                    int idx3 = (2 * N * N) + (c * N + n);
                    int boxRow = r / SQRT;
                    int boxCol = c / SQRT;
                    int boxIdx = boxRow * SQRT + boxCol;
                    int idx4 = (3 * N * N) + (boxIdx * N + n);
                    
                    // Note: In DLX, if the grid is pre-filled, we remove those columns
                    // from the matrix entirely so the algorithm doesn't try to fill them.
                    
                    // Check if valid before covering (simple conflict check)
                    if(columns[idx1].C == columns[idx1] || // already covered?
                       columns[idx2].C == columns[idx2] ||
                       columns[idx3].C == columns[idx3] ||
                       columns[idx4].C == columns[idx4]) {
                       // Logic error or invalid board state
                       return false; 
                    }

                    cover(columns[idx1]);
                    cover(columns[idx2]);
                    cover(columns[idx3]);
                    cover(columns[idx4]);
                    
                    // Add to solution list just in case, though usually we just fill grid at end
                    initial.add(r * (N * N) + c * N + n); 
                }
            }
        }

        List<Integer> solution = new ArrayList<>(initial);
        if (!search(solution)) return false;

        for (int rowID : solution) {
            // Decode RowID: r * N^2 + c * N + n
            int r = rowID / (N * N);
            int remainder = rowID % (N * N);
            int c = remainder / N;
            int n = remainder % N;
            grid[r][c] = n + 1;
        }
        return true;
    }

    public static long solveWithTime(int[][] grid) {
        long start = System.nanoTime();
        // Initialize DLX with specific grid size
        SudokuDLX solver = new SudokuDLX(grid.length);
        boolean solved = solver.solve(grid);
        long end = System.nanoTime();
        return solved ? (end - start) : -1;
    }
}