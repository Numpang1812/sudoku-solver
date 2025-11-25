
import java.util.*;

public class SudokuDLX {
    static final int N = 9;
    static final int COLS = N * N * 4;

    static class DLXNode {
        DLXNode L, R, U, D;
        ColumnNode C;
        int rowID;
        DLXNode() { L = R = U = D = this; }
    }

    static class ColumnNode extends DLXNode {
        int size = 0;
    }

    DLXNode header;
    ColumnNode[] columns;

    public SudokuDLX() {
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

    boolean search(int k, List<Integer> solution) {
        if (header.R == header) return true;
        ColumnNode c = chooseColumn();
        cover(c);
        for (DLXNode r = c.D; r != c; r = r.D) {
            solution.add(r.rowID);
            for (DLXNode j = r.R; j != r; j = j.R) cover(j.C);
            if (search(k + 1, solution)) return true;
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
        int rowID = r * 81 + c * 9 + n;
        DLXNode prev = null;
        DLXNode first = null;

        int[] colIndices = {
            r * 9 + c,                    // cell constraint
            81 + r * 9 + n,              // row constraint
            162 + c * 9 + n,             // column constraint
            243 + (r/3*3 + c/3) * 9 + n  // box constraint
        };

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
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                for (int n = 0; n < N; n++)
                    addRow(r, c, n);

        List<Integer> initial = new ArrayList<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) {
                    int n = grid[r][c] - 1;
                    int rowID = r * 81 + c * 9 + n;
                    initial.add(rowID);
                    cover(columns[r * 9 + c]);
                    cover(columns[81 + r * 9 + n]);
                    cover(columns[162 + c * 9 + n]);
                    cover(columns[243 + (r/3*3 + c/3) * 9 + n]);
                }
            }
        }

        List<Integer> solution = new ArrayList<>(initial);
        if (!search(initial.size(), solution)) return false;

        for (int rowID : solution) {
            if (initial.contains(rowID)) continue;
            int r = rowID / 81;
            int c = (rowID % 81) / 9;
            int n = rowID % 9;
            grid[r][c] = n + 1;
        }
        return true;
    }

    public static long solveWithTime(int[][] grid) {
        long start = System.nanoTime();
        boolean solved = new SudokuDLX().solve(grid);
        long end = System.nanoTime();
        return solved ? (end - start) : -1;
    }
}