package tfsapps.shigadevelopment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * 国土地理院 地域メッシュ統計（5次メッシュ = 約250m四方）の計算クラス
 *
 * メッシュコード体系:
 *   1次 (4桁) : 緯度40分×経度60分 ≈ 80km
 *   2次 (6桁) : 1次を8×8分割 ≈ 10km
 *   3次 (8桁) : 2次を10×10分割 ≈ 1km
 *   4次 (9桁) : 3次を2×2分割 ≈ 500m
 *   5次 (10桁): 4次を2×2分割 ≈ 250m
 */
public class MeshCalculator {

    // 1次メッシュの緯度幅 (40分 = 2/3度)
    private static final double LAT_1ST = 2.0 / 3.0;
    // 1次メッシュの経度幅 (60分 = 1度)
    private static final double LON_1ST = 1.0;

    // 各次の緯度・経度幅
    private static final double LAT_2ND = LAT_1ST / 8.0;
    private static final double LON_2ND = LON_1ST / 8.0;
    private static final double LAT_3RD = LAT_2ND / 10.0;
    private static final double LON_3RD = LON_2ND / 10.0;
    private static final double LAT_4TH = LAT_3RD / 2.0;
    private static final double LON_4TH = LON_3RD / 2.0;
    private static final double LAT_5TH = LAT_4TH / 2.0;
    private static final double LON_5TH = LON_4TH / 2.0;

    /**
     * 緯度・経度から5次メッシュIDを計算する
     * @param lat 緯度 (WGS84)
     * @param lon 経度 (WGS84)
     * @return 10桁の5次メッシュコード (long)
     */
    public static long calcMeshId(double lat, double lon) {
        // --- 1次メッシュ ---
        int p = (int)(lat / LAT_1ST);          // 緯度コード (0起算)
        int u = (int)lon;                        // 経度コード (整数部)

        // --- 2次メッシュ (0-7) ---
        double lat_r1 = lat - p * LAT_1ST;
        double lon_r1 = lon - u;
        int q = (int)(lat_r1 / LAT_2ND);
        int v = (int)(lon_r1 / LON_2ND);

        // --- 3次メッシュ (0-9) ---
        double lat_r2 = lat_r1 - q * LAT_2ND;
        double lon_r2 = lon_r1 - v * LON_2ND;
        int r = (int)(lat_r2 / LAT_3RD);
        int w = (int)(lon_r2 / LON_3RD);

        // --- 4次メッシュ (s=0-1, x=0-1) ---
        double lat_r3 = lat_r2 - r * LAT_3RD;
        double lon_r3 = lon_r2 - w * LON_3RD;
        int s = (int)(lat_r3 / LAT_4TH);   // 0=南半, 1=北半
        int x = (int)(lon_r3 / LON_4TH);   // 0=西半, 1=東半
        int m4 = s * 2 + x + 1;             // 1-4

        // --- 5次メッシュ (t=0-1, y=0-1) ---
        double lat_r4 = lat_r3 - s * LAT_4TH;
        double lon_r4 = lon_r3 - x * LON_4TH;
        int t = (int)(lat_r4 / LAT_5TH);   // 0=南半, 1=北半
        int y = (int)(lon_r4 / LON_5TH);   // 0=西半, 1=東半
        int m5 = t * 2 + y + 1;             // 1-4

        // メッシュコードを組み立て:  PPUU QVQV RWRW M4 M5
        // 1次: p*100 + (u-100)  ←  uは100以上なので2桁に収める
        long code1 = (long)p * 100L + (u - 100);
        long code = code1 * 1_000_000L
                  + (long)(q * 10 + v) * 10_000L
                  + (long)(r * 10 + w) * 100L
                  + (long)(m4 * 10)
                  + (long)m5;
        return code;
    }

    /**
     * 5次メッシュIDからそのメッシュの境界LatLngBoundsを返す
     */
    public static LatLngBounds getMeshBounds(long meshId) {
        // メッシュコードを分解
        long tmp = meshId;
        int m5  = (int)(tmp % 10);  tmp /= 10;
        int m4  = (int)(tmp % 10);  tmp /= 10;
        int rw  = (int)(tmp % 100); tmp /= 100;
        int qv  = (int)(tmp % 100); tmp /= 100;
        long pu = tmp;  // 1次メッシュ番号

        int r  = rw / 10;
        int w  = rw % 10;
        int q  = qv / 10;
        int v  = qv % 10;

        int p  = (int)(pu / 100);
        int u  = (int)(pu % 100) + 100;

        // 4次サブインデックス (1-4 → s, x)
        int m4_0 = m4 - 1;  // 0-3
        int s = m4_0 / 2;
        int x = m4_0 % 2;

        // 5次サブインデックス
        int m5_0 = m5 - 1;  // 0-3
        int t = m5_0 / 2;
        int y = m5_0 % 2;

        // 南西コーナーを計算
        double swLat = p * LAT_1ST
                     + q * LAT_2ND
                     + r * LAT_3RD
                     + s * LAT_4TH
                     + t * LAT_5TH;
        double swLon = u
                     + v * LON_2ND
                     + w * LON_3RD
                     + x * LON_4TH
                     + y * LON_5TH;

        double neLat = swLat + LAT_5TH;
        double neLon = swLon + LON_5TH;

        return new LatLngBounds(new LatLng(swLat, swLon), new LatLng(neLat, neLon));
    }

    /**
     * メッシュの中心座標を返す
     */
    public static LatLng getMeshCenter(long meshId) {
        LatLngBounds bounds = getMeshBounds(meshId);
        return bounds.getCenter();
    }

    /**
     * 2点間の距離をメートルで返す（簡易計算）
     */
    public static float distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        float[] result = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, result);
        return result[0];
    }
}
