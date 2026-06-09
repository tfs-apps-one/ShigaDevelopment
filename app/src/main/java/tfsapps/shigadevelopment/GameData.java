package tfsapps.shigadevelopment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 滋賀県全19市町・76スポットのゲームデータ定義
 * シングルトンで保持し、DatabaseHelper と組み合わせて状態を管理
 */
public class GameData {

    private static GameData sInstance;
    public final List<CityInfo> cities;

    public static GameData getInstance() {
        if (sInstance == null) {
            sInstance = new GameData();
        }
        return sInstance;
    }

    private GameData() {
        cities = buildCities();
    }

    // -----------------------------------------------------------------------
    // 全19市町データ定義（各市町4スポット = 計76箇所）
    // -----------------------------------------------------------------------
    private List<CityInfo> buildCities() {
        List<CityInfo> list = new ArrayList<>();

        // 1. 大津市
        list.add(new CityInfo("otsu", "大津市", spots("otsu",
                spot("otsu_1", "比叡山延暦寺",   35.0705, 135.8409),
                spot("otsu_2", "三井寺（園城寺）", 35.0097, 135.8573),
                spot("otsu_3", "石山寺",          34.9604, 135.9057),
                spot("otsu_4", "近江神宮",         35.0324, 135.8512))));

        // 2. 彦根市
        list.add(new CityInfo("hikone", "彦根市", spots("hikone",
                spot("hikone_1", "彦根城",              35.2760, 136.2513),
                spot("hikone_2", "玄宮園",              35.2764, 136.2527),
                spot("hikone_3", "夢京橋キャッスルロード", 35.2741, 136.2583),
                spot("hikone_4", "佐和山城址",           35.2719, 136.2358))));

        // 3. 長浜市
        list.add(new CityInfo("nagahama", "長浜市", spots("nagahama",
                spot("nagahama_1", "黒壁スクエア",   35.3807, 136.2671),
                spot("nagahama_2", "長浜城",         35.3775, 136.2611),
                spot("nagahama_3", "竹生島",         35.4233, 136.1439),
                spot("nagahama_4", "小谷城址",       35.4594, 136.2771))));

        // 4. 近江八幡市
        list.add(new CityInfo("omihachiman", "近江八幡市", spots("omihachiman",
                spot("omihachiman_1", "八幡堀",          35.1218, 136.0986),
                spot("omihachiman_2", "安土城跡",         35.1474, 136.1264),
                spot("omihachiman_3", "ヴォーリズ記念館",  35.1197, 136.0911),
                spot("omihachiman_4", "水郷めぐり乗り場", 35.1261, 136.1003))));

        // 5. 草津市（フェーズ1モックアップ市町）
        list.add(new CityInfo("kusatsu", "草津市", spots("kusatsu",
                spot("kusatsu_1", "草津宿本陣",     35.0167, 135.9597),
                spot("kusatsu_2", "烏丸半島",       35.0733, 135.9407),
                spot("kusatsu_3", "草津川跡地公園", 35.0219, 135.9516),
                spot("kusatsu_4", "立木神社",       35.0024, 135.9651))));

        // 6. 守山市
        list.add(new CityInfo("moriyama", "守山市", spots("moriyama",
                spot("moriyama_1", "なぎさ公園（菜の花畑）", 35.0633, 135.9436),
                spot("moriyama_2", "守山宿本陣跡",           35.0631, 135.9853),
                spot("moriyama_3", "浮気神社",               35.0936, 135.9944),
                spot("moriyama_4", "中洲神社",               35.0831, 136.0000))));

        // 7. 栗東市
        list.add(new CityInfo("ritto", "栗東市", spots("ritto",
                spot("ritto_1", "金勝寺",            34.9980, 136.0278),
                spot("ritto_2", "栗東歴史民俗博物館", 34.9967, 135.9990),
                spot("ritto_3", "大野神社",           34.9931, 135.9847),
                spot("ritto_4", "金勝山",             35.0069, 136.0497))));

        // 8. 甲賀市
        list.add(new CityInfo("koka", "甲賀市", spots("koka",
                spot("koka_1", "甲賀流忍者屋敷", 34.8869, 136.1672),
                spot("koka_2", "信楽陶芸の森",   34.8611, 136.0100),
                spot("koka_3", "油日神社",       34.9006, 136.1097),
                spot("koka_4", "水口城址",       34.9556, 136.1644))));

        // 9. 野洲市
        list.add(new CityInfo("yasu", "野洲市", spots("yasu",
                spot("yasu_1", "銅鐸博物館（弥生の森）", 35.0831, 136.0111),
                spot("yasu_2", "三上山（近江富士）",     35.0725, 136.0369),
                spot("yasu_3", "兵主大社",              35.0847, 136.0128),
                spot("yasu_4", "野洲川歴史公園",         35.0613, 136.0128))));

        // 10. 湖南市
        list.add(new CityInfo("konan", "湖南市", spots("konan",
                spot("konan_1", "三大神社",   34.9844, 136.0139),
                spot("konan_2", "常楽寺",     34.9789, 135.9994),
                spot("konan_3", "長寿寺",     34.9800, 135.9983),
                spot("konan_4", "阿星山登山口", 34.9600, 136.0139))));

        // 11. 高島市
        list.add(new CityInfo("takashima", "高島市", spots("takashima",
                spot("takashima_1", "メタセコイア並木", 35.4720, 136.0363),
                spot("takashima_2", "マキノ高原",       35.4962, 136.0335),
                spot("takashima_3", "白鬚神社",         35.2745, 136.0111),
                spot("takashima_4", "今津浜",           35.4132, 136.0458))));

        // 12. 東近江市
        list.add(new CityInfo("higashiomi", "東近江市", spots("higashiomi",
                spot("higashiomi_1", "百済寺",          35.1217, 136.2153),
                spot("higashiomi_2", "永源寺",          35.1011, 136.2719),
                spot("higashiomi_3", "五個荘近江商人屋敷", 35.1361, 136.1361),
                spot("higashiomi_4", "太郎坊宮",        35.0906, 136.1769))));

        // 13. 米原市
        list.add(new CityInfo("maibara", "米原市", spots("maibara",
                spot("maibara_1", "醒ヶ井宿（地蔵川）", 35.3300, 136.2992),
                spot("maibara_2", "三島池",             35.3258, 136.3497),
                spot("maibara_3", "伊吹山",             35.4217, 136.4036),
                spot("maibara_4", "柏原宿歴史館",        35.3472, 136.3594))));

        // 14. 日野町
        list.add(new CityInfo("hino", "日野町", spots("hino",
                spot("hino_1", "日野商人屋敷",  35.0039, 136.2492),
                spot("hino_2", "中野城跡",      35.0006, 136.2492),
                spot("hino_3", "馬見岡綿向神社", 35.0183, 136.2717),
                spot("hino_4", "正明寺",         34.9831, 136.2511))));

        // 15. 竜王町
        list.add(new CityInfo("ryuo", "竜王町", spots("ryuo",
                spot("ryuo_1", "竜王かがみの里", 35.0267, 136.1114),
                spot("ryuo_2", "雪野山古墳",     35.0050, 136.1475),
                spot("ryuo_3", "鏡神社",         35.0272, 136.1108),
                spot("ryuo_4", "アウトレット竜王", 35.0131, 136.1311))));

        // 16. 愛荘町
        list.add(new CityInfo("aisho", "愛荘町", spots("aisho",
                spot("aisho_1", "金剛輪寺",        35.1672, 136.2153),
                spot("aisho_2", "愛知川桜並木",     35.1708, 136.2242),
                spot("aisho_3", "愛荘町歴史文化博物館", 35.1711, 136.2248),
                spot("aisho_4", "躊躇池",          35.1800, 136.2150))));

        // 17. 豊郷町
        list.add(new CityInfo("toyosato", "豊郷町", spots("toyosato",
                spot("toyosato_1", "豊郷小学校旧校舎群", 35.2031, 136.2233),
                spot("toyosato_2", "鎌田神社",          35.2050, 136.2167),
                spot("toyosato_3", "豊郷ふるさとの森",  35.2072, 136.2200),
                spot("toyosato_4", "旧伊藤忠兵衛邸",   35.2006, 136.2172))));

        // 18. 甲良町
        list.add(new CityInfo("kora", "甲良町", spots("kora",
                spot("kora_1", "西明寺",     35.1436, 136.2503),
                spot("kora_2", "甲良神社",   35.1617, 136.2450),
                spot("kora_3", "甲良コミュニティ公園", 35.1650, 136.2450),
                spot("kora_4", "愛知川河川公園",       35.1650, 136.2350))));

        // 19. 多賀町
        list.add(new CityInfo("taga", "多賀町", spots("taga",
                spot("taga_1", "多賀大社",   35.2194, 136.2822),
                spot("taga_2", "胡宮神社",   35.2075, 136.2736),
                spot("taga_3", "犬上川渓谷", 35.2400, 136.2956),
                spot("taga_4", "多賀の大杉", 35.2300, 136.3100))));

        return list;
    }

    // ---- ユーティリティ ------------------------------------------------

    private static SpotInfo spot(String id, String name, double lat, double lon) {
        // cityId は後でセットするためダミー渡し
        return new SpotInfo(id, name, lat, lon, "");
    }

    private static List<SpotInfo> spots(String cityId, SpotInfo... arr) {
        List<SpotInfo> list = new ArrayList<>();
        for (SpotInfo s : arr) {
            // cityId をセット
            list.add(new SpotInfo(s.id, s.name, s.lat, s.lon, cityId));
        }
        return list;
    }

    // ---- 検索ヘルパー --------------------------------------------------

    /** スポットIDからSpotInfoを返す */
    public SpotInfo findSpotById(String spotId) {
        for (CityInfo c : cities) {
            for (SpotInfo s : c.spots) {
                if (s.id.equals(spotId)) return s;
            }
        }
        return null;
    }

    /** 市町IDからCityInfoを返す */
    public CityInfo findCityById(String cityId) {
        for (CityInfo c : cities) {
            if (c.id.equals(cityId)) return c;
        }
        return null;
    }

    /** 座標から最も近い市町を返す（簡易：スポット重心で判定） */
    public CityInfo findNearestCity(double lat, double lon) {
        CityInfo nearest = null;
        float minDist = Float.MAX_VALUE;
        float[] result = new float[1];
        for (CityInfo city : cities) {
            // 各市町のスポット重心
            double cLat = 0, cLon = 0;
            for (SpotInfo s : city.spots) { cLat += s.lat; cLon += s.lon; }
            cLat /= city.spots.size();
            cLon /= city.spots.size();
            android.location.Location.distanceBetween(lat, lon, cLat, cLon, result);
            if (result[0] < minDist) {
                minDist = result[0];
                nearest = city;
            }
        }
        return nearest;
    }

    /** 全市町が100%達成済みか（グランドエンディング条件） */
    public boolean isAllCitiesCompleted() {
        for (CityInfo c : cities) {
            if (!c.isCompleted) return false;
        }
        return true;
    }

    /** シングルトンをリセット（デバッグ用） */
    public static void resetInstance() {
        sInstance = null;
    }

    /** DBの訪問済みデータをゲームデータに反映する */
    public void restoreFromDb(java.util.Set<String> visitedSpotIds) {
        for (CityInfo city : cities) {
            for (SpotInfo spot : city.spots) {
                if (visitedSpotIds.contains(spot.id)) {
                    spot.isVisited = true;
                }
            }
            city.checkAndUpdateCompletion();
        }
    }
}
