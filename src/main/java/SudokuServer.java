
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
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

class Handler implements HttpHandler {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public void handle(HttpExchange t) throws IOException {
        String response = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Sudoku Solver</title>
                <style>
                    body {
                        font-family: "Poppins", Arial, sans-serif;
                        background: #f2f5f9;
                        margin: 0;
                        padding: 0;
                    }

                    h2 {
                        text-align: center;
                        margin-top: 30px;
                        font-size: 28px;
                        color: #333;
                    }

                    .container {
                        width: 450px;
                        margin: 20px auto;
                        background: #ffffff;
                        padding: 25px;
                        border-radius: 18px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.08);
                    }

                    .difficulty-buttons {
                        display: flex;
                        justify-content: center;
                        gap: 8px;
                        margin-bottom: 20px;
                        flex-wrap: wrap;
                    }

                    .difficulty-btn {
                        padding: 8px 12px;
                        font-size: 12px;
                        border: none;
                        border-radius: 6px;
                        cursor: pointer;
                        transition: 0.2s ease;
                        font-weight: 500;
                    }

                    .very-easy { background: #4CAF50; color: white; }
                    .easy { background: #8BC34A; color: white; }
                    .medium { background: #FFC107; color: black; }
                    .hard { background: #FF9800; color: white; }
                    .very-hard { background: #F44336; color: white; }

                    .difficulty-btn:hover {
                        opacity: 0.9;
                        transform: translateY(-2px);
                    }

                    table {
                        border-collapse: collapse;
                        margin: 0 auto 20px;
                    }

                    td {
                        width: 45px;
                        height: 45px;
                        text-align: center;
                        border: 1px solid #d0d4d9;
                        position: relative;
                    }

                    input {
                        width: 100%;
                        height: 100%;
                        border: none;
                        text-align: center;
                        font-size: 20px;
                        background: transparent;
                        outline: none;
                        color: #2c3e50;
                        font-weight: 500;
                    }

                    input:focus {
                        background: #e9f3ff;
                        border-radius: 6px;
                    }

                    .thick-right { border-right: 2px solid #000; }
                    .thick-bottom { border-bottom: 2px solid #000; }

                    .buttons {
                        display: flex;
                        justify-content: center;
                        gap: 12px;
                        margin-top: 10px;
                        flex-wrap: wrap;
                        max-width: 450px;
                        margin-left: auto;
                        margin-right: auto;
                    }

                    .buttons button {
                        padding: 10px 16px;
                        font-size: 15px;
                        border: none;
                        border-radius: 8px;
                        cursor: pointer;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.15);
                        transition: 0.2s ease;
                        font-weight: 500;
                        width: calc(33.333% - 12px); /* 3 buttons per row accounting for gap */
                        min-width: 120px;
                    }

                    button {
                        padding: 10px 16px;
                        font-size: 15px;
                        border: none;
                        border-radius: 8px;
                        cursor: pointer;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.15);
                        transition: 0.2s ease;
                        font-weight: 500;
                    }

                    #btn-backtrack {
                        background: #4e73df;
                        color: white;
                    }
                    #btn-backtrack:hover { background: #3c5dc6; }

                    #btn-dlx {
                        background: #1cc88a;
                        color: white;
                    }
                    #btn-dlx:hover { background: #16a472; }

                    #btn-clear {
                        background: #6c757d;
                        color: white;
                    }
                    #btn-clear:hover { background: #5a6268; }

                    button:active {
                        transform: scale(0.97);
                    }

                    #solve-time, #puzzle-info {
                        text-align: center;
                        margin-top: 15px;
                        font-size: 16px;
                        font-weight: 500;
                    }

                    .loading {
                        opacity: 0.6;
                        pointer-events: none;
                    }
                </style>
            </head>
            <body>
                <h2>Sudoku Solver</h2>
                <div class="container">
                    <div class="difficulty-buttons">
                        <button class="difficulty-btn very-easy" onclick="loadPuzzle('easy')">Very Easy</button>
                        <button class="difficulty-btn easy" onclick="loadPuzzle('very-easy')">Easy</button>
                        <button class="difficulty-btn medium" onclick="loadPuzzle('medium')">Medium</button>
                        <button class="difficulty-btn hard" onclick="loadPuzzle('very-hard')">Hard</button>
                        <button class="difficulty-btn very-hard" onclick="loadPuzzle('hard')">Very Hard</button>
                    </div>
                    
                    <table id="sudoku-grid">
                        <!-- Grid will be generated by JavaScript -->
                    </table>
                    
                    <div class="buttons">
                        <button id="btn-backtrack" onclick="solve('backtrack')">Solve (Bitmask Backtracking)</button>
                        <button id="btn-dlx" onclick="solve('dlx')">Solve (DLX)</button>
                        <button id="btn-simple" onclick="solve('simple')">Solve (Simple Backtracking)</button>
                        <button id="btn-clear" onclick="clearGrid()">Clear</button>
                    </div>
                    
                    <div id="puzzle-info">Select a difficulty to load a puzzle</div>
                    <div id="solve-time"></div>
                </div>

                <script>
                    // Initialize empty grid
                    function initializeGrid() {
                        const grid = document.getElementById('sudoku-grid');
                        let html = '';
                        for (let i = 0; i < 9; i++) {
                            html += '<tr>';
                            for (let j = 0; j < 9; j++) {
                                const classes = [];
                                if (j === 2 || j === 5) classes.push('thick-right');
                                if (i === 2 || i === 5) classes.push('thick-bottom');
                                html += `<td class="${classes.join(' ')}">
                                            <input type="text" maxlength="1" id="c${i}${j}" 
                                            oninput="this.value=this.value.replace(/[^1-9]/g,'')">
                                        </td>`;
                            }
                            html += '</tr>';
                        }
                        grid.innerHTML = html;
                    }

                    let originalFilled = [];

                    function saveOriginalState() {
                        originalFilled = [];
                        for (let i = 0; i < 9; i++) {
                            originalFilled[i] = [];
                            for (let j = 0; j < 9; j++) {
                                originalFilled[i][j] = document.getElementById(`c${i}${j}`).value !== '';
                            }
                        }
                    }

                    async function loadPuzzle(difficulty) {
                        const infoEl = document.getElementById('puzzle-info');
                        const buttons = document.querySelectorAll('.difficulty-btn');
                        
                        // Show loading state
                        infoEl.textContent = 'Loading puzzle...';
                        buttons.forEach(btn => btn.classList.add('loading'));
                        
                        try {
                            const response = await fetch('/load-puzzle?difficulty=' + difficulty);
                            const data = await response.json();
                            
                            if (data.puzzle) {
                                // Update the grid with new puzzle
                                for (let i = 0; i < 9; i++) {
                                    for (let j = 0; j < 9; j++) {
                                        const value = data.puzzle[i][j];
                                        const cell = document.getElementById(`c${i}${j}`);
                                        cell.value = value === 0 ? '' : value;
                                        cell.style.color = value !== 0 ? '#e74c3c' : '#2c3e50';
                                    }
                                }
                                infoEl.textContent = `Loaded ${difficulty.replace('-', ' ')} puzzle`;
                                infoEl.style.color = '#2c3e50';
                            } else {
                                infoEl.textContent = 'Failed to load puzzle';
                                infoEl.style.color = '#e74c3c';
                            }
                        } catch (error) {
                            console.error('Error loading puzzle:', error);
                            infoEl.textContent = 'Error loading puzzle';
                            infoEl.style.color = '#e74c3c';
                        } finally {
                            buttons.forEach(btn => btn.classList.remove('loading'));
                        }
                    }

                    function solve(method) {
                        saveOriginalState();
                        const puzzle = [];
                        for (let i = 0; i < 9; i++) {
                            puzzle[i] = [];
                            for (let j = 0; j < 9; j++) {
                                const val = document.getElementById(`c${i}${j}`).value;
                                puzzle[i][j] = val ? parseInt(val) : 0;
                            }
                        }

                        fetch('/solve', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ method: method, puzzle: puzzle })
                        })
                        .then(r => r.json())
                        .then(data => {
                            const timeEl = document.getElementById('solve-time');
                            if (data.solution) {
                                for (let i = 0; i < 9; i++) {
                                    for (let j = 0; j < 9; j++) {
                                        const cell = document.getElementById(`c${i}${j}`);
                                        cell.value = data.solution[i][j];
                                        cell.style.color = originalFilled[i][j] ? '#e74c3c' : '#2c3e50';
                                    }
                                }
                                timeEl.innerHTML = `<span style="color:#4e73df">Algorithm:</span> ${method.toUpperCase()} | 
                                                   <span style="color:#1cc88a">Time:</span> ${data.timeMs} ms`;
                                timeEl.style.color = '#2c3e50';
                            } else {
                                timeEl.textContent = 'No solution found!';
                                timeEl.style.color = '#e74c3c';
                            }
                        });
                    }

                    function clearGrid() {
                        for (let i = 0; i < 9; i++) {
                            for (let j = 0; j < 9; j++) {
                                const cell = document.getElementById(`c${i}${j}`);
                                cell.value = '';
                                cell.style.color = '#2c3e50';
                            }
                        }
                        document.getElementById('solve-time').textContent = '';
                        document.getElementById('puzzle-info').textContent = 'Grid cleared';
                    }

                    // Initialize when page loads
                    initializeGrid();
                </script>
            </body>
            </html>
            """;
        
        if (t.getRequestURI().getPath().equals("/")) {
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            sendResponse(t, response);
        } else if (t.getRequestURI().getPath().equals("/solve")) {
            handleSolveRequest(t);
        } else if (t.getRequestURI().getPath().equals("/load-puzzle")) {
            handleLoadPuzzleRequest(t);
        }
    }

    private void handleSolveRequest(HttpExchange t) throws IOException {
        if ("POST".equals(t.getRequestMethod())) {
            String body = new String(t.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                String method = "bitmask";
                if (body.contains("\"method\":\"dlx\"")) {
                    method = "dlx";
                } else if (body.contains("\"method\":\"simple\"")) {
                    method = "simple";
                } 

                int startIdx = body.indexOf("\"puzzle\":") + 9;
                String puzzleStr = body.substring(startIdx).trim();
                if (puzzleStr.endsWith("}")) {
                    puzzleStr = puzzleStr.substring(0, puzzleStr.length() - 1);
                }
                int[][] puzzle = parsePuzzle(puzzleStr);

                long timeNanos;
                if ("dlx".equals(method)) {
                    timeNanos = SudokuDLX.solveWithTime(puzzle);
                } else if ("simple".equals(method)) {
                    timeNanos = SudokuSolverSimple.solveSudokuWithTime(puzzle);
                } else {
                    timeNanos = SudokuSolver.solveSudokuWithTime(puzzle);
                }

                String solJson;
                if (timeNanos >= 0) {
                    solJson = String.format(
                        "{\"solution\":%s,\"timeMs\":%.2f}",
                        Arrays.deepToString(puzzle).replace(" ", ""),
                        timeNanos / 1_000_000.0
                    );
                } else {
                    solJson = "{\"solution\":null,\"timeMs\":0}";
                }

                t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                sendResponse(t, solJson);
            } catch (Exception e) {
                sendResponse(t, "{\"solution\":null,\"timeMs\":0}");
            }
        }
    }

    private void handleLoadPuzzleRequest(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getQuery();
        String difficulty = "easy";
        
        if (query != null && query.startsWith("difficulty=")) {
            difficulty = query.substring(11);
        }

        try {
            int[][] puzzle = fetchPuzzleFromAPI(difficulty);
            String jsonResponse = String.format("{\"puzzle\":%s}", Arrays.deepToString(puzzle).replace(" ", ""));
            
            t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            sendResponse(t, jsonResponse);
        } catch (Exception e) {
            // Fallback to local puzzles if API fails
            int[][] fallbackPuzzle = getFallbackPuzzle(difficulty);
            String jsonResponse = String.format("{\"puzzle\":%s}", Arrays.deepToString(fallbackPuzzle).replace(" ", ""));
            
            t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            sendResponse(t, jsonResponse);
        }
    }

    private int[][] fetchPuzzleFromAPI(String difficulty) throws IOException, InterruptedException {
        // Using sudoku-api.vercel.app API which provides puzzles of different difficulties
        String apiUrl = "https://sudoku-api.vercel.app/api/dosuku?query={newboard(limit:1){grids{value}}}";
        
        // Alternative API with difficulty support
        String difficultyApiUrl = String.format("https://sugoku.onrender.com/board?difficulty=%s", difficulty);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(difficultyApiUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse the response - format: {"board":[[...],[...],...]}
        String responseBody = response.body();
        int startIdx = responseBody.indexOf("\"board\":") + 8;
        int endIdx = responseBody.indexOf("]", startIdx) + 1;
        String boardStr = responseBody.substring(startIdx, endIdx);
        
        return parsePuzzle(boardStr);
    }

    private int[][] getFallbackPuzzle(String difficulty) {
        // Fallback puzzles in case API is unavailable
        switch (difficulty) {
            case "very-easy":
                return new int[][]{
                    {5, 3, 0, 0, 7, 0, 0, 0, 0},
                    {6, 0, 0, 1, 9, 5, 0, 0, 0},
                    {0, 9, 8, 0, 0, 0, 0, 6, 0},
                    {8, 0, 0, 0, 6, 0, 0, 0, 3},
                    {4, 0, 0, 8, 0, 3, 0, 0, 1},
                    {7, 0, 0, 0, 2, 0, 0, 0, 6},
                    {0, 6, 0, 0, 0, 0, 2, 8, 0},
                    {0, 0, 0, 4, 1, 9, 0, 0, 5},
                    {0, 0, 0, 0, 8, 0, 0, 7, 9}
                };
            case "easy":
                return new int[][]{
                    {0, 0, 3, 0, 2, 0, 6, 0, 0},
                    {9, 0, 0, 3, 0, 5, 0, 0, 1},
                    {0, 0, 1, 8, 0, 6, 4, 0, 0},
                    {0, 0, 8, 1, 0, 2, 9, 0, 0},
                    {7, 0, 0, 0, 0, 0, 0, 0, 8},
                    {0, 0, 6, 7, 0, 8, 2, 0, 0},
                    {0, 0, 2, 6, 0, 9, 5, 0, 0},
                    {8, 0, 0, 2, 0, 3, 0, 0, 9},
                    {0, 0, 5, 0, 1, 0, 3, 0, 0}
                };
            case "medium":
                return new int[][]{
                    {0, 2, 0, 6, 0, 8, 0, 0, 0},
                    {5, 8, 0, 0, 0, 9, 7, 0, 0},
                    {0, 0, 0, 0, 4, 0, 0, 0, 0},
                    {3, 7, 0, 0, 0, 0, 5, 0, 0},
                    {6, 0, 0, 0, 0, 0, 0, 0, 4},
                    {0, 0, 8, 0, 0, 0, 0, 1, 3},
                    {0, 0, 0, 0, 2, 0, 0, 0, 0},
                    {0, 0, 9, 8, 0, 0, 0, 3, 6},
                    {0, 0, 0, 3, 0, 6, 0, 9, 0}
                };
            case "hard":
                return new int[][]{
                    {0, 0, 0, 6, 0, 0, 4, 0, 0},
                    {7, 0, 0, 0, 0, 3, 6, 0, 0},
                    {0, 0, 0, 0, 9, 1, 0, 8, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 5, 0, 1, 8, 0, 0, 0, 3},
                    {0, 0, 0, 3, 0, 6, 0, 4, 5},
                    {0, 4, 0, 2, 0, 0, 0, 6, 0},
                    {9, 0, 3, 0, 0, 0, 0, 0, 0},
                    {0, 2, 0, 0, 0, 0, 1, 0, 0}
                };
            case "very-hard":
                return new int[][]{
                    {8, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 3, 6, 0, 0, 0, 0, 0},
                    {0, 7, 0, 0, 9, 0, 2, 0, 0},
                    {0, 5, 0, 0, 0, 7, 0, 0, 0},
                    {0, 0, 0, 0, 4, 5, 7, 0, 0},
                    {0, 0, 0, 1, 0, 0, 0, 3, 0},
                    {0, 0, 1, 0, 0, 0, 0, 6, 8},
                    {0, 0, 8, 5, 0, 0, 0, 1, 0},
                    {0, 9, 0, 0, 0, 0, 4, 0, 0}
                };
            default:
                return new int[9][9]; // Empty grid
        }
    }

    private void sendResponse(HttpExchange t, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(200, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private int[][] parsePuzzle(String rawBody) {
        String body = rawBody.replaceAll("\\s", "").trim();
        if (body.length() < 4 || !body.startsWith("[[") || !body.endsWith("]]")) {
            throw new IllegalArgumentException("Invalid puzzle payload");
        }

        String inner = body.substring(2, body.length() - 2);
        String[] rows = inner.split("\\],\\[");
        if (rows.length != 9) {
            throw new IllegalArgumentException("Puzzle must have 9 rows");
        }

        int[][] puzzle = new int[9][9];
        for (int i = 0; i < 9; i++) {
            String[] nums = rows[i].split(",");
            if (nums.length != 9) {
                throw new IllegalArgumentException("Each row must have 9 values");
            }
            for (int j = 0; j < 9; j++) {
                puzzle[i][j] = Integer.parseInt(nums[j]);
            }
        }
        return puzzle;
    }
}

public class SudokuServer {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new Handler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server running at http://localhost:" + port);
        System.out.println("Available difficulties: very-easy, easy, medium, hard, very-hard");
    }
}