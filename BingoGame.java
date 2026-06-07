package servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BingoGame {
    private String gameId;
    private List<Integer> bingoNumbers;
    private List<Integer> drawnNumbers;
    private long createdAt;
    private long expiresAt; 
    private long lastBingoAt;

    private List<String> originalPlayers = new CopyOnWriteArrayList<>();
    private Map<String, String> uniquePlayerMap = new ConcurrentHashMap<>();
    private List<PlayerResult> bingoPlayers = new CopyOnWriteArrayList<>();
    private Map<String, List<List<String>>> playerCards = new ConcurrentHashMap<>();

    public BingoGame(String gameId, int validDays) {
        this.gameId = gameId;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + ((long) validDays * 24 * 60 * 60 * 1000); 
        this.lastBingoAt = System.currentTimeMillis();

        this.bingoNumbers = new ArrayList<>();
        for (int i = 1; i <= 75; i++) {
            bingoNumbers.add(i);
        }
        Collections.shuffle(bingoNumbers);
        this.drawnNumbers = new CopyOnWriteArrayList<>();
    }

    // 💡 アイデア②のための新機能：IDと有効期限を残したまま、ゲームデータだけをお掃除する
    public void clearGameDataOnly() {
        // 出た数字の履歴をゼロに戻す
        this.drawnNumbers.clear();
        
        // 残りの玉の山を新しくシャッフルして作り直す
        this.bingoNumbers.clear();
        for (int i = 1; i <= 75; i++) {
            bingoNumbers.add(i);
        }
        Collections.shuffle(bingoNumbers);

        // 参加者、ビンゴ・リーチ情報、カード情報をすべて綺麗にクリア
        this.originalPlayers.clear();
        this.uniquePlayerMap.clear();
        this.bingoPlayers.clear();
        this.playerCards.clear();

        // 最後に操作された時刻を今に更新
        this.lastBingoAt = System.currentTimeMillis();
    }

    public synchronized int drawNumber() {
        if (!bingoNumbers.isEmpty()) {
            int num = bingoNumbers.remove(0);
            drawnNumbers.add(num);
            this.lastBingoAt = System.currentTimeMillis();
            return num;
        }
        return -1;
    }

    public String getGameId() { return gameId; }
    public List<Integer> getDrawnNumbers() { return drawnNumbers; }
    public int getPlayerCount() { return uniquePlayerMap.size(); }
    public List<PlayerResult> getBingoPlayers() { return bingoPlayers; }

    public boolean isExpired() {
        return System.currentTimeMillis() > this.expiresAt;
    }

    public boolean isPast2HoursFromLastBingo() {
        long twoHours = 2 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - this.lastBingoAt) > twoHours;
    }

    public synchronized String registerPlayer(String baseName) {
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = "ゲスト";
        }
        originalPlayers.add(baseName);
        int count = 0;
        for (String p : originalPlayers) {
            if (p.equals(baseName)) count++;
        }
        String uniqueName = baseName;
        if (count > 1) {
            uniqueName = baseName + count;
        }
        uniquePlayerMap.put(uniqueName, baseName);
        return uniqueName;
    }

    public void setPlayerCard(String playerName, List<List<String>> card) {
        playerCards.put(playerName, card);
        checkPlayerStatus(playerName, card);
    }

    private void checkPlayerStatus(String playerName, List<List<String>> card) {
        boolean hasBingo = false;
        boolean hasReach = false;
        int drawnNumberAtBingo = -1;

        for (int i = 0; i < 5; i++) {
            if (checkLine(card.get(i))) { hasBingo = true; }
        }

        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            if (checkLine(col)) { hasBingo = true; }
        }

        List<String> diag1 = new ArrayList<>();
        List<String> diag2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            diag1.add(card.get(i).get(i));
            diag2.add(card.get(i).get(4 - i));
        }
        if (checkLine(diag1)) { hasBingo = true; }
        if (checkLine(diag2)) { hasBingo = true; }

        if (hasBingo) {
            boolean alreadyRegistered = false;
            for (PlayerResult pr : bingoPlayers) {
                if (pr.getPlayerName().equals(playerName)) {
                    alreadyRegistered = true;
                    break;
                }
            }
            if (!alreadyRegistered) {
                if (!drawnNumbers.isEmpty()) {
                    drawnNumberAtBingo = drawnNumbers.get(drawnNumbers.size() - 1);
                }
                bingoPlayers.add(new PlayerResult(playerName, true, false, drawnNumberAtBingo));
            }
            return;
        }

        for (int i = 0; i < 5; i++) { if (countHit(card.get(i)) == 4) { hasReach = true; } }
        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            if (countHit(col) == 4) { hasReach = true; }
        }
        if (countHit(diag1) == 4) { hasReach = true; }
        if (countHit(diag2) == 4) { hasReach = true; }

        if (hasReach) {
            boolean alreadyBingo = false;
            for (PlayerResult pr : bingoPlayers) {
                if (pr.getPlayerName().equals(playerName)) { alreadyBingo = true; break; }
            }
            if (!alreadyBingo) {
                boolean alreadyReach = false;
                for (PlayerResult pr : getReachPlayers()) {
                    if (pr.getPlayerName().equals(playerName)) { alreadyReach = true; break; }
                }
                if (!alreadyReach) {
                    // リーチ状態の保存ロジック（既存互換）
                }
            }
        }
    }

    private boolean checkLine(List<String> line) {
        for (String cell : line) {
            if ("0".equals(cell)) continue;
            if (!drawnNumbers.contains(Integer.parseInt(cell))) return false;
        }
        return true;
    }

    private int countHit(List<String> line) {
        int hit = 0;
        for (String cell : line) {
            if ("0".equals(cell)) { hit++; continue; }
            if (drawnNumbers.contains(Integer.parseInt(cell))) { hit++; }
        }
        return hit;
    }

    public List<PlayerResult> getReachPlayers() {
        List<PlayerResult> reachList = new ArrayList<>();
        for (Map.Entry<String, List<List<String>>> entry : playerCards.entrySet()) {
            String name = entry.getKey();
            List<List<String>> card = entry.getValue();
            
            boolean isBingo = false;
            for (PlayerResult pr : bingoPlayers) {
                if (pr.getPlayerName().equals(name)) { isBingo = true; break; }
            }
            if (isBingo) continue;

            boolean hasReach = false;
            for (int i = 0; i < 5; i++) { if (countHit(card.get(i)) == 4) hasReach = true; }
            for (int c = 0; c < 5; c++) {
                List<String> col = new ArrayList<>();
                for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
                if (countHit(col) == 4) hasReach = true;
            }
            List<String> diag1 = new ArrayList<>();
            List<String> diag2 = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                diag1.add(card.get(i).get(i));
                diag2.add(card.get(i).get(4 - i));
            }
            if (countHit(diag1) == 4) hasReach = true;
            if (countHit(diag2) == 4) hasReach = true;

            if (hasReach) {
                reachList.add(new PlayerResult(name, false, true, -1));
            }
        }
        return reachList;
    }

    public String getWaitNumbers(String playerName) {
        List<List<String>> card = playerCards.get(playerName);
        if (card == null) return "";
        List<Integer> waits = new ArrayList<>();

        for (int i = 0; i < 5; i++) { addWaitFromLine(card.get(i), waits); }
        for (int c = 0; c < 5; c++) {
            List<String> col = new ArrayList<>();
            for (int r = 0; r < 5; r++) { col.add(card.get(r).get(c)); }
            addWaitFromLine(col, waits);
        }
        List<String> diag1 = new ArrayList<>();
        List<String> diag2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            diag1.add(card.get(i).get(i));
            diag2.add(card.get(i).get(4 - i));
        }
        addWaitFromLine(diag1, waits);
        addWaitFromLine(diag2, waits);

        Collections.sort(waits);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < waits.size(); i++) {
            sb.append(waits.get(i));
            if (i < waits.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private void addWaitFromLine(List<String> line, List<Integer> waits) {
        if (countHit(line) == 4) {
            for (String cell : line) {
                if ("0".equals(cell)) continue;
                int val = Integer.parseInt(cell);
                if (!drawnNumbers.contains(val) && !waits.contains(val)) {
                    waits.add(val);
                }
            }
        }
    }
}
