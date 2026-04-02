graph TD
    // サブグラフを縦に配置

    subgraph Main[メイン画面]
        A[トップページ / 検索結果一覧<br/>'index.html' /]
    end

    subgraph Details[詳細画面]
        B[観光スポット詳細<br/>'detail.html' /spot]
        C[グルメスポット詳細<br/>'detail_gourmet.html' /gourmet]
        D[宿泊詳細<br/>'detail_lodging.html' /lodging]
    end

    subgraph Tools[ツール機能]
        E[しおり一覧<br/>'itinerary.html' /itinerary]
        F[全体マップ<br/>'map.html' /map]
    end

    // 接続：メインから詳細へ
    A -->|リストから選択| B
    A -->|リストから選択| C
    A -->|リストから選択| D
   
    // 接続：詳細からメイン・ツールへ
    B & C & D -->|一覧に戻る| A
    B & C & D -->|➕ しおりに追加| E

    // ツールから詳細への戻り
    E -.->|詳細を確認| B
    E -.->|詳細を確認| C
    E -.->|詳細を確認| D
   
    F -->|ピンをクリック| B
    F -->|ピンをクリック| C
    F -->|ピンをクリック| D

    // ナビゲーションバー（base.html）による共通遷移
    A -.->|Navbar: 📒しおり| E
    A -.->|🗺️全体マップを見る| F
   
    // 各詳細からもナビバーで移動可能
    B & C & D -.->|Navbar| E

    // スタイルの指定
    style Main fill:#f5f5f5,stroke:#333,stroke-width:2px
    style Details fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style Tools fill:#fff3e0,stroke:#e65100,stroke-width:2px
   
    // ノードの形状調整
    classDef default font-family:sans-serif,font-size:14px;