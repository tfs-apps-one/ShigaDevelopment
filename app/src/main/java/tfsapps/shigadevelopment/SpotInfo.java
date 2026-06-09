package tfsapps.shigadevelopment;

/**
 * チェックインスポット情報
 */
public class SpotInfo {
    public final String id;          // "kusatsu_1" など
    public final String name;        // "草津宿本陣"
    public final double lat;
    public final double lon;
    public final String cityId;      // 所属市町ID
    public boolean isVisited;        // 訪問済みか

    /** チェックイン判定半径 (メートル) */
    public static final float CHECKIN_RADIUS_M = 300.0f;

    public SpotInfo(String id, String name, double lat, double lon, String cityId) {
        this.id      = id;
        this.name    = name;
        this.lat     = lat;
        this.lon     = lon;
        this.cityId  = cityId;
        this.isVisited = false;
    }

    /** 現在地からのチェックイン判定（デフォルト半径） */
    public boolean isInRange(double myLat, double myLon) {
        return isInRange(myLat, myLon, CHECKIN_RADIUS_M);
    }

    /** 現在地からのチェックイン判定（半径指定：宝箱ブースト対応） */
    public boolean isInRange(double myLat, double myLon, float radiusM) {
        float[] result = new float[1];
        android.location.Location.distanceBetween(myLat, myLon, lat, lon, result);
        return result[0] <= radiusM;
    }
}
