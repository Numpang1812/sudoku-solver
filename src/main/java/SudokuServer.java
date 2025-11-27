import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public class SudokuServer {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new Handler());
        // Use a thread pool to handle multiple users simultaneously
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server running at http://localhost:" + port);
        System.out.println("Supports 9x9, 16x16, 25x25 with Simple, Bitmask, and DLX algorithms.");
    }
}

class Handler implements HttpHandler {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public void handle(HttpExchange t) throws IOException {
        String path = t.getRequestURI().getPath();
        if (path.equals("/")) {
            handleRoot(t);
        } else if (path.equals("/solve")) {
            handleSolveRequest(t);
        } else if (path.equals("/load-puzzle")) {
            handleLoadPuzzleRequest(t);
        } else {
            t.sendResponseHeaders(404, -1);
        }
    }

    private void handleRoot(HttpExchange t) throws IOException {
        String response = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Sudoku Solver (Multi-Algo)</title>
                <style>
                    body { font-family: "Poppins", Arial, sans-serif; background: #f2f5f9; margin: 0; padding: 0; }
                    h2 { text-align: center; margin-top: 20px; color: #333; }
                    .container {
                        width: fit-content; min-width: 500px; max-width: 95%; margin: 20px auto;
                        background: #ffffff; padding: 25px; border-radius: 18px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.08);
                        display: flex; flex-direction: column; align-items: center;
                    }
                    .controls-top { display: flex; gap: 20px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; justify-content: center; }
                    select { padding: 8px; border-radius: 6px; border: 1px solid #ddd; font-size: 14px; min-width: 100px; }
                    label { font-weight: 500; color: #555; }
                    
                    .difficulty-buttons { display: flex; gap: 8px; margin-bottom: 15px; }
                    .difficulty-btn {
                        padding: 8px 12px; font-size: 12px; border: none; border-radius: 6px; cursor: pointer; color: white; transition: 0.2s;
                    }
                    .difficulty-btn:disabled { opacity: 0.5; cursor: not-allowed; background: #ccc !important; }
                    .very-easy { background: #4CAF50; }
                    .easy { background: #8BC34A; }
                    .medium { background: #FFC107; color: black; }
                    .hard { background: #FF9800; }
                    .very-hard { background: #F44336; }

                    table { border-collapse: collapse; margin: 0 auto 20px; }
                    td { text-align: center; border: 1px solid #d0d4d9; padding: 0; }
                    input {
                        width: 100%; height: 100%; border: none; text-align: center;
                        background: transparent; outline: none; color: #2c3e50; font-weight: 500; padding: 0;
                    }
                    input:focus { background: #e9f3ff; }

                    .thick-right { border-right: 2px solid #000 !important; }
                    .thick-bottom { border-bottom: 2px solid #000 !important; }
                    .thick-left { border-left: 2px solid #000 !important; }
                    .thick-top { border-top: 2px solid #000 !important; }

                    .buttons { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; justify-content: center;}
                    button {
                        padding: 10px 16px; font-size: 14px; border: none; border-radius: 8px;
                        cursor: pointer; color: white; font-weight: 500;
                    }
                    #btn-solve { background: #4e73df; }
                    #btn-solve:hover { background: #3c5dc6; }
                    #btn-clear { background: #6c757d; }
                    #btn-clear:hover { background: #5a6268; }
                    
                    #solve-time, #puzzle-info { text-align: center; margin-top: 15px; min-height: 20px; }
                </style>
            </head>
            <body>
                <h2>Sudoku Solver</h2>
                <div class="container">
                    <div class="controls-top">
                        <label>Size: 
                            <select id="size-select" onchange="changeSize()">
                                <option value="9">9x9</option>
                                <option value="16">16x16</option>
                                <option value="25">25x25</option>
                            </select>
                        </label>
                        <label>Algorithm: 
                            <select id="algo-select">
                                <option value="dlx">Dancing Links (DLX)</option>
                                <option value="bitmask">Bitmask Backtracking</option>
                                <option value="simple">Simple Backtracking</option>
                            </select>
                        </label>
                    </div>

                    <div class="difficulty-buttons" id="diff-container">
                        <button class="difficulty-btn very-easy" onclick="loadPuzzle('easy')">Very Easy</button>
                        <button class="difficulty-btn easy" onclick="loadPuzzle('very-easy')">Easy</button>
                        <button class="difficulty-btn medium" onclick="loadPuzzle('medium')">Medium</button>
                        <button class="difficulty-btn hard" onclick="loadPuzzle('very-hard')">Hard</button>
                        <button class="difficulty-btn very-hard" onclick="loadPuzzle('hard')">Very Hard</button>
                    </div>
                    
                    <div id="puzzle-info" style="margin-bottom: 10px; font-size: 0.9em; color:#666;">Select difficulty to load</div>

                    <table id="sudoku-grid"></table>
                    
                    <div class="buttons">
                        <button id="btn-solve" onclick="solve()">Solve Puzzle</button>
                        <button id="btn-clear" onclick="clearGrid()">Clear</button>
                    </div>
                    
                    <div id="solve-time"></div>
                </div>

                <script>
                    let currentSize = 9;
                    let originalFilled = [];

                    function changeSize() {
                        const sel = document.getElementById('size-select');
                        currentSize = parseInt(sel.value);
                        initializeGrid(currentSize);
                        
                        const diffBtns = document.querySelectorAll('.difficulty-btn');
                        diffBtns.forEach(btn => {
                            btn.disabled = (currentSize !== 9);
                            btn.style.display = (currentSize !== 9) ? 'none' : 'inline-block';
                        });
                        
                        document.getElementById('puzzle-info').textContent = 
                            currentSize === 9 ? "Select difficulty to load" : "External API only supports 9x9. Please enter manually.";
                    }

                    function initializeGrid(size) {
                        const grid = document.getElementById('sudoku-grid');
                        let html = '';
                        const blockSize = Math.sqrt(size);
                        let cellSize = 45; let fontSize = 20;
                        if(size === 16) { cellSize = 30; fontSize = 14; }
                        if(size === 25) { cellSize = 24; fontSize = 11; }

                        for (let i = 0; i < size; i++) {
                            html += '<tr>';
                            for (let j = 0; j < size; j++) {
                                const classes = [];
                                if ((j + 1) % blockSize === 0 && j !== size - 1) classes.push('thick-right');
                                if ((i + 1) % blockSize === 0 && i !== size - 1) classes.push('thick-bottom');
                                if (i === 0) classes.push('thick-top');
                                if (j === 0) classes.push('thick-left');
                                if (j === size - 1) classes.push('thick-right');
                                if (i === size - 1) classes.push('thick-bottom');

                                html += `<td class="${classes.join(' ')}" style="width:${cellSize}px; height:${cellSize}px;">
                                            <input type="text" id="c${i}_${j}" style="font-size:${fontSize}px"
                                                oninput="validateInput(this, ${size})">
                                        </td>`;
                            }
                            html += '</tr>';
                        }
                        grid.innerHTML = html;
                    }

                    function validateInput(el, size) {
                        el.value = el.value.replace(/[^0-9]/g, '');
                        if(el.value !== '') {
                            const val = parseInt(el.value);
                            if(val < 1 || val > size) el.value = '';
                        }
                    }

                    function saveOriginalState() {
                        originalFilled = [];
                        for (let i = 0; i < currentSize; i++) {
                            originalFilled[i] = [];
                            for (let j = 0; j < currentSize; j++) {
                                originalFilled[i][j] = document.getElementById(`c${i}_${j}`).value !== '';
                            }
                        }
                    }

                    async function loadPuzzle(difficulty) {
                        if(currentSize !== 9) return; 
                        const infoEl = document.getElementById('puzzle-info');
                        infoEl.textContent = 'Loading...';
                        try {
                            const response = await fetch('/load-puzzle?difficulty=' + difficulty);
                            const data = await response.json();
                            if (data.puzzle) {
                                clearGrid();
                                for (let i = 0; i < 9; i++) {
                                    for (let j = 0; j < 9; j++) {
                                        const value = data.puzzle[i][j];
                                        const cell = document.getElementById(`c${i}_${j}`);
                                        if(value !== 0) { cell.value = value; }
                                    }
                                }
                                infoEl.textContent = "Loaded!";
                            }
                        } catch (error) { infoEl.textContent = 'Error loading puzzle'; }
                    }

                    function solve() {
                        saveOriginalState();
                        const method = document.getElementById('algo-select').value;
                        const puzzle = [];
                        for (let i = 0; i < currentSize; i++) {
                            puzzle[i] = [];
                            for (let j = 0; j < currentSize; j++) {
                                const val = document.getElementById(`c${i}_${j}`).value;
                                puzzle[i][j] = val ? parseInt(val) : 0;
                            }
                        }

                        const timeEl = document.getElementById('solve-time');
                        timeEl.innerHTML = "Solving with <b>" + method.toUpperCase() + "</b>...";

                        fetch('/solve', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ puzzle: puzzle, method: method })
                        })
                        .then(r => r.json())
                        .then(data => {
                            if (data.solution) {
                                for (let i = 0; i < currentSize; i++) {
                                    for (let j = 0; j < currentSize; j++) {
                                        const cell = document.getElementById(`c${i}_${j}`);
                                        cell.value = data.solution[i][j];
                                        cell.style.color = originalFilled[i][j] ? '#e74c3c' : '#2c3e50';
                                    }
                                }
                                timeEl.innerHTML = `<span style="color:#1cc88a">Algorithm:</span> ${method.toUpperCase()} | <span style="color:#1cc88a">Time:</span> ${data.timeMs} ms`;
                            } else {
                                timeEl.textContent = 'No solution found! (Puzzle might be invalid)';
                                timeEl.style.color = '#e74c3c';
                            }
                        })
                        .catch(e => {
                            timeEl.textContent = 'Server Error (Check console)';
                            timeEl.style.color = '#e74c3c';
                        });
                    }

                    function clearGrid() {
                        for (let i = 0; i < currentSize; i++) {
                            for (let j = 0; j < currentSize; j++) {
                                const cell = document.getElementById(`c${i}_${j}`);
                                cell.value = '';
                                cell.style.color = '#2c3e50';
                            }
                        }
                        document.getElementById('solve-time').textContent = '';
                    }

                    initializeGrid(9);
                </script>
            </body>
            </html>
            """;
        t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        sendResponse(t, response);
    }

    private void handleSolveRequest(HttpExchange t) throws IOException {
        if ("POST".equals(t.getRequestMethod())) {
            String body = new String(t.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                // Parse Method from JSON
                String method = "dlx"; 
                if (body.contains("\"method\":\"bitmask\"")) method = "bitmask";
                if (body.contains("\"method\":\"simple\"")) method = "simple";

                // Robust Parsing: Extract and Parse the Matrix
                int[][] puzzle = parsePuzzleRegex(body);
                
                long timeTaken = -1;
                int size = puzzle.length;

                // Create new instances for thread safety
                if (method.equals("bitmask")) {
                    timeTaken = new SolverBitmask().solveWithTime(puzzle);
                } else if (method.equals("simple")) {
                    timeTaken = SolverSimple.solveWithTime(puzzle);
                } else {
                    timeTaken = new SolverDLX(size).solveWithTime(puzzle);
                }

                String solJson = (timeTaken >= 0) 
                    ? String.format("{\"solution\":%s,\"timeMs\":%.2f}", arrayToJSON(puzzle), timeTaken / 1_000_000.0)
                    : "{\"solution\":null,\"timeMs\":0}";

                t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                sendResponse(t, solJson);
            } catch (Exception e) {
                e.printStackTrace(); // This goes to server console
                // Return valid JSON error to client so it doesn't hang
                sendResponse(t, "{\"solution\":null,\"timeMs\":0}");
            }
        }
    }

    // New Robust Regex Parser
    private int[][] parsePuzzleRegex(String jsonBody) {
        // 1. Extract the array part: Find substring between [[ and ]]
        int start = jsonBody.indexOf("[[");
        int end = jsonBody.lastIndexOf("]]");
        
        if (start == -1 || end == -1) {
            throw new IllegalArgumentException("Invalid JSON: '[[...]]' structure not found.");
        }
        
        String arrayContent = jsonBody.substring(start, end + 2);
        
        // 2. Extract all numbers using Regex. This ignores brackets, whitespace, and commas.
        List<Integer> numbers = new ArrayList<>();
        Matcher m = Pattern.compile("-?\\d+").matcher(arrayContent);
        
        while (m.find()) {
            numbers.add(Integer.parseInt(m.group()));
        }
        
        // 3. Determine grid size
        int totalCells = numbers.size();
        int size = (int) Math.sqrt(totalCells);
        
        if (size * size != totalCells) {
             // Fallback or error if not a perfect square (e.g. malformed input)
             throw new IllegalArgumentException("Total numbers (" + totalCells + ") do not form a square grid.");
        }

        // 4. Fill Grid
        int[][] grid = new int[size][size];
        int idx = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = numbers.get(idx++);
            }
        }
        return grid;
    }

    private void handleLoadPuzzleRequest(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getQuery();
        String difficulty = "easy";
        if (query != null && query.startsWith("difficulty=")) difficulty = query.substring(11);

        try {
            int[][] puzzle = fetchPuzzleFromAPI(difficulty);
            String jsonResponse = String.format("{\"puzzle\":%s}", arrayToJSON(puzzle));
            t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            sendResponse(t, jsonResponse);
        } catch (Exception e) {
            int[][] fallback = new int[9][9];
            fallback[0][0] = 5; 
            String jsonResponse = String.format("{\"puzzle\":%s}", arrayToJSON(fallback));
            t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            sendResponse(t, jsonResponse);
        }
    }

    private int[][] fetchPuzzleFromAPI(String difficulty) throws IOException, InterruptedException {
        String difficultyApiUrl = String.format("https://sugoku.onrender.com/board?difficulty=%s", difficulty);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(difficultyApiUrl)).header("Accept", "application/json").GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parsePuzzleRegex(response.body());
    }

    private void sendResponse(HttpExchange t, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(200, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String arrayToJSON(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < matrix.length; i++) {
            sb.append("[");
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]);
                if (j < matrix[i].length - 1) sb.append(",");
            }
            sb.append("]");
            if (i < matrix.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}

// ================= ALGORITHM 1: SIMPLE BACKTRACKING =================
class SolverSimple {
    static boolean isSafe(int[][] mat, int row, int col, int num, int n, int sqrt) {
        for (int x = 0; x < n; x++) if (mat[row][x] == num) return false;
        for (int x = 0; x < n; x++) if (mat[x][col] == num) return false;
        int startRow = row - (row % sqrt);
        int startCol = col - (col % sqrt);
        for (int i = 0; i < sqrt; i++)
            for (int j = 0; j < sqrt; j++)
                if (mat[i + startRow][j + startCol] == num) return false;
        return true;
    }

    static boolean solveRec(int[][] mat, int row, int col, int n, int sqrt) {
        if (row == n - 1 && col == n) return true;
        if (col == n) { row++; col = 0; }
        if (mat[row][col] != 0) return solveRec(mat, row, col + 1, n, sqrt);
        for (int num = 1; num <= n; num++) {
            if (isSafe(mat, row, col, num, n, sqrt)) {
                mat[row][col] = num;
                if (solveRec(mat, row, col + 1, n, sqrt)) return true;
                mat[row][col] = 0;
            }
        }
        return false;
    }

    static long solveWithTime(int[][] puzzle) {
        int n = puzzle.length;
        int sqrt = (int) Math.sqrt(n);
        int[][] copy = new int[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(puzzle[i], 0, copy[i], 0, n);
        
        long start = System.nanoTime();
        boolean solved = solveRec(copy, 0, 0, n, sqrt);
        long end = System.nanoTime();
        
        if (solved) for (int i = 0; i < n; i++) System.arraycopy(copy[i], 0, puzzle[i], 0, n);
        return solved ? (end - start) : -1;
    }
}

// ================= ALGORITHM 2: BITMASK BACKTRACKING =================
class SolverBitmask {
    private int[] rowMask;
    private int[] colMask;
    private int[] boxMask;
    private int N, SQRT;

    public long solveWithTime(int[][] puzzle) {
        this.N = puzzle.length;
        this.SQRT = (int) Math.sqrt(N);
        this.rowMask = new int[N];
        this.colMask = new int[N];
        this.boxMask = new int[N];

        int[][] copy = new int[N][N];
        for (int i = 0; i < N; i++) System.arraycopy(puzzle[i], 0, copy[i], 0, N);

        if (!initializeMasks(copy)) return -1; // Invalid initial board
        
        long start = System.nanoTime();
        boolean solved = solveRec(copy, 0, 0);
        long end = System.nanoTime();

        if (solved) for (int i = 0; i < N; i++) System.arraycopy(copy[i], 0, puzzle[i], 0, N);
        return solved ? (end - start) : -1;
    }

    private boolean initializeMasks(int[][] grid) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] != 0) {
                    int num = grid[i][j];
                    int bit = 1 << num;
                    int box = (i / SQRT) * SQRT + (j / SQRT);
                    
                    // Check for pre-existing conflicts
                    if ((rowMask[i] & bit) != 0 || (colMask[j] & bit) != 0 || (boxMask[box] & bit) != 0) {
                        return false; 
                    }
                    
                    rowMask[i] |= bit;
                    colMask[j] |= bit;
                    boxMask[box] |= bit;
                }
            }
        }
        return true;
    }

    private boolean solveRec(int[][] mat, int row, int col) {
        if (row == N - 1 && col == N) return true;
        if (col == N) { row++; col = 0; }
        if (mat[row][col] != 0) return solveRec(mat, row, col + 1);

        int box = (row / SQRT) * SQRT + (col / SQRT);
        for (int num = 1; num <= N; num++) {
            int bit = 1 << num;
            if ((rowMask[row] & bit) == 0 && (colMask[col] & bit) == 0 && (boxMask[box] & bit) == 0) {
                mat[row][col] = num;
                rowMask[row] |= bit; colMask[col] |= bit; boxMask[box] |= bit;
                
                if (solveRec(mat, row, col + 1)) return true;
                
                mat[row][col] = 0;
                rowMask[row] &= ~bit; colMask[col] &= ~bit; boxMask[box] &= ~bit;
            }
        }
        return false;
    }
}

// ================= ALGORITHM 3: DANCING LINKS (DLX) =================
class SolverDLX {
    class DLXNode {
        DLXNode L, R, U, D;
        ColumnNode C;
        int rowID;
        DLXNode() { L=R=U=D=this; }
    }
    class ColumnNode extends DLXNode { int size = 0; }

    private int N, SQRT, COLS;
    private DLXNode header;
    private ColumnNode[] columns;
    private boolean[] isColumnCovered; // To track coverage status safely

    public SolverDLX(int size) {
        this.N = size;
        this.SQRT = (int) Math.sqrt(N);
        this.COLS = 4 * N * N;
        this.isColumnCovered = new boolean[COLS];
    }

    public long solveWithTime(int[][] puzzle) {
        buildMatrix();
        long start = System.nanoTime();
        boolean solved = solveInternal(puzzle);
        long end = System.nanoTime();
        return solved ? (end - start) : -1;
    }

    private void buildMatrix() {
        header = new DLXNode();
        columns = new ColumnNode[COLS];
        DLXNode prev = header;
        for (int i = 0; i < COLS; i++) {
            ColumnNode col = new ColumnNode();
            col.C = col; prev.R = col; col.L = prev; prev = col; columns[i] = col;
        }
        prev.R = header; header.L = prev;
        
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                for (int n = 0; n < N; n++)
                    addRow(r, c, n);
    }

    private void addRow(int r, int c, int n) {
        int rowID = r * (N * N) + c * N + n;
        int box = (r / SQRT) * SQRT + (c / SQRT);
        int[] indices = {
            r * N + c,
            N * N + r * N + n,
            2 * N * N + c * N + n,
            3 * N * N + box * N + n
        };
        DLXNode prev = null, first = null;
        for (int idx : indices) {
            DLXNode node = new DLXNode();
            node.rowID = rowID;
            node.C = columns[idx];
            node.U = columns[idx].U; node.D = columns[idx];
            columns[idx].U.D = node; columns[idx].U = node;
            columns[idx].size++;
            if (first == null) first = node;
            if (prev != null) { prev.R = node; node.L = prev; }
            prev = node;
        }
        if(prev != null) { prev.R = first; first.L = prev; }
    }

    private void cover(ColumnNode c) {
        c.R.L = c.L; c.L.R = c.R;
        for (DLXNode i = c.D; i != c; i = i.D)
            for (DLXNode j = i.R; j != i; j = j.R) {
                j.D.U = j.U; j.U.D = j.D; j.C.size--;
            }
    }

    private void uncover(ColumnNode c) {
        for (DLXNode i = c.U; i != c; i = i.U)
            for (DLXNode j = i.L; j != i; j = j.L) {
                j.C.size++; j.D.U = j; j.U.D = j;
            }
        c.R.L = c; c.L.R = c;
    }

    private boolean search(List<Integer> solution) {
        if (header.R == header) return true;
        ColumnNode c = null; int min = Integer.MAX_VALUE;
        for (DLXNode j = header.R; j != header; j = j.R)
            if (((ColumnNode)j).size < min) { min = ((ColumnNode)j).size; c = (ColumnNode)j; }
        
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

    private boolean solveInternal(int[][] grid) {
        List<Integer> solution = new ArrayList<>();
        // Apply pre-filled cells constraints
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) {
                    int n = grid[r][c] - 1;
                    int box = (r/SQRT)*SQRT + (c/SQRT);
                    int[] idxs = {
                        r*N+c, N*N+r*N+n, 2*N*N+c*N+n, 3*N*N+box*N+n
                    };
                    for(int idx : idxs) {
                        // Prevent covering an already covered column (Invalid Puzzle)
                        if (isColumnCovered[idx]) return false;
                        
                        cover(columns[idx]);
                        isColumnCovered[idx] = true;
                    }
                    solution.add(r*(N*N) + c*N + n);
                }
            }
        }
        
        if (!search(solution)) return false;
        
        for (int id : solution) {
            int r = id / (N * N);
            int rem = id % (N * N);
            grid[r][rem / N] = (rem % N) + 1;
        }
        return true;
    }
}