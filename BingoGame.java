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
        this.lastBingoTime = new Date(); 

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, validDays);
        this.expireTime = cal.getTime();
    }

    // 👤 無記名のときに「ゲスト1」「ゲスト2」と安全に名前を生成する部品
    public synchronized String generateAnonymousName() {
        this.anonymousCount++;
        return "ゲスト" + this.anonymousCount;
    }

    public void clearGameDataOnly() {
        this.drawnNumbers.clear();       
        this.bingoPlayers.clear();       
        this.reachPlayers.clear();       
        this.allPlayers.clear();         
        this.playerCards.clear();        
        this.playerWaitNumbers.clear();  
        this.lastBingoTime = new Date(); 
        this.anonymousCount = 0;
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
        this.lastBingoTime = new Date(); 
        
        updateAllPlayersStatus();
        return nextNum;
    }

    // 👥 プレイヤー全員のビンゴ・リーチ状態をライン基準で正しく更新する
    public void updateAllPlayersStatus() {
        bingoPlayers.clear();
        reachPlayers.clear();
        
        int currentDrawnNumber = drawnNumbers.isEmpty() ? -1 : drawnNumbers.get(drawnNumbers.size() - 1);

        for (String name : playerCards.keySet()) {
            List<List<String>> card = playerCards.get(name);
            
            // 縦横斜めをチェックして「本物の待ち番号」を抽出
            List<String> waits = calculateActualWaitNumbers(card);
            playerWaitNumbers.put(name, waits);

            // 1列でも揃っていれば（checkBingoが真）ビンゴ達成者へ
            if (checkBingo(card)) {
                addBingoPlayer(name, currentDrawnNumber);
            } 
            // ビンゴしていないが、あと1マスで揃うラインがあればリーチ達成者へ
            else if (checkActualReachLines(card)) {
                addReachPlayer(name);
            }
        }
    }

    // 🏆 1列揃っているラインが1つでもあるか判定（ビンゴ用）
    private boolean checkBingo(List<List<String>> card) {
        // 横
        for (int i = 0; i < 5; i++) {
            if (countHitInLine(card.get(i)) == 5) return true;
        }
        // 縦
        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            if (countHitInLine(col) == 5) return true;
        }
        // 斜め
        List<String> d1 = new ArrayList<>();
        List<String> d2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            d1.add(card.get(i).get(i));
            d2.add(card.get(i).get(4 - i));
        }
        if (countHitInLine(d1) == 5) return true;
        if (countHitInLine(d2) == 5) return true;
        
        return false;
    }

    // 📏 あと1マスで揃うラインが1つでもあるか判定（リーチ用）
    private boolean checkActualReachLines(List<List<String>> card) {
        // 横
        for (int i = 0; i < 5; i++) {
            if (countHitInLine(card.get(i)) == 4) return true;
        }
        // 縦
        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            if (countHitInLine(col) == 4) return true;
        }
        // 斜め
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

    // 🔮 あと何番が出れば一列揃うか、ラインごとの待ち番号リストを作る処理
    private List<String> calculateActualWaitNumbers(List<List<String>> card) {
        List<String> waits = new ArrayList<>();
        // 横
        for (int i = 0; i < 5; i++) { getWaitFromLine(card.get(i), waits); }
        // 縦
        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            getWaitFromLine(col, waits);
        }
        // 斜め
        List<String> d1 = new ArrayList<>();
        List<String> d2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            d1.add(card.get(i).get(i));
            d2.add(card.get(i).get(4 - i));
        }
        getWaitFromLine(d1, waits);
        getWaitFromLine(d2, waits);

        return waits;
    }

    private void getWaitFromLine(List<String> line, List<String> waits) {
        if (countHitInLine(line) == 4) {
            for (String cell : line) {
                if (!"0".equals(cell) && !drawnNumbers.contains(Integer.parseInt(cell))) {
                    if (!waits.contains(cell)) {
                        waits.add(cell);
                    }
                }
            }
        }
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
        for (PlayerResult p : bingoPlayers) {
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
        if (drawnNumbers.isEmpty() && bingoPlayers.isEmpty()) return false;
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
