# check_data.py
from app import app
from models import db, TouristSpot, ServiceCounter

with app.app_context():
    # 観光地の件数
    spot_count = db.session.query(TouristSpot).count()
    # カウンターの件数
    counter_count = db.session.query(ServiceCounter).count()

    print("--------------------------------------------------")
    print(f"観光地データ件数: {spot_count} 件")
    print(f"カウンターデータ件数: {counter_count} 件")
    
    if spot_count > 0:
        first_spot = db.session.query(TouristSpot).first()
        print(f"データ例 (観光地): ID={first_spot.id}, 名前={first_spot.name}, エリア={first_spot.area}")
    else:
        print("警告: 観光地データが空です！ CSVの読み込みに失敗しています。")
        
    print("--------------------------------------------------")