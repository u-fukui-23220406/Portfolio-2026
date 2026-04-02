import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Reel {
    private List<Symbol> currentSymbols;
    private static final int NUM_ROWS = 3;

    public Reel() {
        currentSymbols = new ArrayList<>();
        spin(); 
    }

    // 縦の並びで重複しないようにシンボルを選択
    public List<Symbol> spin() {
        currentSymbols = generateUniqueSymbols(NUM_ROWS);
        return new ArrayList<>(currentSymbols);
    }

    // アニメーション中も重複しないようにする
    public List<Symbol> getRandomSymbolsForAnimation() {
        return generateUniqueSymbols(NUM_ROWS);
    }

    /**
     * 重複のないシンボルリストを生成するヘルパーメソッド
     */
    private List<Symbol> generateUniqueSymbols(int count) {
        List<Symbol> uniqueList = new ArrayList<>();
        
        while (uniqueList.size() < count) {
            Symbol newSymbol = Symbol.getRandomSymbol();
            
            // すでにリストに含まれているかチェック（画像パスで比較）
            boolean isDuplicate = false;
            for (Symbol s : uniqueList) {
                if (s.getImagePath().equals(newSymbol.getImagePath())) {
                    isDuplicate = true;
                    break;
                }
            }
            
            // 重複していなければ追加
            if (!isDuplicate) {
                uniqueList.add(newSymbol);
            }
            
            // 万が一、全シンボル数より count が大きいと無限ループになるため注意
            // (現在の仕様ではシンボル6種類に対し count=3 なので安全です)
        }
        return uniqueList;
    }

    public List<Symbol> getCurrentSymbols() {
        return Collections.unmodifiableList(currentSymbols);
    }
}