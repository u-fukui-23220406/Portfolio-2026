sequenceDiagram
    autonumber
    participant User as 利用者
    participant Browser as ブラウザ
    participant App as Flaskアプリ (app.py)
    participant DB as SQLAlchemy (models.py)
    participant Session as セッション (Cookie)

    Note over User, Session: 【1. 検索・一覧表示処理】
    User->>+Browser: 検索条件入力 (キーワード/位置情報)
    Browser->>+App: GET /?type=spot&keyword=...&lat=36...
    App->>DB: 指定タイプ(Spot/Gourmet/Lodging)をクエリ
    DB-->>App: 該当データリスト
   
    opt 位置情報(lat/lng)がある場合
        loop 各スポットに対して
            App->>App: calculate_distance() で距離算出
        end
        App->>App: 距離が近い順にソート
    end
   
    App->>App: 1ページ9件にページネーション切出し
    App-->>-Browser: index.html (結果一覧) レンダリング
    Browser-->>-User: 一覧画面を表示

    Note over User, Session: 【2. 詳細表示 ＆ 周辺レコメンド】
    User->>+Browser: 「詳細を見る」をクリック
    Browser->>+App: GET /spot/<id> (or /gourmet/, /lodging/)
    App->>DB: db.session.get() で主役データを取得
    DB-->>App: オブジェクト(item)
   
    rect rgb(240, 248, 255)
        Note right of App: 周辺情報の取得ロジック
        alt 観光地(Spot)詳細の場合
            App->>DB: ServiceCounter(荷物預かり)を全件取得
        else グルメ/宿泊詳細の場合
            App->>DB: TouristSpot(周辺観光地)を全件取得
        end
        DB-->>App: マスタリスト
        App->>App: 各データとの距離を計算し、近いものを抽出
    end

    App-->>-Browser: detail.html / detail_lodging.html 等
    Browser-->>-User: 詳細・周辺情報を表示

    Note over User, Session: 【3. しおり(お気に入り)機能】
    User->>+Browser: 「しおりに追加」をクリック
    Browser->>+App: GET /add_favorite/<type>/<id>
    App->>Session: session.get('my_itinerary') を確認
    App->>Session: リストに {'type': type, 'id': id} を追加保存
    App-->>-Browser: Redirect to 遷移元ページ (flashメッセージ付)
    Browser-->>-User: 画面更新 (しおりバッジの件数が増える)

    User->>+Browser: 「しおり」メニューをクリック
    Browser->>+App: GET /itinerary
    App->>Session: セッションから保存済みIDリストを取得
    loop 保存された各アイテム
        App->>DB: モデルとIDに基づき実データを取得
    end
    DB-->>App: spot_list, gourmet_list, lodging_list
    App-->>-Browser: itinerary.html (しおり一覧) レンダリング
    Browser-->>-User: 自分の「しおり」画面を表示