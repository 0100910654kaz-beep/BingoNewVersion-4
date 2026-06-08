<%@ page language=\"java\" contentType=\"text/html; charset=UTF-8\" pageEncoding=\"UTF-8\"%>\n<%@ page import=\"servlet.BingoGame\" %>\n<%@ page import=\"servlet.PlayerResult\" %>\n<%@ page import=\"java.util.List\" %>\n<%@ page import=\"java.util.ArrayList\" %>\n<%@ page import=\"java.util.Collections\" %>\n<%\n    BingoGame game = (BingoGame) request.getAttribute(\"game\");\n    String error = (String) request.getAttribute(\"error\");\n    String gameId = (game != null) ? game.getGameId() : \"\";\n    \n    String playerName = (String) request.getAttribute(\"confirmedPlayerName\");\n    if (playerName == null) {\n        playerName = (String) session.getAttribute(\"myConfirmedName\");\n    }\n    if (playerName == null) { playerName = \"\"; }\n\n    // 🔄【新機能】司会者のリセットを検知してカードを自動配り直しする安全ルール\n    if (game != null && game.getDrawnNumbers().isEmpty()) {\n        // サーバー側がリセット（数字が0個）されたのに、ブラウザ側が古いカードを記憶していたらリフレッシュ\n        session.removeAttribute(\"card\");\n    }\n\n    List<List<String>> bingoCard = (List<List<String>>) session.getAttribute(\"card\");\n\n    // 🎲 もしリセット等でカードが消えていたら、今の名前のまま自動で新しいカードを生成する\n    if (bingoCard == null && game != null && !playerName.isEmpty()) {\n        List<List<Integer>> columns = new ArrayList<>();\n        for (int i = 0; i < 5; i++) {\n            List<Integer> pool = new ArrayList<>();\n            for (int j = 1; j <= 15; j++) { pool.add(i * 15 + j); }\n            Collections.shuffle(pool);\n            columns.add(pool.subList(0, 5));\n        }\n        bingoCard = new ArrayList<>();\n        for (int r = 0; r < 5; r++) {\n            List<String> row = new ArrayList<>();\n            for (int c = 0; c < 5; c++) {\n                if (r == 2 && c == 2) { row.add(\"0\"); }\n                else { row.add(String.valueOf(columns.get(c).get(r))); }\n            }\n            bingoCard.add(row);\n        }\n        session.setAttribute(\"card\", bingoCard);\n        game.setPlayerCard(playerName, bingoCard); // サーバーへ新しいカードを再登録\n    }\n\n    List<Integer> reverseDrawnNumbers = new ArrayList<>();\n    int ballCount = 0;\n    if (game != null) {\n        reverseDrawnNumbers.addAll(game.getDrawnNumbers());\n        ballCount = reverseDrawnNumbers.size();\n        Collections.reverse(reverseDrawnNumbers);\n    }\n\n    boolean myBingo = false;\n    if (game != null && !playerName.isEmpty()) {\n        for (PlayerResult p : game.getBingoPlayers()) {\n            if (p.getPlayerName().equals(playerName)) {\n                myBingo = true;\n                break;\n            }\n        }\n    }\n    \n    boolean myReach = false;\n    if (game != null && !playerName.isEmpty()) {\n        for (PlayerResult p : game.getReachPlayers()) {\n            if (p.getPlayerName().equals(playerName)) {\n                myReach = true;\n                break;\n            }\n        }\n    }\n%>\n<!DOCTYPE html>\n<html>\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>ビンゴ大会 - プレイヤー画面</title>\n    <style>\n        body { font-family: Arial, sans-serif; background-color: #f7f9fa; padding: 10px; text-align: center; margin: 0; }\n        .container { max-width: 450px; margin: 0 auto; background: white; padding: 15px; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.08); }\n        h2 { color: #333; margin-top: 5px; margin-bottom: 10px; font-size: 20px; }\n        .info-box { background: #eef2f3; padding: 8px; border-radius: 6px; margin-bottom: 12px; font-size: 14px; font-weight: bold; }\n        .bingo-table { width: 100%; margin: 10px 0; border-collapse: separate; border-spacing: 6px; }\n        .bingo-cell { width: 18%; aspect-ratio: 1; border: 2px solid #ccc; font-size: 18px; font-weight: bold; text-align: center; vertical-align: middle; background: #fff; border-radius: 8px; color: #333; }\n        .bingo-cell.hit { background: #ffadad; border-color: #ff6b6b; color: #d00000; position: relative; }\n        .bingo-cell.hit::after { content: \"✓\"; position: absolute; top: 2px; right: 4px; font-size: 10px; color: #ff6b6b; }\n        .free-cell { background: #ffd166 !important; border-color: #f5a623 !important; color: #d00000 !important; }\n        .list-box { margin-top: 15px; text-align: left; background: #fff; padding: 10px; border-radius: 8px; border: 1px solid #ddd; }\n        .history-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 5px; margin-top: 8px; }\n        .history-cell { padding: 6px 0; background: #eef2f3; border-radius: 4px; font-size: 12px; font-weight: bold; text-align: center; color: #555; }\n        .history-cell.newest { animation: pulse 1s infinite alternate; font-size: 14px; }\n        @keyframes pulse { from { transform: scale(1); } to { transform: scale(1.1); } }\n        \n        /* 🎉 ビンゴ・リーチお祝いアニメーション */\n        .overlay { position: fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.75); color:white; display:flex; flex-direction:column; justify-content:center; align-items:center; z-index:9999; }\n        .firework { font-size: 80px; animation: bounce 0.6s infinite alternate; }\n        @keyframes bounce { from { transform: scale(1); } to { transform: scale(1.2); } }\n    </style>\n    <% if (game != null && !myBingo) { %>\n        \n        <script>\n            setInterval(function() {\n                window.location.href = \"BingoServlet?userType=player\";\n            }, 5000);\n        </script>\n    <% } %>\n</head>\n<body>\n\n<% if (myBingo) { %>\n    \n    <div class=\"overlay\" onclick=\"window.location.href='BingoServlet?userType=player'\">\n        <div class=\"firework\">🎆🏆</div>\n        <h1 style=\"font-size:36px; color:#ffd166; margin:10px 0;\">BINGO !!</h1>\n        <p style=\"font-size:18px; font-weight:bold;\"><%= playerName %> さん、おめでとうございます！</p>\n        <p style=\"font-size:13px; color:#ccc; margin-top:20px;\">画面をタップするとカードに戻ります (5秒ごとに自動更新中)</p>\n    </div>\n    <script>\n        // ビンゴ中も裏で5秒おきに状態をチェックして、リセットされたら自動で花火を閉じる処理\n        setInterval(function() {\n            window.location.href = \"BingoServlet?userType=player\";\n        }, 5000);\n    </script>\n<% } %>\n\n<div class=\"container\">\n    <h2>🎯 ビンゴ大会 🎯</h2>\n\n    <% if (game == null || gameId.isEmpty()) { %>\n        \n        <div class=\"info-box\" style=\"background:#ffe3e3; color:#c0392b;\">\n            <%= (error != null) ? error : \"⏳ 司会者がゲームを開始するのを待っています...\" %>\n        </div>\n        <form action=\"BingoServlet\" method=\"get\" style=\"margin-top:20px;\">\n            <input type=\"hidden\" name=\"action\" value=\"join\">\n            <div style=\"margin-bottom:12px;\">\n                <label style=\"font-weight:bold; display:block; margin-bottom:5px;\">部屋番号 (4桁):</label>\n                <input type=\"text\" name=\"gameId\" style=\"padding:8px; width:60%; border-radius:4px; border:1px solid #ccc; font-size:16px; text-align:center;\" required>\n            </div>\n            <div style=\"margin-bottom:20px;\">\n                <label style=\"font-weight:bold; display:block; margin-bottom:5px;\">あなたの名前 (空欄でゲスト):</label>\n                <input type=\"text\" name=\"playerName\" style=\"padding:8px; width:60%; border-radius:4px; border:1px solid #ccc; font-size:16px; text-align:center;\" placeholder=\"例: たかし\">\n            </div>\n            <button type=\"submit\" style=\"padding:10px 25px; background:#2ec4b6; color:white; border:none; border-radius:6px; font-size:16px; font-weight:bold; cursor:pointer;\">参加する</button>\n        </form>\n    <% } else { %>\n        \n        <div class=\"info-box\">\n            部屋番号: <span style=\"color:#e71d36;\"><%= gameId %></span> &nbsp;|&nbsp; \n            名前: <span style=\"color:#011627;\"><%= playerName %> さん</span>\n            <% if (myReach && !myBingo) { %>\n                <div style=\"color:#ff9f1c; margin-top:4px; font-size:15px; animation: pulse 0.5s infinite alternate;\">🔥 REACH (リーチ中!) 🔥</div>\n            <% } %>\n        </div>\n\n        <div style=\"font-size:13px; color:#666; margin-bottom:5px; text-align:right;\">\n            現在の玉数: <%= ballCount %> / 75 球\n        </div>\n\n        <% if (bingoCard != null) { %>\n            <table class=\"bingo-table\">\n                <% for (int r = 0; r < 5; r++) {\n                    %><tr><% \n                        for (int c = 0; c < 5; c++) { \n                            String num = bingoCard.get(r).get(c);\n                            boolean isHit = game.getDrawnNumbers().contains(Integer.parseInt(num)) || num.equals(\"0\");\n                            if (num.equals(\"0\")) { %>\n                                <td class=\"bingo-cell free-cell hit\">FREE</td>\n                            <% } else { %>\n                                <td class=\"bingo-cell <%= isHit ? \"hit\" : \"\" %>\"><%= num %></td>\n                            <% } \n                        } \n                    %></tr><% \n                } %>\n            </table>\n        <% } %>\n\n        <div class=\"list-box\">\n            <h3 style=\"margin: 5px 0; font-size:14px;\">📊 出た数字一覧（最新が赤）</h3>\n            <div class=\"history-grid\">\n                <% for (int i = 0; i < reverseDrawnNumbers.size(); i++) { \n                    int num = reverseDrawnNumbers.get(i);\n                    if (i == 0) { %>\n                        <div class=\"history-cell newest\" style=\"background:#ff6b6b; color:white;\"><%= num %></div>\n                    <% } else { %>\n                        <div class=\"history-cell\"><%= num %></div>\n                    <% }\n                } %>\n            </div>\n        </div>\n    <% } %>\n</div>\n</body>\n</html>\n```

---

### 💾 2. `BingoServlet.java` の修正コード（全書き換え用）

プレイヤーが自動更新（5秒おき）してきた際、上記の「新しくシャッフルされたカード」を正しく受け取ってサーバー側に上書き記憶させるための連携コードを、**最下部の受け皿部分（216行目付近）**に2行だけ追加しました。こちらも全体の流れや交通整理のロジックは**完全にそのまま維持**しています。

GitHubの `BingoServlet.java` を以下のコードで丸ごと上書きしてください。

```java
package servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/BingoServlet")
public class BingoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String userType = request.getParameter("userType"); 
        ServletContext application = getServletContext();
        HttpSession session = request.getSession();
        
        BingoGame game = (BingoGame) application.getAttribute("game");

        // ⏱️ 1. 定期自動期限チェック
        if (game != null) {
            if (game.isExpired() || game.isPast2HoursFromLastBingo()) {
                application.removeAttribute("game");
                game = null;
            }
        }

        // 🚀 2. 【正しい交通整理】
        String confirmedName = (String) session.getAttribute("myConfirmedName");
        if ((action == null || action.trim().isEmpty()) && !"admin".equals(userType) && confirmedName == null) {
            request.setAttribute("game", null); 
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 🟢 3. 新しくゲーム（部屋）を作る処理
        if ("create".equals(action)) {
            String validDaysStr = request.getParameter("validDays");
            int validDays = 8; 
            if (validDaysStr != null) {
                try {
                    validDays = Integer.parseInt(validDaysStr);
                } catch (NumberFormatException e) {
                    validDays = 8;
                }
            }
            
            int random4Digit = (int)(Math.random() * 9000) + 1000;
            String newGameId = String.valueOf(random4Digit);
            
            game = new BingoGame(newGameId, validDays);
            application.setAttribute("game", game);
            request.setAttribute("game", game);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 🔄 4. リセット処理
        if ("reset".equals(action)) {
            if (game != null) {
                game.clearGameDataOnly();
            }
            request.setAttribute("game", game);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 🎲 5. 数字を引く処理
        if ("draw".equals(action)) {
            if (game != null) {
                game.drawNumber();
            }
            request.setAttribute("game", game);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 👥 6. プレイヤーの参加処理
        if ("join".equals(action)) {
            String inputId = request.getParameter("gameId");
            String playerName = request.getParameter("playerName");
            
            if (game != null && game.getGameId().equals(inputId)) {
                
                if (playerName == null || playerName.trim().isEmpty()) {
                    confirmedName = game.generateAnonymousName();
                } else {
                    confirmedName = playerName.trim();
                }
                
                List<List<String>> card = new ArrayList<>();
                List<List<Integer>> columns = new ArrayList<>();
                
                for (int i = 0; i < 5; i++) {
                    List<Integer> pool = new ArrayList<>();
                    for (int j = 1; j <= 15; j++) { pool.add(i * 15 + j); }
                    Collections.shuffle(pool);
                    columns.add(pool.subList(0, 5));
                }

                for (int r = 0; r < 5; r++) {
                    List<String> row = new ArrayList<>();
                    for (int c = 0; c < 5; c++) {
                        if (r == 2 && c == 2) { row.add("0"); } 
                        else { row.add(String.valueOf(columns.get(c).get(r))); }
                    }
                    card.add(row);
                }
                
                session.setAttribute("card", card);
                session.setAttribute("myConfirmedName", confirmedName);
                game.setPlayerCard(confirmedName, card);
                
                request.setAttribute("game", game);
                request.setAttribute("confirmedPlayerName", confirmedName);
                request.getRequestDispatcher("index.jsp").forward(request, response);
            } else {
                request.setAttribute("error", "⚠️ 部屋番号（ゲームID）が正しくありません。");
                request.getRequestDispatcher("index.jsp").forward(request, response);
            }
            return;
        }

        // 7. その他のアクセス（5秒自動更新の受け皿）
        request.setAttribute("game", game);
        
        if ("admin".equals(userType)) {
            request.getRequestDispatcher("admin.jsp").forward(request, response);
        } else {
            if (confirmedName == null) {
                confirmedName = request.getParameter("playerName");
            }
            
            // 🔄【ここを追加】リセットで新しくなった最新のカードデータを、サーバー側にも確実に同期する
            List<List<String>> currentCard = (List<List<String>>) session.getAttribute("card");
            if (game != null && confirmedName != null && currentCard != null) {
                game.setPlayerCard(confirmedName, currentCard);
            }
            
            request.setAttribute("confirmedPlayerName", confirmedName);
            request.getRequestDispatcher("index.jsp").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}
