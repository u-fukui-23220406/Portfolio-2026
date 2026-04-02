from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class TouristSpot(db.Model):
    __tablename__ = 'tourist_spots'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String)
    category = db.Column(db.String)
    area = db.Column(db.String)
    address = db.Column(db.String)
    description = db.Column(db.String)
    img_url = db.Column(db.String)
    lat = db.Column(db.Float)
    lng = db.Column(db.Float)
    parking_info = db.Column(db.String)
    business_hours = db.Column(db.String)
    holiday = db.Column(db.String)
    price = db.Column(db.String)
    distance_from_user = 0.0

class GourmetSpot(db.Model):
    __tablename__ = 'gourmet_spots'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String)
    category = db.Column(db.String)
    address = db.Column(db.String)
    area = db.Column(db.String) # エリア
    tel = db.Column(db.String)
    business_hours = db.Column(db.String)
    holiday = db.Column(db.String)
    lat = db.Column(db.Float)
    lng = db.Column(db.Float)
    menu_info = db.Column(db.String)
    pr_text = db.Column(db.String)
    distance_from_user = 0.0

class ServiceCounter(db.Model):
    __tablename__ = 'service_counters'
    id = db.Column(db.String, primary_key=True)
    name = db.Column(db.String)
    address = db.Column(db.String)
    lat = db.Column(db.Float)
    lng = db.Column(db.Float)
    is_cool_delivery = db.Column(db.Boolean)
    languages = db.Column(db.String)
    business_hours = db.Column(db.String)
    price = db.Column(db.String)
    distance = 0.0

# 宿泊モデル
class Lodging(db.Model):
    __tablename__ = 'lodgings'
    id = db.Column(db.Integer, primary_key=True)
    type = db.Column(db.String)     # 営業種別
    name = db.Column(db.String)     # 名称
    address = db.Column(db.String)  # 所在地
    area = db.Column(db.String)     # 検索用エリア（市町村名）
    tel = db.Column(db.String)      # 電話番号
    rooms = db.Column(db.String)    # 部屋数 (CSVによっては文字が入ることもあるためString推奨)
    capacity = db.Column(db.String) # 定員
    lat = db.Column(db.Float)       # 緯度
    lng = db.Column(db.Float)       # 経度
    distance_from_user = 0.0