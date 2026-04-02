from flask import Flask, render_template, request, session, redirect, url_for, flash
from sqlalchemy import func
from models import db, TouristSpot, ServiceCounter, GourmetSpot
from models import db, TouristSpot, ServiceCounter, GourmetSpot, Lodging
import math
import os

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///smart_travel.db'
app.secret_key = os.urandom(24)
db.init_app(app)

# 除外するエリアのリスト
EXCLUDED_AREAS = [
    '東京都', '大阪府大阪市', '石川県加賀市'
]

# 距離計算
def calculate_distance(lat1, lng1, lat2, lng2):
    if lat1 == 0 or lat2 == 0: return 9999
    r = 6371
    p1 = math.radians(lat1)
    p2 = math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lng2 - lng1)
    a = math.sin(dp/2) * math.sin(dp/2) + \
        math.cos(p1) * math.cos(p2) * \
        math.sin(dl/2) * math.sin(dl/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return r * c

def find_nearby_counters(lat, lng):
    all_counters = ServiceCounter.query.all()
    nearby = []
    for c in all_counters:
        dist = calculate_distance(lat, lng, c.lat, c.lng)
        c.distance = round(dist, 2)
        nearby.append(c)
    nearby.sort(key=lambda x: x.distance)
    return nearby[:5]

def find_nearby_spots(lat, lng, limit=3):
    all_spots = TouristSpot.query.all()
    nearby = []
    for s in all_spots:
        dist = calculate_distance(lat, lng, s.lat, s.lng)
        s.distance = round(dist, 2)
        nearby.append(s)
    nearby.sort(key=lambda x: x.distance)
    return nearby[:limit]

@app.route('/')
def index():
    search_type = request.args.get('type', 'spot')
    keyword = request.args.get('keyword', '').strip()
    area = request.args.get('area', '').strip()
    page = request.args.get('page', 1, type=int)
    per_page = 24 # ページネーションあり（24件）
    
    user_lat = request.args.get('lat', type=float)
    user_lng = request.args.get('lng', type=float)

    # クエリ作成とエリアリスト作成
    if search_type == 'gourmet':
        TargetModel = GourmetSpot
    elif search_type == 'lodging':
        TargetModel = Lodging
    else:
        TargetModel = TouristSpot

    query = db.session.query(TargetModel)
    
    # エリアリストの作成（県外除外、件数>1）
    area_list_query = db.session.query(TargetModel.area)\
        .filter(TargetModel.area.notin_(EXCLUDED_AREAS))\
        .group_by(TargetModel.area)\
        .having(func.count(TargetModel.id) > 1)

    # 検索条件の適用
    if keyword:
        query = query.filter(TargetModel.name.like(f'%{keyword}%'))
            
    if area and area != "すべて":
        query = query.filter(TargetModel.area == area)

    # 全件取得して距離計算・ソート（ページネーションのため）
    results = query.all()
    
    if user_lat and user_lng:
        for item in results:
            dist = calculate_distance(user_lat, user_lng, item.lat, item.lng)
            item.distance_from_user = round(dist, 2)
        results.sort(key=lambda x: x.distance_from_user)

    # ページネーション処理
    total_count = len(results)
    max_page = math.ceil(total_count / per_page)
    if page < 1: page = 1
    if page > max_page and max_page > 0: page = max_page
    
    start = (page - 1) * per_page
    end = start + per_page
    paginated_results = results[start:end]
    
    # エリアリスト
    areas = [r[0] for r in area_list_query if r[0]]
    areas.sort()
    
    return render_template('index.html', 
                           results=paginated_results, 
                           areas=areas, 
                           search_type=search_type,
                           current_page=page,
                           max_page=max_page,
                           total_count=total_count,
                           keyword=keyword,
                           area=area,
                           user_lat=user_lat,
                           user_lng=user_lng)

@app.route('/spot/<int:spot_id>')
def spot_detail(spot_id):
    spot = db.session.get(TouristSpot, spot_id)
    counters = find_nearby_counters(spot.lat, spot.lng)
    user_lat = request.args.get('lat', type=float)
    user_lng = request.args.get('lng', type=float)
    return render_template('detail.html', item=spot, counters=counters, item_type='spot', user_lat=user_lat, user_lng=user_lng)

@app.route('/gourmet/<int:gourmet_id>')
def gourmet_detail(gourmet_id):
    gourmet = db.session.get(GourmetSpot, gourmet_id)
    nearby_spots = find_nearby_spots(gourmet.lat, gourmet.lng, limit=3)
    user_lat = request.args.get('lat', type=float)
    user_lng = request.args.get('lng', type=float)
    return render_template('detail_gourmet.html', item=gourmet, nearby_spots=nearby_spots, user_lat=user_lat, user_lng=user_lng)

# 宿泊詳細
@app.route('/lodging/<int:lodging_id>')
def lodging_detail(lodging_id):
    lodging = db.session.get(Lodging, lodging_id)
    # 近くの観光地も表示すると親切
    nearby_spots = find_nearby_spots(lodging.lat, lodging.lng, limit=3)
    user_lat = request.args.get('lat', type=float)
    user_lng = request.args.get('lng', type=float)
    return render_template('detail_lodging.html', item=lodging, nearby_spots=nearby_spots, user_lat=user_lat, user_lng=user_lng)

@app.route('/add_favorite/<item_type>/<int:item_id>')
def add_favorite(item_type, item_id):
    cart = session.get('my_itinerary', [])
    exists = False
    for item in cart:
        if item['type'] == item_type and item['id'] == item_id:
            exists = True
            break
    if not exists:
        cart.append({'type': item_type, 'id': item_id})
        session['my_itinerary'] = cart
        flash('しおりに追加しました！', 'success')
    else:
        flash('すでに追加されています', 'info')
    return redirect(request.referrer or url_for('index'))

@app.route('/remove_favorite/<item_type>/<int:item_id>')
def remove_favorite(item_type, item_id):
    cart = session.get('my_itinerary', [])
    cart = [item for item in cart if not (item['type'] == item_type and item['id'] == item_id)]
    session['my_itinerary'] = cart
    flash('しおりから削除しました', 'warning')
    return redirect(url_for('itinerary'))

@app.route('/itinerary')
def itinerary():
    cart = session.get('my_itinerary', [])
    spot_list = []
    gourmet_list = []
    lodging_list = []
    for item in cart:
        if item['type'] == 'spot':
            obj = db.session.get(TouristSpot, item['id'])
            if obj: spot_list.append(obj)
        elif item['type'] == 'gourmet':
            obj = db.session.get(GourmetSpot, item['id'])
            if obj: gourmet_list.append(obj)
        elif item['type'] == 'lodging':
            obj = db.session.get(Lodging, item['id'])
            if obj: lodging_list.append(obj)

    return render_template('itinerary.html', spots=spot_list, gourmets=gourmet_list, lodgings=lodging_list)

@app.route('/map')
def show_map():
    all_spots = TouristSpot.query.all()
    all_gourmets = GourmetSpot.query.all()
    all_lodgings = Lodging.query.all()
    return render_template('map.html', spots=all_spots, gourmets=all_gourmets, lodgings=all_lodgings)

if __name__ == '__main__':
    app.run(debug=True)