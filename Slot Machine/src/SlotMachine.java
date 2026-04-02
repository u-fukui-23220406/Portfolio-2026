import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class SlotMachine extends JFrame implements GameLogic.GameResultListener {

    private final JLabel[][] reelLabels = new JLabel[3][3];
    private final JButton spinButton;
    private final JLabel resultLabel;
    private final JLabel medalCountLabel;
    private final JLabel betAmountLabel;
    private Clip bgmClip;

    private JButton selectAllLinesButton;
    private JButton addLineButton;

    private List<Boolean> selectedLines;

    private final GameLogic gameLogic;
    private final SoundPlayer soundPlayer;

    private static final int REEL_IMAGE_SIZE = 100;
    private static final int SPIN_DURATION_MS = 2000;
    private static final int STOP_INTERVAL_MS = 300;

    public static final int BASE_BET_AMOUNT_PER_LINE = 100;

    private Timer spinTimer;
    private Timer stopTimer;
    private int currentStoppingReelColumn = 0;

    private String lastGameResultMessage;
    

    private static final Color SELECTED_LINE_BORDER_COLOR = Color.RED;
    private static final Color DEFAULT_LINE_BORDER_COLOR = Color.black;

    public SlotMachine() {
        // ウィンドウサイズを配当表パネルの分だけ広げます
        setSize(REEL_IMAGE_SIZE * 3 + 500 + 200, REEL_IMAGE_SIZE * 3 + 600); // 200px分、横幅を追加
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        gameLogic = new GameLogic(50000);
        gameLogic.setGameResultListener(this);
        soundPlayer = new SoundPlayer();

        selectedLines = new ArrayList<>();
        selectedLines.add(false); // 上段
        selectedLines.add(true);  // 中段
        selectedLines.add(false); // 下段

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(25, 25, 112));

        JPanel reelPanel = new JPanel();
        reelPanel.setLayout(new GridLayout(3, 3, 5, 5));
        reelPanel.setBackground(new Color(25, 25, 112));

        // 1. 先に「3本のリール（縦棒）」の中身を、重複なしで作ってしまう
        List<Symbol> col0 = Symbol.getRandomSymbolsWithoutDuplicates(3); // 左リール用
        List<Symbol> col1 = Symbol.getRandomSymbolsWithoutDuplicates(3); // 中リール用
        List<Symbol> col2 = Symbol.getRandomSymbolsWithoutDuplicates(3); // 右リール用

        // 2. 作った縦棒から、1マスずつ「横」の順番で画面にはめ込んでいく
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                reelLabels[row][col] = new JLabel();
                
                // 列(col)に応じて、さっき作った「縦棒リスト」から row番目を取り出す
                Symbol s;
                if (col == 0) s = col0.get(row);
                else if (col == 1) s = col1.get(row);
                else s = col2.get(row);

                updateReelImage(row, col, s);
                
                // --- 以下、見た目の設定（変更なし） ---
                reelLabels[row][col].setHorizontalAlignment(SwingConstants.CENTER);
                reelLabels[row][col].setVerticalAlignment(SwingConstants.CENTER);
                reelLabels[row][col].setOpaque(true);
                reelLabels[row][col].setBackground(new Color(240, 248, 255));
                reelLabels[row][col].setBorder(BorderFactory.createLineBorder(DEFAULT_LINE_BORDER_COLOR, 3));
                reelPanel.add(reelLabels[row][col]);
            }
        }
        
        mainPanel.add(reelPanel, BorderLayout.CENTER);

        // 配当表パネルを作成してメインパネルの右側に配置
        JPanel payoutPanel = createPayoutPanel();
        mainPanel.add(payoutPanel, BorderLayout.EAST);

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(70, 130, 180));
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        medalCountLabel = new JLabel("メダル: " + gameLogic.getCurrentMedals());
        medalCountLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        medalCountLabel.setForeground(Color.WHITE);
        controlPanel.add(medalCountLabel);

        betAmountLabel = new JLabel("BET: " + calculateCurrentBetAmount());
        betAmountLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        betAmountLabel.setForeground(Color.WHITE);
        controlPanel.add(betAmountLabel);

        selectAllLinesButton = new JButton("全ライン選択");
        selectAllLinesButton.addActionListener(e -> {
        	soundPlayer.playSound("select.wav");
            selectAllLines();
            updateBetAmountLabel();
        });
        controlPanel.add(selectAllLinesButton);

        addLineButton = new JButton("ライン追加");
        addLineButton.addActionListener(e -> {
        	soundPlayer.playSound("select.wav");
            addLine();
            updateBetAmountLabel();
        });
        controlPanel.add(addLineButton);

        spinButton = new JButton("スピン！");
        spinButton.setFont(new Font("SansSerif", Font.BOLD, 24));
        spinButton.setBackground(new Color(255, 215, 0));
        spinButton.setForeground(Color.BLACK);
        spinButton.setFocusPainted(false);
        spinButton.addActionListener(new SpinButtonActionListener());
        controlPanel.add(spinButton);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        JPanel resultPanel = new JPanel();
        resultPanel.setBackground(new Color(25, 25, 112));
        resultPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        resultLabel = new JLabel("結果: スピンしてください");
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        resultLabel.setForeground(Color.YELLOW);
        resultPanel.add(resultLabel);

        mainPanel.add(resultPanel, BorderLayout.NORTH);

        add(mainPanel);
        playBGM("bgm.wav");
        setVisible(true);

        updateLineBorders();
        updateBetAmountLabel();
    }

    /**
     * 配当表パネルを作成します。
     * 各シンボルの画像と、それが3つ揃った場合の配当倍率を表示します。
     * @return 配当表を表示するJPanel
     */
    private JPanel createPayoutPanel() {
        JPanel payoutPanel = new JPanel();
        payoutPanel.setLayout(new GridLayout(0, 1, 5, 5)); // 1列で自動的に行数を調整
        payoutPanel.setBackground(new Color(47, 79, 79)); // ダークスレートグレー
        payoutPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2),
            "配当表",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 18),
            Color.WHITE
        ));

        // すべてのシンボルを取得し、配当情報を表示
        List<Symbol> allSymbols = Symbol.getAllSymbols();
        for (Symbol symbol : allSymbols) {
            JPanel symbolPayoutEntry = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            symbolPayoutEntry.setBackground(new Color(47, 79, 79));

            // シンボル画像
            JLabel symbolImageLabel = new JLabel();
            // REEL_IMAGE_SIZEの半分程度のサイズで表示すると良いでしょう
            ImageIcon originalIcon = new ImageIcon(getClass().getClassLoader().getResource(symbol.getImagePath()));
            Image image = originalIcon.getImage().getScaledInstance(REEL_IMAGE_SIZE / 2, REEL_IMAGE_SIZE / 2, Image.SCALE_SMOOTH);
            symbolImageLabel.setIcon(new ImageIcon(image));
            symbolPayoutEntry.add(symbolImageLabel);

            // "x3" 表示
            JLabel countLabel = new JLabel("x 3");
            countLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            countLabel.setForeground(Color.WHITE);
            symbolPayoutEntry.add(countLabel);

            // 配当額
            JLabel payoutLabel = new JLabel("= " + (BASE_BET_AMOUNT_PER_LINE * symbol.getPayoutMultiplier()) + " メダル");
            payoutLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            payoutLabel.setForeground(Color.YELLOW);
            symbolPayoutEntry.add(payoutLabel);

            payoutPanel.add(symbolPayoutEntry);
        }

        return payoutPanel;
    }

    private void selectAllLines() {
        for (int i = 0; i < selectedLines.size(); i++) {
            selectedLines.set(i, true);
        }
        updateLineBorders();
    }

    private void addLine() {
        int currentSelectedCount = 0;
        for (boolean selected : selectedLines) {
            if (selected) {
                currentSelectedCount++;
            }
        }

        if (currentSelectedCount == 3) {
            selectedLines.set(0, false);
            selectedLines.set(1, true);
            selectedLines.set(2, false);
        } else {
            for (int i = 0; i < selectedLines.size(); i++) {
                if (!selectedLines.get(i)) {
                    selectedLines.set(i, true);
                    break;
                }
            }
        }
        updateLineBorders();
    }

    private void updateLineBorders() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (selectedLines.get(row)) {
                    reelLabels[row][col].setBorder(BorderFactory.createLineBorder(SELECTED_LINE_BORDER_COLOR, 3));
                } else {
                    reelLabels[row][col].setBorder(BorderFactory.createLineBorder(DEFAULT_LINE_BORDER_COLOR, 3));
                }
            }
        }
    }

    private int calculateCurrentBetAmount() {
        int selectedLineCount = 0;
        for (boolean selected : selectedLines) {
            if (selected) {
                selectedLineCount++;
            }
        }
        return BASE_BET_AMOUNT_PER_LINE * selectedLineCount;
    }

    private void updateBetAmountLabel() {
        betAmountLabel.setText("BET: " + calculateCurrentBetAmount());
    }

    private void updateReelImage(int row, int col, Symbol symbol) {
        updateImageForLabel(reelLabels[row][col], symbol);
    }

    private void updateAllReelsSpinningImages() {
        for (int col = 0; col < 3; col++) {
            // Symbol.getRandomSymbols(3) ではなく、Reelクラスの重複回避ロジックを模した
            // もしくは一時的にReelオブジェクトのメソッドを借りる形にします
            List<Symbol> randomSymbols = Symbol.getRandomSymbolsWithoutDuplicates(3); 
            for (int row = 0; row < 3; row++) {
                updateImageForLabel(reelLabels[row][col], randomSymbols.get(row));
            }
        }
    }

    private void updateImageForLabel(JLabel label, Symbol symbol) {
        if (symbol != null) {
            java.net.URL imageUrl = getClass().getClassLoader().getResource(symbol.getImagePath());
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                Image image = originalIcon.getImage().getScaledInstance(REEL_IMAGE_SIZE, REEL_IMAGE_SIZE, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(image));
                label.setText("");
            } else {
                label.setText("画像なし: " + symbol.getImagePath());
                label.setIcon(null);
                System.err.println("Warning: Image not found at " + symbol.getImagePath());
            }
        }
    }

    @Override
    public void onSpinResult(String message, int acquiredMedals) {
        SwingUtilities.invokeLater(() -> {
            lastGameResultMessage = message;
            currentStoppingReelColumn = 0;
            stopTimer = new Timer(STOP_INTERVAL_MS, new StopReelActionListener());
            stopTimer.setRepeats(true);
            stopTimer.start();
        });
    }

    @Override
    public void onMedalCountChanged(int newMedalCount) {
        SwingUtilities.invokeLater(() -> {
            medalCountLabel.setText("メダル: " + newMedalCount);
        });
    }

    private List<Integer> getCurrentlySelectedLineIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < selectedLines.size(); i++) {
            if (selectedLines.get(i)) {
                indices.add(i);
            }
        }
        return indices;
    }

    private class SpinButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int betAmount = calculateCurrentBetAmount();
            List<Integer> selectedLineIndices = getCurrentlySelectedLineIndices();

            if (selectedLineIndices.isEmpty()) {
                resultLabel.setText("ラインを選択してください！");
                return;
            }

            if (gameLogic.getCurrentMedals() < betAmount) {
                resultLabel.setText("メダルが足りません！");
                return;
            }
            gameLogic.deductMedals(betAmount);
            soundPlayer.playSound("spin.wav");

            spinButton.setEnabled(false);
            selectAllLinesButton.setEnabled(false);
            addLineButton.setEnabled(false);

            resultLabel.setText("回転中...");

            spinTimer = new Timer(50, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateAllReelsSpinningImages();
                }
            });
            spinTimer.setRepeats(true);
            spinTimer.start();

            Timer delaySpinLogicTimer = new Timer(SPIN_DURATION_MS, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    spinTimer.stop();
                    ((Timer) e.getSource()).stop();
                    gameLogic.spin(selectedLineIndices);
                }
            });
            delaySpinLogicTimer.setRepeats(false);
            delaySpinLogicTimer.start();
        }
    }

    private class StopReelActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentStoppingReelColumn < 3) {
                List<Symbol> finalSymbols = gameLogic.getReelSymbol(currentStoppingReelColumn);
                for (int row = 0; row < 3; row++) {
                    updateReelImage(row, currentStoppingReelColumn, finalSymbols.get(row));
                }
                currentStoppingReelColumn++;
            } else {
                stopTimer.stop();
                spinButton.setEnabled(true);
                selectAllLinesButton.setEnabled(true);
                addLineButton.setEnabled(true);

                // メッセージが空でなければ設定
                if (!lastGameResultMessage.isEmpty()) {
                    resultLabel.setText("結果: " + lastGameResultMessage);
                } else {
                    // メッセージが空の場合は、結果ラベルをクリアするか、初期状態に戻す
                    resultLabel.setText(""); // または "結果: " など
                }


                selectedLines.set(0, false);
                selectedLines.set(1, true);
                selectedLines.set(2, false);
                updateLineBorders();
                updateBetAmountLabel();
            }
        }
    }
    
    private void playBGM(String fileName) {
        try {
            // パスの候補をいくつか試す
            java.net.URL url = getClass().getClassLoader().getResource("sounds/" + fileName);
            
            if (url == null) {
                // 見つからない場合はコンソールに警告を出す
                System.err.println("【警告】音ファイルが見つかりません: src/sounds/" + fileName);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(ais);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();

        } catch (Exception e) {
            System.err.println("【エラー】再生中に問題が発生しました:");
            e.printStackTrace();
        }
    }
}