package tfsapps.shigadevelopment;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 5次メッシュID ↔ 市町ID の対応表を管理するクラス
 *
 * 【初回起動時】
 *   generateLookupTable() でバックグラウンドスレッドにて全メッシュを走査。
 *   結果を SQLite (DatabaseHelper) に保存し、SharedPreferences にフラグを立てる。
 *   処理時間目安: 約2〜4秒
 *
 * 【2回目以降の起動】
 *   loadFromDb() で SQLite から HashMap に一括ロード（< 1秒）。
 *
 * 【メッシュ対応表の精度】
 *   境界付近 ≈85%、内部 ≈98%（CityBoundary の近似ポリゴンに依存）
 */
public class MeshLookupTable {

    private static final String PREF_NAME        = "mesh_lookup_prefs";
    private static final String PREF_GENERATED   = "lookup_generated_v1";

    // 5次メッシュの緯度・経度幅
    private static final double LAT_5TH = 2.0 / (3.0 * 320); // ≈ 0.002083°
    private static final double LON_5TH = 1.0 / 320;          // ≈ 0.003125°

    // 市町ID → そのメッシュ総数（ルックアップ完了後に確定）
    private final Map<String, Integer> cityMeshCounts = new HashMap<>();

    // 全滋賀メッシュ数合計（ルックアップ完了後に確定）
    private int totalShigaMeshCount = 0;

    // ロード済みフラグ
    private volatile boolean isLoaded = false;

    // シングルトン
    private static MeshLookupTable sInstance;
    public static synchronized MeshLookupTable getInstance() {
        if (sInstance == null) sInstance = new MeshLookupTable();
        return sInstance;
    }
    private MeshLookupTable() {}

    // -----------------------------------------------------------------
    // ルックアップ生成済みか？
    // -----------------------------------------------------------------
    public static boolean isAlreadyGenerated(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                  .getBoolean(PREF_GENERATED, false);
    }

    // -----------------------------------------------------------------
    // バックグラウンドでルックアップテーブルを生成 → SQLiteに保存
    // onDone は完了時に UI スレッドで呼ばれる
    // -----------------------------------------------------------------
    public void generateAsync(Context ctx, DatabaseHelper db, Runnable onDone) {
        new Thread(() -> {
            generate(db);
            // 生成済みフラグを保存
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
               .edit().putBoolean(PREF_GENERATED, true).apply();
            // ロード
            loadFromDb(db);
            new Handler(Looper.getMainLooper()).post(onDone);
        }).start();
    }

    // -----------------------------------------------------------------
    // SQLite からメモリに一括ロード（2回目以降の起動）
    // -----------------------------------------------------------------
    public void loadFromDb(DatabaseHelper db) {
        cityMeshCounts.clear();
        totalShigaMeshCount = 0;

        SQLiteDatabase rdb = db.getReadableDatabase();
        Cursor c = rdb.rawQuery(
            "SELECT city_id, COUNT(*) FROM mesh_city_lookup GROUP BY city_id", null);
        while (c.moveToNext()) {
            String cityId = c.getString(0);
            int    count  = c.getInt(1);
            cityMeshCounts.put(cityId, count);
            totalShigaMeshCount += count;
        }
        c.close();
        isLoaded = true;
    }

    // -----------------------------------------------------------------
    // 対応する市町IDを返す（ランタイム使用 — 個別メッシュには不要）
    // -----------------------------------------------------------------
    public String getCityId(long meshId, DatabaseHelper db) {
        SQLiteDatabase rdb = db.getReadableDatabase();
        Cursor c = rdb.rawQuery(
            "SELECT city_id FROM mesh_city_lookup WHERE mesh_id=?",
            new String[]{String.valueOf(meshId)});
        String cityId = null;
        if (c.moveToFirst()) cityId = c.getString(0);
        c.close();
        return cityId;
    }

    // -----------------------------------------------------------------
    // 指定市町に属する全メッシュIDを Set で返す
    // （市町コンプリート時に霧を一気晴らすために使用）
    // -----------------------------------------------------------------
    public Set<Long> getMeshIdsForCity(String cityId, DatabaseHelper db) {
        Set<Long> set = new HashSet<>();
        SQLiteDatabase rdb = db.getReadableDatabase();
        Cursor c = rdb.rawQuery(
            "SELECT mesh_id FROM mesh_city_lookup WHERE city_id=?",
            new String[]{cityId});
        while (c.moveToNext()) {
            set.add(c.getLong(0));
        }
        c.close();
        return set;
    }

    // -----------------------------------------------------------------
    // 滋賀全体のメッシュ総数
    // -----------------------------------------------------------------
    public int getTotalShigaMeshCount() {
        return totalShigaMeshCount;
    }

    // -----------------------------------------------------------------
    // 市町ごとのメッシュ数
    // -----------------------------------------------------------------
    public int getMeshCountForCity(String cityId) {
        Integer n = cityMeshCounts.get(cityId);
        return n != null ? n : 0;
    }

    public boolean isLoaded() { return isLoaded; }

    // -----------------------------------------------------------------
    // 内部: 全メッシュを走査して SQLite に保存
    // -----------------------------------------------------------------
    private void generate(DatabaseHelper db) {
        List<CityBoundary> boundaries = CityBoundary.getAllBoundaries();
        SQLiteDatabase wdb = db.getWritableDatabase();

        wdb.beginTransaction();
        try {
            // 既存データを削除してから再生成
            wdb.execSQL("DELETE FROM mesh_city_lookup");

            // 滋賀バウンディングボックス内の全5次メッシュを走査
            // メッシュ中心点で市町判定する
            double lat = CityBoundary.SHIGA_MIN_LAT + LAT_5TH / 2;
            while (lat < CityBoundary.SHIGA_MAX_LAT) {
                double lon = CityBoundary.SHIGA_MIN_LON + LON_5TH / 2;
                while (lon < CityBoundary.SHIGA_MAX_LON) {
                    long meshId = MeshCalculator.calcMeshId(lat, lon);
                    String cityId = findCity(lat, lon, boundaries);
                    if (cityId != null) {
                        ContentValues cv = new ContentValues();
                        cv.put("mesh_id", meshId);
                        cv.put("city_id", cityId);
                        wdb.insert("mesh_city_lookup", null, cv);
                    }
                    lon += LON_5TH;
                }
                lat += LAT_5TH;
            }
            wdb.setTransactionSuccessful();
        } finally {
            wdb.endTransaction();
        }
    }

    // -----------------------------------------------------------------
    // 優先順位付きリストで最初にマッチした市町IDを返す
    // -----------------------------------------------------------------
    private static String findCity(double lat, double lon,
                                   List<CityBoundary> boundaries) {
        for (CityBoundary b : boundaries) {
            if (b.contains(lat, lon)) return b.cityId;
        }
        return null; // 琵琶湖または滋賀県外
    }
}
