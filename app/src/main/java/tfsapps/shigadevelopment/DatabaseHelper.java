package tfsapps.shigadevelopment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * SQLiteデータベースヘルパー
 *
 * テーブル一覧:
 *   visited_meshes     - ユーザーが手動で踏んだ5次メッシュID
 *   visited_spots      - チェックイン済みスポットID
 *   game_state         - KVストア（英雄エンディング等のフラグ）
 *   mesh_city_lookup   - [プランB] meshId → cityId 対応表（初回起動時に生成）
 *   city_completion    - [プランB] 市町コンプリート記録（cityId, meshCount）
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "shiga_kaito_rally.db";
    private static final int    DB_VERSION = 2; // プランB追加でバージョンアップ

    private static final String TABLE_MESH         = "visited_meshes";
    private static final String TABLE_SPOT         = "visited_spots";
    private static final String TABLE_STATE        = "game_state";
    private static final String TABLE_MESH_LOOKUP  = "mesh_city_lookup";
    private static final String TABLE_CITY_COMP    = "city_completion";

    private static final String KEY_HERO_ENDING    = "hero_ending";

    private static DatabaseHelper sInstance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 手動踏破メッシュ
        db.execSQL("CREATE TABLE " + TABLE_MESH
                + " (mesh_id INTEGER PRIMARY KEY)");

        // 訪問済みスポット
        db.execSQL("CREATE TABLE " + TABLE_SPOT
                + " (spot_id TEXT PRIMARY KEY)");

        // ゲーム状態KV
        db.execSQL("CREATE TABLE " + TABLE_STATE
                + " (key TEXT PRIMARY KEY, value TEXT NOT NULL)");

        // [プランB] meshId → cityId 対応表
        db.execSQL("CREATE TABLE " + TABLE_MESH_LOOKUP
                + " (mesh_id INTEGER PRIMARY KEY, city_id TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_mcl_city ON "
                + TABLE_MESH_LOOKUP + " (city_id)");

        // [プランB] 市町コンプリート記録
        db.execSQL("CREATE TABLE " + TABLE_CITY_COMP
                + " (city_id TEXT PRIMARY KEY, mesh_count INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // v1→v2: プランBテーブル追加
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESH_LOOKUP
                    + " (mesh_id INTEGER PRIMARY KEY, city_id TEXT NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_mcl_city ON "
                    + TABLE_MESH_LOOKUP + " (city_id)");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CITY_COMP
                    + " (city_id TEXT PRIMARY KEY, mesh_count INTEGER NOT NULL)");
        }
    }

    // =====================================================================
    // 手動踏破メッシュ
    // =====================================================================

    public Set<Long> loadVisitedMeshes() {
        Set<Long> set = new HashSet<>();
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT mesh_id FROM " + TABLE_MESH, null);
        while (c.moveToNext()) set.add(c.getLong(0));
        c.close();
        return set;
    }

    /** 新規追加なら true を返す */
    public boolean addVisitedMesh(long meshId) {
        ContentValues cv = new ContentValues();
        cv.put("mesh_id", meshId);
        long r = getWritableDatabase().insertWithOnConflict(
                TABLE_MESH, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return r != -1;
    }

    public int getVisitedMeshCount() {
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT COUNT(*) FROM " + TABLE_MESH, null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    /**
     * 指定した市町に属するメッシュを visited_meshes から削除する
     * （対策C: 市町コンプリート時に手動メッシュをリセット）
     */
    public void deleteVisitedMeshesForCity(String cityId) {
        getWritableDatabase().execSQL(
            "DELETE FROM " + TABLE_MESH
            + " WHERE mesh_id IN "
            + " (SELECT mesh_id FROM " + TABLE_MESH_LOOKUP
            + "  WHERE city_id = ?)",
            new String[]{cityId});
    }

    // =====================================================================
    // 訪問済みスポット
    // =====================================================================

    public Set<String> loadVisitedSpots() {
        Set<String> set = new HashSet<>();
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT spot_id FROM " + TABLE_SPOT, null);
        while (c.moveToNext()) set.add(c.getString(0));
        c.close();
        return set;
    }

    public boolean addVisitedSpot(String spotId) {
        ContentValues cv = new ContentValues();
        cv.put("spot_id", spotId);
        long r = getWritableDatabase().insertWithOnConflict(
                TABLE_SPOT, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return r != -1;
    }

    // >>>test_make>>>
    /** スポットIDの達成フラグを削除（デバッグ用リセット） */
    public void removeVisitedSpot(String spotId) {
        getWritableDatabase().delete(TABLE_SPOT, "spot_id=?", new String[]{spotId});
    }
    // <<<test_make<<<

    // =====================================================================
    // ゲーム状態KV
    // =====================================================================

    public boolean isHeroEndingAchieved() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT value FROM " + TABLE_STATE + " WHERE key=?",
                new String[]{KEY_HERO_ENDING});
        boolean ok = false;
        if (c.moveToFirst()) ok = "1".equals(c.getString(0));
        c.close();
        return ok;
    }

    public void setHeroEndingAchieved() {
        ContentValues cv = new ContentValues();
        cv.put("key",   KEY_HERO_ENDING);
        cv.put("value", "1");
        getWritableDatabase().insertWithOnConflict(
                TABLE_STATE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // =====================================================================
    // [プランB] mesh_city_lookup テーブル
    // =====================================================================

    /**
     * 指定メッシュIDが滋賀県内（mesh_city_lookupに存在する）か確認
     * 滋賀県外の移動をカウント除外するために使用
     */
    public boolean isMeshInShiga(long meshId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM " + TABLE_MESH_LOOKUP + " WHERE mesh_id=? LIMIT 1",
                new String[]{String.valueOf(meshId)});
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    /** ルックアップが空（未生成）か確認 */
    public boolean isMeshLookupEmpty() {
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT COUNT(*) FROM " + TABLE_MESH_LOOKUP, null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n == 0;
    }

    /** 指定した市町に属する全メッシュIDを返す（霧解放用） */
    public Set<Long> getMeshIdsForCity(String cityId) {
        Set<Long> set = new HashSet<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT mesh_id FROM " + TABLE_MESH_LOOKUP + " WHERE city_id=?",
                new String[]{cityId});
        while (c.moveToNext()) set.add(c.getLong(0));
        c.close();
        return set;
    }

    /** 滋賀全体のメッシュ総数 */
    public int getTotalShigaMeshCount() {
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT COUNT(*) FROM " + TABLE_MESH_LOOKUP, null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    /** 市町ごとのメッシュ数 */
    public int getMeshCountForCity(String cityId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_MESH_LOOKUP + " WHERE city_id=?",
                new String[]{cityId});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    // =====================================================================
    // [プランB] city_completion テーブル
    // =====================================================================

    /** 市町コンプリートを記録（meshCount = その市町の全メッシュ数） */
    public void markCityCompleted(String cityId, int meshCount) {
        ContentValues cv = new ContentValues();
        cv.put("city_id",    cityId);
        cv.put("mesh_count", meshCount);
        getWritableDatabase().insertWithOnConflict(
                TABLE_CITY_COMP, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** コンプリート済み市町の累計メッシュ数（探索率計算に使用） */
    public int getCompletedCityTotalMeshCount() {
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT SUM(mesh_count) FROM " + TABLE_CITY_COMP, null);
        int n = 0;
        if (c.moveToFirst() && !c.isNull(0)) n = c.getInt(0);
        c.close();
        return n;
    }

    /** コンプリート済み市町IDの集合 */
    public Set<String> loadCompletedCityIds() {
        Set<String> set = new HashSet<>();
        Cursor c = getReadableDatabase()
                .rawQuery("SELECT city_id FROM " + TABLE_CITY_COMP, null);
        while (c.moveToNext()) set.add(c.getString(0));
        c.close();
        return set;
    }

    // =====================================================================
    // デバッグ: 全データリセット
    // =====================================================================

    public void resetAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_MESH);
        db.execSQL("DELETE FROM " + TABLE_SPOT);
        db.execSQL("DELETE FROM " + TABLE_STATE);
        db.execSQL("DELETE FROM " + TABLE_CITY_COMP);
        // mesh_city_lookup はルックアップデータなので削除しない
    }
}
