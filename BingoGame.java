package servlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class BingoGame implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameId;                             
    private List<Integer> drawnNumbers;                
    private List<PlayerResult> bingoPlayers;           
    private List<PlayerResult> reachPlayers;           
    private List<String> allPlayers;                   
    private Date expireTime;                           
    private Date lastBingoTime;                        
    private int anonymousCount = 0;                    

    private ConcurrentHashMap<String, List<List<String>>> playerCards = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<String>> playerWaitNumbers = new ConcurrentHashMap<>();

    public BingoGame(String gameId, int validDays) {
        this.gameId = gameId;
        this.drawnNumbers = new CopyOnWriteArrayList<>();
        this.bingoPlayers = new CopyOnWriteArrayList<>();
        this.reachPlayers = new CopyOnWriteArrayList<>();
        this.allPlayers = new CopyOnWriteArrayList<>();
        this.lastBingoTime = new Date(); // 初期値として現在の時刻を設定

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, validDays);
        this.expireTime = cal.getTime();
    }

    // 💡 アイデア②：IDと有効期限（タイマー）は一切変えず、ゲーム進行データだけをクリアする
    public void clearGameDataOnly() {
        this.drawnNumbers.clear();       // 出た数字の履歴をゼロに
        this.bingoPlayers.clear();       // ビンゴ達成者一覧をゼロに
        this.reachPlayers.clear();       // リーチの人一覧をゼロに
        this.allPlayers.clear();         // 参加プレイヤーリストをゼロに
        this.playerCards.clear();        // プレイヤーのカードデータをゼロに
        this.playerWaitNumbers.clear();  // プレイヤーの待ち番号データをゼロに
        
        this.lastBingoTime = new Date(); // 最新操作時刻をリセットした「今」に更新
    }

    public int drawNumber() {
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 75; i++) {
            if (!drawnNumbers.contains(i)) {
                pool.add(i);
            }
        }
        if (pool.isEmpty()) return -1;
        
        java.util.Collections.shuffle(pool);
        int nextNum = pool.get(0);
        drawnNumbers.add(nextNum);
        
        this.lastBingoTime = new Date(); // 数字を引いた時刻を更新
        
        updateAllPlayersStatus();
        return nextNum;
    }

    private void updateAllPlayersStatus() {
        bingoPlayers.clear();
        reachPlayers.clear();
        
        int currentDrawnNumber = drawnNumbers.isEmpty() ? -1 : drawnNumbers.get(drawnNumbers.size() - 1);

        for (String name : playerCards.keySet()) {
            List<List<String>> card = playerCards.get(name);
            List<String> waits = calculateWaitNumbers(card);
            playerWaitNumbers.put(name, waits);

            if (waits.isEmpty()) {
                addBingoPlayer(name, currentDrawnNumber);
            } else if (waits.size() == 1 || waits.size() == 2 || waits.size() == 3 || waits.size() == 4) {
                // 待ち番号がある場合はリーチ判定
                boolean hasRealReach = checkActualReachLines(card);
                if (hasRealReach) {
                    addReachPlayer(name);
                }
            }
        }
    }

    private boolean checkActualReachLines(List<List<String>> card) {
        for (int i = 0; i < 5; i++) {
            if (countHitInLine(card.get(i)) == 4) return true;
        }
        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            if (countHitInLine(col) == 4) return true;
        }
        List<String> d1 = new ArrayList<>();
        List<String> d2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            d1.add(card.get(i).get(i));
            d2.add(card.get(i).get(4 - i));
        }
        if (countHitInLine(d1) == 4) return true;
        if (countHitInLine(d2) == 4) return true;
        return false;
    }

    private int countHitInLine(List<String> line) {
        int hit = 0;
        for (String cell : line) {
            if ("0".equals(cell) || drawnNumbers.contains(Integer.parseInt(cell))) {
                hit++;
            }
        }
        return hit;
    }

    private List<String> calculateWaitNumbers(List<List<String>> card) {
        List<String> waits = new ArrayList<>();
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                String cell = card.get(r).get(c);
                if (!"0".equals(cell) && !drawnNumbers.contains(Integer.parseInt(cell))) {
                    if (!waits.contains(cell)) {
                        waits.add(cell);
                    }
                }
            }
        }
        return waits;
    }

    private void addBingoPlayer(String name, int currentDrawnNumber) {
        for (PlayerResult p : bingoPlayers) {
            if (p.getPlayerName().equals(name)) return;
        }
        Date now = new Date();
        bingoPlayers.add(0, new PlayerResult(name, now, currentDrawnNumber));
        this.lastBingoTime = now;
        removeReachPlayer(name);
    }

    private void addReachPlayer(String name) {
        for (PlayerResult p : reachPlayers) {
            if (p.getPlayerName().equals(name)) return;
        }
        reachPlayers.add(0, new PlayerResult(name, new Date(), 0));
    }

    public void removeReachPlayer(String name) {
        reachPlayers.removeIf(p -> p.getPlayerName().equals(name));
    }

    public List<String> getWaitNumbers(String name) {
        return playerWaitNumbers.getOrDefault(name, new ArrayList<>());
    }

    public boolean isExpired() { return new Date().after(this.expireTime); }
    
    public boolean isPast2HoursFromLastBingo() {
        if (bingoPlayers.isEmpty()) return false;
        long twoHoursInMilliseconds = 2L * 60 * 60 * 1000;
        long timePassed = new Date().getTime() - lastBingoTime.getTime();
        return timePassed > twoHoursInMilliseconds;
    }

    public String getGameId() { return gameId; }
    public List<Integer> getDrawnNumbers() { return drawnNumbers; }
    public List<PlayerResult> getBingoPlayers() { return bingoPlayers; }
    public List<PlayerResult> getReachPlayers() { return reachPlayers; }
    public int getPlayerCount() { return playerCards.size(); }
    public List<String> getAllPlayers() { return allPlayers; }

    public void setPlayerCard(String playerName, List<List<String>> card) {
        playerCards.put(playerName, card);
        updateAllPlayersStatus();
    }
}
