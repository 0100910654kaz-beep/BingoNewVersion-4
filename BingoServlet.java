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
        ServletContext application = getServletContext();
        HttpSession session = request.getSession();
        
        BingoGame game = (BingoGame) application.getAttribute("game");

        // 自動期限チェック
        if (game != null) {
            if (game.isExpired() || game.isPast2HoursFromLastBingo()) {
                application.removeAttribute("game");
                game = null;
            }
        }

        // 🚀 新規部屋作成
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
            
            // 簡単な4桁IDを抽選
            String newGameId = "1111"; 
            if (Math.random() < 0.10) {
                String[] easyNumbers = {
                    "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
                    "1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000",
                    "1234", "5678", "7788", "1122", "5566"
                };
                int idx = (int)(Math.random() * easyNumbers.length);
                newGameId = easyNumbers[idx];
            } else {
                int random4Digit = (int)(Math.random() * 9000) + 1000;
                newGameId = String.valueOf(random4Digit);
            }
            
            game = new BingoGame(newGameId, validDays);
            application.setAttribute("game", game);
            request.setAttribute("game", game);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 🔄 リセットボタンの新しい挙動（IDとタイマーは残してお掃除）
        if ("reset".equals(action)) {
            if (game != null) {
                game.clearGameDataOnly();
            }
            request.setAttribute("game", game);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 🎲 次の数字を引く
        if ("draw".equals(action)) {
            if (game != null) {
                game.drawNumber();
            }
            request.setAttribute("game", game);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
            return;
        }

        // 👥 プレイヤーの参加処理
        if ("join".equals(action)) {
            String inputId = request.getParameter("gameId");
            String playerName = request.getParameter("playerName");
            
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "ゲスト";
            } else {
                playerName = playerName.trim();
            }

            if (game != null && game.getGameId().equals(inputId)) {
                String confirmedName = (String) session.getAttribute("myConfirmedName");
                List<List<String>> bingoCard = (List<List<String>>) session.getAttribute("card");

                if (confirmedName == null || !confirmedName.equals(playerName) || bingoCard == null) {
                    confirmedName = game.getGameId() + "_" + playerName; 
                    
                    // 名前の重複防止対策（簡易版）
                    if (game.getWaitNumbers(playerName) != null && !playerName.equals("ゲスト")) {
                        confirmedName = playerName;
                    } else {
                        confirmedName = playerName;
                    }
                    
                    List<List<String>> card = new ArrayList<>();
                    List<List<Integer>> columns = new ArrayList<>();
                    
                    for (int i = 0; i < 5; i++) {
                        List<Integer> pool = new ArrayList<>();
                        for (int j = 1; j <= 15; j++) {
                            pool.add(i * 15 + j);
                        }
                        Collections.shuffle(pool);
                        columns.add(pool.subList(0, 5));
                    }

                    for (int r = 0; r < 5; r++) {
                        List<String> row = new ArrayList<>();
                        for (int c = 0; c < 5; c++) {
                            if (r == 2 && c == 2) {
                                row.add("0"); 
                            } else {
                                row.add(String.valueOf(columns.get(c).get(r)));
                            }
                        }
                        card.add(row);
                    }
                    
                    session.setAttribute("card", card);
                    session.setAttribute("myConfirmedName", confirmedName);
                }
                
                List<List<String>> currentCard = (List<List<String>>) session.getAttribute("card");
                game.setPlayerCard(confirmedName, currentCard);
                
                request.setAttribute("game", game);
                request.setAttribute("confirmedPlayerName", confirmedName);
                request.getRequestDispatcher("index.jsp").forward(request, response);
            } else {
                request.setAttribute("error", "⚠️ 部屋番号（ゲームID）が正しくありません。");
                request.getRequestDispatcher("index.jsp").forward(request, response);
            }
            return;
        }

        String userType = request.getParameter("userType");
        request.setAttribute("game", game);
        
        if ("admin".equals(userType)) {
            request.getRequestDispatcher("admin.jsp").forward(request, response);
        } else {
            String confirmedName = (String) session.getAttribute("myConfirmedName");
            if (confirmedName == null) {
                confirmedName = request.getParameter("playerName");
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
