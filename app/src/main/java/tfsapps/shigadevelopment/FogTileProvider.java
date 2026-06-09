package tfsapps.shigadevelopment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Google Maps TileOverlay 用の霧エフェクトプロバイダー（3状態対応版）
 *
 * 霧の3状態:
 *   未探索               → 霧（暗い青紺色）
 *   自分の足で踏んだマス → 薄い緑のマス（selfWalkedMeshes）
 *   市町コンプリート     → 完全クリア（地図の色）（completedCityMeshes）
 *
 * 描画順序:
 *   1. 全面に霧を塗る
 *   2. selfWalkedMeshes → 薄い緑でオーバーレイ
 *   3. completedCityMeshes → CLEAR（地図そのまま）
 *
 * 霧は全タイルに適用（滋賀の矩形境界が見えてしまう問題を解消）。
 * 霧が解除されるのは selfWalkedMeshes / completedCityMeshes のメッシュだけで、
 * これらは滋賀県内でのみ記録されるため探索自体は引き続き滋賀限定となる。
 */
public class FogTileProvider implements TileProvider {

    private static final int TILE_SIZE = 256;
    private static final int FOG_COLOR = Color.argb(180, 0, 0, 30);

    // 自分の足で踏んだマスの色（薄い緑・半透明）
    private static final int FOOTPRINT_COLOR = Color.argb(120, 80, 200, 80);

    // 自分の足で踏んだメッシュ（緑表示）
    private volatile Set<Long> selfWalkedMeshes;

    // コンプリート済み市町の全メッシュIDセット（完全クリア表示）
    private volatile Set<Long> completedCityMeshes;

    private final Paint fogPaint;
    private final Paint footprintPaint;  // 薄い緑
    private final Paint clearPaint;      // 完全クリア（PorterDuff.CLEAR）

    public FogTileProvider(Set<Long> selfWalkedMeshes, Set<Long> completedCityMeshes) {
        this.selfWalkedMeshes    = new HashSet<>(selfWalkedMeshes);
        this.completedCityMeshes = new HashSet<>(completedCityMeshes);

        fogPaint = new Paint();
        fogPaint.setColor(FOG_COLOR);
        fogPaint.setStyle(Paint.Style.FILL);

        footprintPaint = new Paint();
        footprintPaint.setColor(FOOTPRINT_COLOR);
        footprintPaint.setStyle(Paint.Style.FILL);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    // -----------------------------------------------------------------
    // 公開更新API
    // -----------------------------------------------------------------

    /** 自分の足跡メッシュを更新（位置情報踏破時に呼ぶ） */
    public synchronized void updateSelfWalkedMeshes(Set<Long> meshes) {
        this.selfWalkedMeshes = new HashSet<>(meshes);
    }

    /** 市町コンプリート時にそのメッシュIDセットをまるごと追加 */
    public synchronized void addCompletedCityMeshes(Set<Long> cityMeshIds) {
        Set<Long> merged = new HashSet<>(this.completedCityMeshes);
        merged.addAll(cityMeshIds);
        this.completedCityMeshes = merged;
    }

    /** リセット時に呼び出す */
    public synchronized void resetCompletedCityMeshes() {
        this.completedCityMeshes = new HashSet<>();
    }

    // -----------------------------------------------------------------
    // TileProvider 本体
    // -----------------------------------------------------------------

    @Override
    public Tile getTile(int tileX, int tileY, int zoom) {
        // 全タイルに霧を適用（滋賀の矩形境界を見えなくするため）
        LatLngBounds tileBounds = tileXYZToBounds(tileX, tileY, zoom);

        Bitmap bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(true);
        Canvas canvas = new Canvas(bitmap);

        // ステップ1: 全面に霧を塗る
        canvas.drawRect(0, 0, TILE_SIZE, TILE_SIZE, fogPaint);

        // ステップ2: コンプリート済み市町のメッシュを完全クリア（地図の色を見せる）
        for (Long meshId : completedCityMeshes) {
            LatLngBounds mb = MeshCalculator.getMeshBounds(meshId);
            if (intersects(tileBounds, mb)) {
                drawColorRect(canvas, mb, tileX, tileY, zoom, clearPaint);
            }
        }

        // ステップ3: 自分の足跡メッシュを薄い緑で描画（クリアの上から上書き → 常に表示）
        for (Long meshId : selfWalkedMeshes) {
            LatLngBounds mb = MeshCalculator.getMeshBounds(meshId);
            if (intersects(tileBounds, mb)) {
                drawColorRect(canvas, mb, tileX, tileY, zoom, footprintPaint);
            }
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return new Tile(TILE_SIZE, TILE_SIZE, stream.toByteArray());
    }

    // -----------------------------------------------------------------
    // 座標変換ユーティリティ
    // -----------------------------------------------------------------

    /**
     * 緯度・経度 → Webメルカトル正規化座標 (0.0〜1.0) を返す。
     * x: 西端(lon=-180)=0.0, 東端(lon=+180)=1.0
     * y: 北端(lat≒+85.05)=0.0, 南端(lat≒-85.05)=1.0
     */
    private static double[] latLonToNormalized(double lat, double lon) {
        double x = (lon + 180.0) / 360.0;
        double sinLat = Math.sin(Math.toRadians(lat));
        sinLat = Math.max(-0.9999, Math.min(0.9999, sinLat));
        double y = 0.5 - Math.log((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI);
        return new double[]{x, y};
    }

    /**
     * 正規化座標 → タイル内ピクセル座標に変換する。
     */
    private static float[] normalizedToTilePixel(double[] norm, int tileX, int tileY, int zoom) {
        double scale = Math.pow(2, zoom) * TILE_SIZE;
        float px = (float)(norm[0] * scale - tileX * TILE_SIZE);
        float py = (float)(norm[1] * scale - tileY * TILE_SIZE);
        return new float[]{px, py};
    }

    private static LatLngBounds tileXYZToBounds(int tileX, int tileY, int zoom) {
        double n        = Math.pow(2, zoom);
        double lonWest  = tileX / n * 360.0 - 180.0;
        double lonEast  = (tileX + 1) / n * 360.0 - 180.0;
        double latNorth = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2.0 * tileY / n))));
        double latSouth = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2.0 * (tileY + 1) / n))));
        return new LatLngBounds(new LatLng(latSouth, lonWest), new LatLng(latNorth, lonEast));
    }

    private static boolean intersects(LatLngBounds a, LatLngBounds b) {
        return a.southwest.latitude  <= b.northeast.latitude  &&
               a.northeast.latitude  >= b.southwest.latitude  &&
               a.southwest.longitude <= b.northeast.longitude &&
               a.northeast.longitude >= b.southwest.longitude;
    }

    /**
     * 指定エリアを指定Paintで描画する汎用メソッド。
     * clearPaint（CLEAR）でも footprintPaint（緑）でも使える。
     */
    private void drawColorRect(Canvas canvas, LatLngBounds area,
                               int tileX, int tileY, int zoom, Paint paint) {
        double[] swNorm = latLonToNormalized(area.southwest.latitude,  area.southwest.longitude);
        double[] neNorm = latLonToNormalized(area.northeast.latitude,  area.northeast.longitude);
        float[] sw = normalizedToTilePixel(swNorm, tileX, tileY, zoom);
        float[] ne = normalizedToTilePixel(neNorm, tileX, tileY, zoom);
        float left   = Math.min(sw[0], ne[0]);
        float top    = Math.min(sw[1], ne[1]);
        float right  = Math.max(sw[0], ne[0]);
        float bottom = Math.max(sw[1], ne[1]);
        canvas.drawRect(left, top, right, bottom, paint);
    }
}
