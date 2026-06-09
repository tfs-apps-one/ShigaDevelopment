package tfsapps.shigadevelopment;

import java.util.List;

/**
 * 市町情報
 */
public class CityInfo {
    public final String id;          // "kusatsu"
    public final String name;        // "草津市"
    public final List<SpotInfo> spots;
    public boolean isCompleted;      // 4スポット全訪問済み（= 100%達成）

    public CityInfo(String id, String name, List<SpotInfo> spots) {
        this.id          = id;
        this.name        = name;
        this.spots       = spots;
        this.isCompleted = false;
    }

    /** 訪問済みスポット数 */
    public int getVisitedCount() {
        int count = 0;
        for (SpotInfo s : spots) {
            if (s.isVisited) count++;
        }
        return count;
    }

    /** 達成率 (0-100) */
    public int getProgressPercent() {
        if (spots.isEmpty()) return 0;
        return getVisitedCount() * 100 / spots.size();
    }

    /** 全スポット訪問済み（100%）かチェックし、isCompleted を更新 */
    public boolean checkAndUpdateCompletion() {
        if (getVisitedCount() == spots.size()) {
            isCompleted = true;
        }
        return isCompleted;
    }
}
