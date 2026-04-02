import csv
import re
from app import app
from models import db, TouristSpot, GourmetSpot, ServiceCounter, Lodging

SPOT_CSV = 'tourist_spots.csv'
HANDS_CSV = 'hands_free.csv'
FOOD_CERT_CSV = 'food_cert.csv'
FOOD_SHOP_CSV = 'food_shop.csv'
LODGING_CSV = 'lodging.csv'

# 住所からエリア名（市町名）を抜き出す関数
def extract_area(address):
    if not address: return "その他"
    addr = address.replace("福井県", "").strip()
    # "○○市"、"○○郡○○町" を取り出す
    match = re.match(r'(.+?[市町村])', addr)
    if match:
        # "丹生郡越前町" -> "越前町" のように郡を消す処理
        clean_area = match.group(1)
        if "郡" in clean_area:
            clean_area = re.sub(r'.+?郡', '', clean_area)
        return clean_area
    return "その他"

def init_db():
    with app.app_context():
        db.drop_all()
        db.create_all()
        print("DB初期化開始...")

        # 1. 観光地
        try:
            with open(SPOT_CSV, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    try:
                        if not row.get('lat') or not row.get('lng'): continue
                        spot = TouristSpot(
                            id=int(row.get('id', 0)),
                            name=row.get('name'),
                            category=row.get('category'),
                            area=row.get('area'),
                            address=row.get('住所'),
                            description=row.get('description'),
                            img_url=row.get('image'),
                            lat=float(row.get('lat')),
                            lng=float(row.get('lng')),
                            parking_info=row.get('駐車場：備考') or row.get('駐車場'),
                            business_hours=row.get('営業時間'),
                            holiday=row.get('定休日'),
                            price=row.get('料金')
                        )
                        db.session.add(spot)
                    except: continue
            print("観光地完了")
        except: pass

        # 2. 食事データA
        try:
            with open(FOOD_CERT_CSV, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    try:
                        if not row.get('緯度') or not row.get('経度'): continue
                        area_name = extract_area(row.get('住所'))
                        spot = GourmetSpot(
                            name=row.get('店舗名'),
                            category=row.get('区分', '飲食店'),
                            address=row.get('住所'),
                            area=area_name,
                            tel=row.get('電話番号'),
                            business_hours=row.get('営業時間'),
                            holiday=row.get('休業日'),
                            lat=float(row.get('緯度')),
                            lng=float(row.get('経度')),
                            menu_info=row.get('認証のメニュー名'),
                            pr_text=row.get('認証のメニューのPRポイント') or row.get('備考')
                        )
                        db.session.add(spot)
                    except: continue
            print("食事A完了")
        except: pass

        # 3. 食事データB
        try:
            with open(FOOD_SHOP_CSV, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    try:
                        if not row.get('緯度') or not row.get('経度'): continue
                        menus = []
                        if row.get('おすすめ1'): menus.append(f"{row['おすすめ1']}({row.get('おすすめ1（価格）','')})")
                        area_name = extract_area(row.get('住所'))
                        spot = GourmetSpot(
                            name=row.get('店舗名'),
                            category='郷土料理・そば',
                            address=row.get('住所'),
                            area=area_name,
                            tel=row.get('電話番号'),
                            business_hours=row.get('営業時間'),
                            holiday=row.get('定休日'),
                            lat=float(row.get('緯度')),
                            lng=float(row.get('経度')),
                            menu_info=" / ".join(menus),
                            pr_text=row.get('店主のひとこと')
                        )
                        db.session.add(spot)
                    except: continue
            print("食事B完了")
        except: pass

        # 4. 宿泊データ
        try:
            with open(LODGING_CSV, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                print(f"宿泊CSV列名: {reader.fieldnames}")
                for row in reader:
                    try:
                        if not row.get('緯度') or not row.get('経度'): continue
                        
                        area_name = extract_area(row.get('所在地'))

                        lodging = Lodging(
                            type=row.get('営業種別'),
                            name=row.get('名称'),
                            address=row.get('所在地'),
                            area=area_name, # エリアを自動抽出して保存
                            tel=row.get('電話番号'),
                            rooms=row.get('部屋数'),
                            capacity=row.get('定員'),
                            lat=float(row.get('緯度')),
                            lng=float(row.get('経度'))
                        )
                        db.session.add(lodging)
                    except Exception as e:
                        # print(f"宿泊エラー: {e}") 
                        continue
            print("宿泊データ完了")
        except Exception as e: print(f"宿泊CSV読み込み失敗: {e}")

        # 5. 手ぶら観光
        try:
            with open(HANDS_CSV, 'r', encoding='utf-8-sig') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    try:
                        if not row.get('カウンター住所_緯度'): continue
                        is_cool = (row.get('配送_一般配送_取扱可能品目_クール品フラグ') == '1' or row.get('一時預かり_取扱可能品目_クール品フラグ') == '1')
                        langs = []
                        if row.get('対応可能言語_英語フラグ') == '1': langs.append("英語")
                        if row.get('対応可能言語_中国語（簡体字）フラグ') == '1': langs.append("中国語")
                        
                        counter = ServiceCounter(
                            id=row.get('カウンターID'),
                            name=row.get('カウンター名称（日本語）'),
                            address=row.get('カウンター住所_大字丁目・番地等（日本語）'),
                            lat=float(row.get('カウンター住所_緯度')),
                            lng=float(row.get('カウンター住所_経度')),
                            is_cool_delivery=is_cool,
                            languages=",".join(langs),
                            business_hours=f"{row.get('営業時間_月曜日_開始時間')} - {row.get('営業時間_月曜日_終了時間')}",
                            price=row.get('一時預かり_料金_固定') or "要確認"
                        )
                        db.session.add(counter)
                    except: continue
            print("手ぶら完了")
        except: pass

        db.session.commit()
        print("=== 全完了 ===")

if __name__ == '__main__':
    init_db()