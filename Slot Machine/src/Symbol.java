import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Symbol {
    private final String imagePath;
    private final int payoutMultiplier;
    private final int weight; //シンボルの重み

    private static final List<Symbol> ALL_SYMBOLS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static int TOTAL_WEIGHT = 0; //全シンボルの重みの合計

    static {
        // 重みを設定してシンボルを追加 (例: 重み10が基準の場合)
        // 重みが大きいほど出やすい
        ALL_SYMBOLS.add(new Symbol("images/02.png", 500, 5)); // ジャックポット: 重み4 (最も出にくい)
        ALL_SYMBOLS.add(new Symbol("images/21.png", 100, 10));  // 重み8
        ALL_SYMBOLS.add(new Symbol("images/30.png", 50, 15));   // 重み12
        ALL_SYMBOLS.add(new Symbol("images/christmas_bell.png", 30, 20));   // 重み16
        ALL_SYMBOLS.add(new Symbol("images/food_curryrice_white.png", 10, 25));  // 重み25
        ALL_SYMBOLS.add(new Symbol("images/tomato_red.png", 3, 25));  // 重み25 

        // 全シンボルの重みの合計を計算
        for (Symbol symbol : ALL_SYMBOLS) {
            TOTAL_WEIGHT += symbol.weight;
        }
    }

    private Symbol(String imagePath, int payoutMultiplier, int weight) { //コンストラクタに重みを追加
        this.imagePath = imagePath;
        this.payoutMultiplier = payoutMultiplier;
        this.weight = weight;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getPayoutMultiplier() {
        return payoutMultiplier;
    }

    public int getWeight() { // 重みを取得するメソッド
        return weight;
    }

    public static List<Symbol> getAllSymbols() {
        return Collections.unmodifiableList(ALL_SYMBOLS);
    }

    //重みに基づいてランダムなシンボルを返す
    public static Symbol getRandomSymbol() {
        if (TOTAL_WEIGHT == 0) { // シンボルが設定されていない場合の安全策
            return null; // またはデフォルトシンボルを返す
        }
        int randomNumber = RANDOM.nextInt(TOTAL_WEIGHT); // 0から全重みの合計-1までの乱数
        int currentWeightSum = 0;
        for (Symbol symbol : ALL_SYMBOLS) {
            currentWeightSum += symbol.weight;
            if (randomNumber < currentWeightSum) {
                return symbol;
            }
        }
        // ここには到達しないはずだが、念のため最初のシンボルを返す (エラーハンドリング)
        return ALL_SYMBOLS.get(0);
    }

    // 指定された数のランダムなシンボルのリストを返す (アニメーション用)
    // アニメーション中も重み付きのgetRandomSymbolを使うことで、よりリアルな動きになる
    public static List<Symbol> getRandomSymbols(int count) {
        List<Symbol> randomList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            randomList.add(getRandomSymbol()); // 重み付きのgetRandomSymbolを使う
        }
        return randomList;
    }

    public static Symbol getSymbolByImagePath(String path) {
        for (Symbol symbol : ALL_SYMBOLS) {
            if (symbol.getImagePath().equals(path)) {
                return symbol;
            }
        }
        return null;
    }
    
    public static List<Symbol> getRandomSymbolsWithoutDuplicates(int count) {
        List<Symbol> randomList = new ArrayList<>();
        while (randomList.size() < count) {
            Symbol s = getRandomSymbol();
            if (!randomList.contains(s)) { // Symbolクラスで適切にequalsが定義されている場合
                randomList.add(s);
            }
        }
        return randomList;
    }
}