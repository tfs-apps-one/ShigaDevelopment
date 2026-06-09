package tfsapps.shigadevelopment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

// >>>test_make>>>
import java.util.HashMap;
import java.util.Map;
// <<<test_make<<<
import java.util.ArrayList;
import java.util.List;

/**
 * 開拓日誌リスト（全19市町の攻略状況）RecyclerView アダプタ
 */
public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {

    public enum SortMode { BY_DISTANCE, BY_PROGRESS }

    private final Context context;
    private List<CityInfo> displayList;
    private SortMode sortMode = SortMode.BY_DISTANCE;
    private double myLat = 0, myLon = 0;

    // >>>test_make>>>
    // ---- デバッグモード: 市町カード10連続タップ → 4スポット全達成 ----
    /** 市町全達成をトリガーするコールバック */
    public interface OnDebugCityAchieveListener {
        void onDebugCityAchieve(String cityId);
    }
    private OnDebugCityAchieveListener debugCityAchieveListener = null;
    /** cityId → 連続タップ数 */
    private final Map<String, Integer> debugCityTapCount   = new HashMap<>();
    /** cityId → 最後にタップした時刻 */
    private final Map<String, Long>    debugCityLastTapMs  = new HashMap<>();
    private static final int  DEBUG_CITY_TAP_THRESHOLD = 10;    // 10回で全達成
    private static final long DEBUG_CITY_TAP_WINDOW_MS = 5000L; // 5秒以内の連続タップ

    /** デバッグリスナーを登録する */
    public void setDebugCityAchieveListener(OnDebugCityAchieveListener listener) {
        this.debugCityAchieveListener = listener;
    }
    // <<<test_make<<<

    public CityAdapter(Context context, List<CityInfo> cities) {
        this.context     = context;
        this.displayList = new ArrayList<>(cities);
    }

    public void setMyLocation(double lat, double lon) {
        this.myLat = lat;
        this.myLon = lon;
    }

    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        sort();
        notifyDataSetChanged();
    }

    public void refreshData(List<CityInfo> cities) {
        this.displayList = new ArrayList<>(cities);
        sort();
        notifyDataSetChanged();
    }

    private void sort() {
        if (sortMode == SortMode.BY_PROGRESS) {
            displayList.sort((a, b) ->
                    Integer.compare(b.getProgressPercent(), a.getProgressPercent()));
        } else {
            if (myLat != 0 && myLon != 0) {
                float[] ra = new float[1], rb = new float[1];
                displayList.sort((a, b) -> {
                    double aLat = 0, aLon = 0, bLat = 0, bLon = 0;
                    for (SpotInfo s : a.spots) { aLat += s.lat; aLon += s.lon; }
                    aLat /= a.spots.size(); aLon /= a.spots.size();
                    for (SpotInfo s : b.spots) { bLat += s.lat; bLon += s.lon; }
                    bLat /= b.spots.size(); bLon /= b.spots.size();
                    android.location.Location.distanceBetween(myLat, myLon, aLat, aLon, ra);
                    android.location.Location.distanceBetween(myLat, myLon, bLat, bLon, rb);
                    return Float.compare(ra[0], rb[0]);
                });
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_city, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CityInfo city = displayList.get(position);
        int visited   = city.getVisitedCount();
        int total     = city.spots.size();
        int pct       = city.getProgressPercent();

        h.tvCityName.setText(city.name);
        h.tvStars.setText(buildStars(visited, total));
        h.tvPercent.setText(visited + "/" + total);
        h.progressBar.setProgress(pct);

        // コンプリート済みはゴールド背景
        if (city.isCompleted) {
            h.card.setCardBackgroundColor(
                    context.getResources().getColor(R.color.gold_light, null));
        } else {
            h.card.setCardBackgroundColor(
                    context.getResources().getColor(R.color.card_bg, null));
        }

        // >>>test_make>>>
        // 市町カード 10連続タップ → その市町の4スポット全達成（デバッグ用）
        final String cityId = city.id;
        h.card.setOnClickListener(v -> {
            if (debugCityAchieveListener == null) return;

            long now  = System.currentTimeMillis();
            Long last = debugCityLastTapMs.get(cityId);
            int count = (last != null && (now - last) < DEBUG_CITY_TAP_WINDOW_MS)
                    ? debugCityTapCount.getOrDefault(cityId, 0) + 1
                    : 1;
            debugCityTapCount.put(cityId, count);
            debugCityLastTapMs.put(cityId, now);

            if (count >= DEBUG_CITY_TAP_THRESHOLD) {
                debugCityTapCount.put(cityId, 0);
                debugCityAchieveListener.onDebugCityAchieve(cityId); // 全達成トリガー
            }
        });
        // <<<test_make<<<
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    private String buildStars(int visited, int total) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            sb.append(i < visited ? "★" : "☆");
        }
        return sb.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView    card;
        TextView    tvCityName;
        TextView    tvStars;
        TextView    tvPercent;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            card        = itemView.findViewById(R.id.cardCity);
            tvCityName  = itemView.findViewById(R.id.tvCityName);
            tvStars     = itemView.findViewById(R.id.tvStars);
            tvPercent   = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
