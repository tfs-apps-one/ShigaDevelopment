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
                spot("otsu_1", "比叡山延暦寺",   35.0705,              135.8409),
                spot("otsu_2", "三井寺（園城寺）", 35.01356066134698,    135.85282881356102),
                spot("otsu_3", "石山寺",          34.9604,              135.9057),
                spot("otsu_4", "近江神宮",         35.0324,              135.8512))));

        // 2. 彦根市
        list.add(new CityInfo("hikone", "彦根市", spots("hikone",
                spot("hikone_1", "彦根城",              35.2760,              136.2513),
                spot("hikone_2", "玄宮園",              35.278117695954705,   136.25392093872216),
                spot("hikone_3", "夢京橋キャッスルロード", 35.27107160027103,    136.25162149824797),
                spot("hikone_4", "佐和山城址",           35.28076443129244,    136.2693573904332))));

        // 3. 長浜市
        list.add(new CityInfo("nagahama", "長浜市", spots("nagahama",
                spot("nagahama_1", "黒壁スクエア",   35.3807,              136.2671),
                spot("nagahama_2", "長浜城",         35.3775,              136.2611),
                spot("nagahama_3", "竹生島",         35.42151641271829,    136.14339574109832),
                spot("nagahama_4", "小谷城址",       35.45942259611682,    136.27707154127077))));

        // 4. 近江八幡市
        list.add(new CityInfo("omihachiman", "近江八幡市", spots("omihachiman",
                spot("omihachiman_1", "八幡堀",          35.14058174695905,    136.09021989262655),
                spot("omihachiman_2", "安土城跡",         35.15602002345294,    136.139553125226),
                spot("omihachiman_3", "ヴォーリズ記念館",  35.1399417282397,     136.09465814057026),
                spot("omihachiman_4", "水郷めぐり乗り場", 35.146278156299374,   136.09364852522577))));

        // 5. 草津市
        list.add(new CityInfo("kusatsu", "草津市", spots("kusatsu",
                spot("kusatsu_1", "草津宿本陣",     35.01789263566647,    135.96017344849628),
                spot("kusatsu_2", "烏丸半島",       35.07356867760595,    135.93626388441047),
                spot("kusatsu_3", "草津川跡地公園", 35.03442484778866,    135.93110221388102),
                spot("kusatsu_4", "立木神社",       35.013603943728086,   135.9563851810394))));

        // 6. 守山市
        list.add(new CityInfo("moriyama", "守山市", spots("moriyama",
                spot("moriyama_1", "なぎさ公園（菜の花畑）", 35.12667554656884,    135.9505316828975),
                spot("moriyama_2", "守山宿本陣跡",           35.05572919843027,    135.9925554675496),
                spot("moriyama_3", "浮気城跡",               35.051910371057026,   136.0022834774078),
                spot("moriyama_4", "中州護国社",              35.10848846449856,    135.98406916989143))));

        // 7. 栗東市
        list.add(new CityInfo("ritto", "栗東市", spots("ritto",
                spot("ritto_1", "金勝寺",            34.960025516409935,   136.0351805042922),
                spot("ritto_2", "栗東歴史民俗博物館", 35.018292290403956,   136.00871679267732),
                spot("ritto_3", "大野神社",           34.994335050366146,   136.02293949261022),
                spot("ritto_4", "金勝山",             34.966154964977484,   136.02091557874547))));

        // 8. 甲賀市
        list.add(new CityInfo("koka", "甲賀市", spots("koka",
                spot("koka_1", "甲賀流忍者屋敷", 34.91855322955671,    136.16739646754428),
                spot("koka_2", "信楽陶芸の森",   34.88588833919503,    136.05894355371748),
                spot("koka_3", "油日神社",       34.88821653414506,    136.24944293201798),
                spot("koka_4", "水口城址",       34.9702194573565,     136.16571760008964))));

        // 9. 野洲市
        list.add(new CityInfo("yasu", "野洲市", spots("yasu",
                spot("yasu_1", "銅鐸博物館（弥生の森）", 35.0728052066683,     136.04485559638658),
                spot("yasu_2", "三上山（近江富士）",     35.050199426230215,   136.03811545706242),
                spot("yasu_3", "兵主大社",              35.1154722478212,     136.0102702387158),
                spot("yasu_4", "野洲川歴史公園",         35.10971172450375,    135.99260777238527))));

        // 10. 湖南市
        list.add(new CityInfo("konan", "湖南市", spots("konan",
                spot("konan_1", "石部宿場の里", 35.0046313061762,     136.04520238850156),
                spot("konan_2", "常楽寺",       34.99220753044666,    136.04848391697638),
                spot("konan_3", "長寿寺",       34.98741205055783,    136.05998518341363),
                spot("konan_4", "阿星山登山口", 34.96453608790252,    136.06109874164548))));

        // 11. 高島市
        list.add(new CityInfo("takashima", "高島市", spots("takashima",
                spot("takashima_1", "メタセコイア並木", 35.48832824801025,    136.03732217216395),
                spot("takashima_2", "マキノ高原",       35.496904721058385,   136.03353778493388),
                spot("takashima_3", "白鬚神社",         35.27476225534235,    136.0110802083254),
                spot("takashima_4", "今津浜",           35.42137117353879,    136.0469193926266))));

        // 12. 東近江市
        list.add(new CityInfo("higashiomi", "東近江市", spots("higashiomi",
                spot("higashiomi_1", "百済寺",          35.12690304615221,    136.28898432522485),
                spot("higashiomi_2", "永源寺",          35.080620418543425,   136.31989711764334),
                spot("higashiomi_3", "五個荘近江商人屋敷", 35.15481992896524,    136.1799285578118),
                spot("higashiomi_4", "太郎坊宮",        35.118374975706836,   136.18162145728192))));

        // 13. 米原市
        list.add(new CityInfo("maibara", "米原市", spots("maibara",
                spot("maibara_1", "醒ヶ井宿（地蔵川）", 35.32949038621298,    136.3517367675605),
                spot("maibara_2", "三島池",             35.37482904601558,    136.35816984818237),
                spot("maibara_3", "伊吹山",             35.41839049346785,    136.40660227264252),
                spot("maibara_4", "柏原宿歴史館",        35.34241974376385,    136.39874886756098))));

        // 14. 日野町
        list.add(new CityInfo("hino", "日野町", spots("hino",
                spot("hino_1", "日野商人屋敷",  35.01167793156167,    136.24767972522042),
                spot("hino_2", "中野城跡",      35.00951576669884,    136.2652213176395),
                spot("hino_3", "馬見岡綿向神社", 35.015024116090274,   136.26188251172923),
                spot("hino_4", "正明寺",         35.02581720649701,    136.2455512463133))));

        // 15. 竜王町
        list.add(new CityInfo("ryuo", "竜王町", spots("ryuo",
                spot("ryuo_1", "竜王かがみの里", 35.085697517478785,   136.07890018897302),
                spot("ryuo_2", "雪野山古墳",     35.07658179729394,    136.14579809638673),
                spot("ryuo_3", "鏡神社",         35.10292958598764,    136.07360484197844),
                spot("ryuo_4", "アウトレット竜王", 35.05881482406065,    136.09953383871357))));

        // 16. 愛荘町
        list.add(new CityInfo("aisho", "愛荘町", spots("aisho",
                spot("aisho_1", "金剛輪寺",           35.16296588064098,    136.27930372960432),
                spot("aisho_2", "豊満神社",            35.16786024000902,    136.2181943397354),
                spot("aisho_3", "愛荘町歴史文化博物館", 35.16383672032369,    136.27823779639007),
                spot("aisho_4", "大隴神社",            35.1855843815091,     136.2086370117359))));

        // 17. 豊郷町
        list.add(new CityInfo("toyosato", "豊郷町", spots("toyosato",
                spot("toyosato_1", "豊郷小学校旧校舎群", 35.20347870573395,    136.2328275829003),
                spot("toyosato_2", "阿自岐神社",          35.212235697489824,   136.22771551169978),
                spot("toyosato_3", "岡村本家（蔵しっく館）", 35.18622943738736,  136.23225414057214),
                spot("toyosato_4", "旧伊藤忠兵衛邸",       35.200089676966634,   136.22721013454338))));

        // 18. 甲良町
        list.add(new CityInfo("kora", "甲良町", spots("kora",
                spot("kora_1", "西明寺",               35.189730624151494,   136.28418766170324),
                spot("kora_2", "甲良神社",             35.21341039373086,    136.2540980453788),
                spot("kora_3", "甲良コミュニティ公園", 35.18949092045692,    136.26946172737468),
                spot("kora_4", "道の駅 せせらぎの里こうら", 35.20170117178991, 136.27552784057275))));

        // 19. 多賀町
        list.add(new CityInfo("taga", "多賀町", spots("taga",
                spot("taga_1", "多賀大社",                  35.225297801377295,   136.29102499639257),
                spot("taga_2", "胡宮神社",                  35.21528594943876,    136.28656853871976),
                spot("taga_3", "河内風穴",                  35.250903541701696,   136.35376015406604),
                spot("taga_4", "犬上川ダム（おしどりの里）", 35.17104318972454,    136.33917994216674))));

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
