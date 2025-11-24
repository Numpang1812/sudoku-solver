package src.main.java;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

class Handler implements HttpHandler {

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
                        width: 420px;
                        margin: 20px auto;
                        background: #ffffff;
                        padding: 25px;
                        border-radius: 18px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.08);
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

                    button:active {
                        transform: scale(0.97);
                    }

                    #solve-time {
                        text-align: center;
                        margin-top: 15px;
                        font-size: 16px;
                        font-weight: 500;
                    }
                </style>
            </head>
            <body>
                <h2>Sudoku Solver</h2>

                <div class="container">
                    <table id="grid">
                        <tbody></tbody>
                    </table>

                    <div class="buttons">
                        <button id="btn-backtrack" onclick="solve('backtrack')">Solve (Backtracking)</button>
                        <button id="btn-dlx" onclick="solve('dlx')">Solve (DLX)</button>
                    </div>

                    <div id="solve-time"></div>
                </div>

                <script>
                    const grid = document.getElementById('grid');
                    let html = '';
                    const samplePuzzle = [
                        [0, 2, 9, 0, 6, 1, 5, 0, 0],
                        [0, 1, 5, 0, 2, 0, 0, 3, 0],
                        [7, 0, 0, 0, 0, 0, 0, 0, 0],
                        [0, 7, 0, 0, 3, 2, 0, 0, 1],
                        [1, 0, 0, 9, 0, 7, 0, 0, 3],
                        [3, 0, 0, 1, 5, 0, 0, 9, 0],
                        [0, 0, 0, 0, 0, 0, 0, 0, 5],
                        [0, 3, 0, 0, 9, 0, 4, 7, 0],
                        [0, 0, 7, 5, 1, 0, 3, 2, 0]
                    ];

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
                    loadSamplePuzzle();

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
                            } else {
                                timeEl.textContent = 'No solution found!';
                                timeEl.style.color = '#e74c3c';
                            }
                        });
                    }

                    function loadSamplePuzzle() {
                        for (let i = 0; i < 9; i++) {
                            for (let j = 0; j < 9; j++) {
                                const value = samplePuzzle[i][j];
                                const cell = document.getElementById(`c${i}${j}`);
                                cell.value = value === 0 ? '' : value;
                            }
                        }
                    }
                </script>
            </body>
            </html>
            """;
        if (t.getRequestURI().getPath().equals("/")) {
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            sendResponse(t, response);
        } else if (t.getRequestURI().getPath().equals("/solve")) {
            if ("POST".equals(t.getRequestMethod())) {
                String body = new String(t.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                try {
                    // Parse JSON manually (simple format)
                    String method = "backtrack";
                    if (body.contains("\"method\":\"dlx\"")) {
                        method = "dlx";
                    }
                    
                    // Extract puzzle array string
                    int startIdx = body.indexOf("\"puzzle\":") + 9;
                    String puzzleStr = body.substring(startIdx).trim();
                    if (puzzleStr.endsWith("}")) {
                        puzzleStr = puzzleStr.substring(0, puzzleStr.length() - 1);
                    }
                    int[][] puzzle = parsePuzzle(puzzleStr);

                    long timeNanos;
                    if ("dlx".equals(method)) {
                        timeNanos = SudokuDLX.solveWithTime(puzzle);
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
        System.out.println("Server running at http://localhost:8080");
    }
}