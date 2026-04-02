import java.util.ArrayList;
import java.util.List;

public class GameLogic {
    private int currentMedals;
    private final Reel[] reels;

    public interface GameResultListener {
        void onSpinResult(String message, int acquiredMedals);
        void onMedalCountChanged(int newMedalCount);
    }

    private GameResultListener listener;

    private List<List<Symbol>> currentFinalReelResults;

    public GameLogic(int initialMedals) {
        this.currentMedals = initialMedals;
        this.reels = new Reel[3];
        for (int i = 0; i < 3; i++) {
            reels[i] = new Reel();
        }
        this.currentFinalReelResults = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<Symbol> row = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                row.add(Symbol.getRandomSymbol());
            }
            currentFinalReelResults.add(row);
        }
    }

    public void setGameResultListener(GameResultListener listener) {
        this.listener = listener;
    }

    public int getCurrentMedals() {
        return currentMedals;
    }

    public void deductMedals(int amount) {
        if (amount > 0) {
            this.currentMedals -= amount;
            if (listener != null) {
                listener.onMedalCountChanged(this.currentMedals);
            }
        }
    }

    public void spin(List<Integer> selectedLineIndices) {
        if (listener == null) {
            System.err.println("GameResultListener is not set.");
            return;
        }

        List<List<Symbol>> finalReelResults = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            finalReelResults.add(reels[i].spin());
        }
        this.currentFinalReelResults = finalReelResults;

        int maxPayout = 0;
        
        for (int lineIndex : selectedLineIndices) {
            Symbol[] currentLineSymbols = new Symbol[3];
            for (int i = 0; i < 3; i++) {
                currentLineSymbols[i] = finalReelResults.get(i).get(lineIndex);
            }
            
            int linePayout = calculatePayout(currentLineSymbols, SlotMachine.BASE_BET_AMOUNT_PER_LINE);
            
            if (linePayout > maxPayout) {
                maxPayout = linePayout;
            }
        }
        
        int totalPayout = maxPayout;

        if (totalPayout > 0) {
            // 当たりが出たら coin.wav を鳴らす
        	new SoundPlayer().playSound("coin.wav");
            
            currentMedals += totalPayout;
        }
        
        listener.onMedalCountChanged(currentMedals);
        String message = getResultMessage(totalPayout); // ここでメッセージを取得
        listener.onSpinResult(message, totalPayout);
    }

    private int calculatePayout(Symbol[] symbols, int betAmount) {
        // Symbol jackpotSymbol = Symbol.getSymbolByImagePath("images/02.png"); // 未使用のためコメントアウト
        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            return betAmount * symbols[0].getPayoutMultiplier();
        } 
        return 0;
    }

    // 配当が0の場合は空文字列を返す
    private String getResultMessage(int totalPayout) {
        if (totalPayout > 0) {
            return "合計 " + totalPayout + "メダル獲得！";
        }
        return ""; // 配当が0の場合は空文字列を返す
    }

    public List<Symbol> getReelSymbol(int colIndex) {
        if (colIndex >= 0 && colIndex < reels.length) {
            // currentFinalReelResultsはList<List<Symbol>>で、outerリストがリール番号、innerリストが行のシンボル
            // しかし、SlotMachine側は列のシンボルリストを期待しているため変換が必要
            List<Symbol> columnSymbols = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
                columnSymbols.add(currentFinalReelResults.get(colIndex).get(rowIndex));
            }
            return columnSymbols;
        }
        return null;
    }
}