package tfsapps.shigadevelopment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 滋賀開拓ラリー メインアクティビティ（プランB対応版）
 *
 * 探索率 = (手動踏破メッシュ数 + コンプリート済み市町の全メッシュ数) / 滋賀全メッシュ数
 *
 * 市町コンプリート時の処理（対策C）:
 *   1. その市町内の手動踏破メッシュを visited_meshes から削除（重複カウント防止）
 *   2. 市町の全メッシュIDを FogTileProvider に渡して霧を一気解放
 *   3. city_completion テーブルに記録
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERM_LOCATION = 1001;

    // --- Map ---
    private GoogleMap mMap;
    private TileOverlay fogOverlay;
    private FogTileProvider fogProvider;
    // ステータスバーの高さ（px）。地図右上の現在地ボタンをずらすために使用
    private int statusBarInsetTopPx = 0;

    // 現在地 125m 緑円 + 300m チェックインリング
    private Circle myRadiusCircle;
    private Circle checkinRadiusCircle;

    // スポットマーカー（spotId → Marker）
    private final Map<String, Marker> spotMarkers = new HashMap<>();

    // >>>test_make>>>
    // ---- デバッグモード用変数 ----
    /** markerId → spotId 逆引きマップ（タップ判定に使用） */
    private final Map<String, String> markerToSpotId = new HashMap<>();
    /** spotId → 連続タップ回数 */
    private final Map<String, Integer> debugTapCount = new HashMap<>();
    /** spotId → 最後にタップした時刻 */
    private final Map<String, Long> debugLastTapTime = new HashMap<>();
    private static final int  DEBUG_TAP_THRESHOLD   = 10;    // 10回で達成ON
    private static final long DEBUG_TAP_WINDOW_MS   = 5000L; // 5秒以内の連続タップ
    private static final long DEBUG_HOLD_MS         = 10000L;// 10秒ホールドで達成OFF
    private final Handler  debugHoldHandler  = new Handler(Looper.getMainLooper());
    private Runnable       debugHoldRunnable = null;
    private float          debugHoldStartX, debugHoldStartY;
    private static final float DEBUG_HOLD_SLOP = 40f; // ホールド中の許容移動量(px)
    /** 探索率エリア連続タップ数（10回で全リセット） */
    private int  debugExpTapCount   = 0;                  // >>>test_make>>>
    private long debugExpLastTapMs  = 0L;                 // >>>test_make>>>
    // <<<test_make<<<

    // 魔王の霧エフェクト
    private static final LatLng MAOU_CASTLE  = new LatLng(35.4697, 136.1400);
    private static final LatLng SHIGA_CENTER = new LatLng(35.18,   136.07);
    private final List<Circle>  maouFogCircles  = new ArrayList<>();
    private int[]               maouBaseAlphas  = new int[0];
    private ValueAnimator        maouFogAnimator;

    // --- Location ---
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private double myLat = 0, myLon = 0;

    // --- Game ---
    private GameData    gameData;
    private DatabaseHelper db;
    private MeshLookupTable lookupTable;

    // 手動踏破メッシュ（in-memory）
    private Set<Long>  visitedMeshes      = new HashSet<>();
    // コンプリート済み市町の全メッシュID（in-memory、霧描画用）
    private Set<Long>  completedCityMeshes = new HashSet<>();
    // コンプリート済み市町ID
    private Set<String> completedCityIds   = new HashSet<>();

    // [プランB] 探索率
    private int totalShigaMeshCount = 0;

    // ── 宝箱ブーストシステム ─────────────────────────────────────────
    /** 現在のブースト半径 (m)。0 = ブーストなし */
    private float boostRadius          = 0f;
    /** ブースト残り有効マス数 */
    private int   boostRemainingMeshes = 0;
    /** 宝箱ポップアップを表示中か */
    private boolean isTreasureVisible  = false;
    /** 宝箱ポップアップに表示する報酬半径 */
    private int pendingBoostRadius     = 0;

    // --- UI ---
    private TextView tvCurrentCity;
    private TextView tvStars;
    private TextView tvPercent;
    private ProgressBar pbCity;
    private RecyclerView rvCities;
    private CityAdapter cityAdapter;
    private LinearLayout overlayConquestLayout;
    private TextView tvConquestTitle;
    private TextView tvConquestSub;
    private View confettiView;
    private TextView tvExplorationRate;    // 地図上の探索率テキスト
    private TextView tvBoostStatus;        // ブーストステータスバナー
    // スポット到達お祝いポップアップ構成ビュー
    private android.widget.LinearLayout layoutSpotCelebration;
    private View     celebFlashView;
    private TextView tvCelebSpotName;
    private TextView tvCelebMessage;
    private TextView tvCelebStars;
    private TextView tvCelebProgress;
    // 宝箱ポップアップ構成ビュー
    private android.widget.LinearLayout layoutTreasureChest;
    private View     treasureDimOverlay;
    private TextView tvChestPrize;
    private TextView tvChestEffectDesc;
    private Button   btnChestOpen;
    private Button   btnChestSkip;

    private boolean isHeroMode = false;

    // ── 画面ロック機能 ──────────────────────────────────────────────────────────
    /** ロック中かどうか */
    private boolean isScreenLocked = false;
    /** WakeLock: 画面ロック中のスリープ防止・GPS継続用 */
    private PowerManager.WakeLock wakeLock;
    /** WakeLock: 探索モード中の画面点灯維持用（常時取得） */
    private PowerManager.WakeLock exploreWakeLock;
    /** ロックオーバーレイ本体 */
    private FrameLayout lockOverlay;
    /** 長押し進捗バー */
    private View unlockProgressFill;
    /** 長押し中かどうか */
    private boolean isUnlockHolding = false;
    /** 長押し完了までの時間（ms） */
    private static final long UNLOCK_HOLD_MS = 3000L;
    /** 長押し進捗アニメーター */
    private ValueAnimator unlockAnimator;
    /** ロックボタン */
    private Button btnScreenLock;

    // =========================================================================
    // onCreate
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db          = DatabaseHelper.getInstance(this);
        gameData    = GameData.getInstance();
        lookupTable = MeshLookupTable.getInstance();

        // DB から復元
        visitedMeshes = db.loadVisitedMeshes();
        gameData.restoreFromDb(db.loadVisitedSpots());

        // コンプリート済み市町の復元
        completedCityIds = db.loadCompletedCityIds();
        for (CityInfo c : gameData.cities) {
            if (completedCityIds.contains(c.id)) c.isCompleted = true;
        }

        isHeroMode = db.isHeroEndingAchieved();

        // UI参照
        tvCurrentCity         = findViewById(R.id.tvCurrentCity);
        tvStars               = findViewById(R.id.tvStars);
        tvPercent             = findViewById(R.id.tvPercent);
        pbCity                = findViewById(R.id.pbCity);
        rvCities              = findViewById(R.id.rvCities);
        overlayConquestLayout = findViewById(R.id.overlayConquest);
        tvConquestTitle       = findViewById(R.id.tvConquestTitle);
        tvConquestSub         = findViewById(R.id.tvConquestSub);
        confettiView          = findViewById(R.id.confettiView);
        tvExplorationRate      = findViewById(R.id.tvExplorationRate);
        tvBoostStatus          = findViewById(R.id.tvBoostStatus);

        // ステータスバー（時刻・電波・バッテリー表示）と探索率オーバーレイの重なり対策。
        // Android 15(targetSdk 35)以降は edge-to-edge がデフォルトになり、
        // 何もしないとコンテンツがステータスバーの下に潜り込んで重なって見える。
        // ステータスバー分の高さだけ paddingTop を動的に追加してずらす。
        View topStatusContainer = findViewById(R.id.topStatusContainer);
        ViewCompat.setOnApplyWindowInsetsListener(topStatusContainer, (v, insets) -> {
            int statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarTop, v.getPaddingRight(), v.getPaddingBottom());
            // 地図右上の「現在地」ボタン（Googleマップ標準UI）も同じ分だけ下げる
            statusBarInsetTopPx = statusBarTop;
            applyMapTopPaddingForStatusBar();
            return insets;
        });
        layoutSpotCelebration  = findViewById(R.id.layoutSpotCelebration);
        celebFlashView         = findViewById(R.id.celebFlashView);
        tvCelebSpotName        = findViewById(R.id.tvCelebSpotName);
        tvCelebMessage         = findViewById(R.id.tvCelebMessage);
        tvCelebStars           = findViewById(R.id.tvCelebStars);
        tvCelebProgress        = findViewById(R.id.tvCelebProgress);
        // 宝箱UI
        layoutTreasureChest    = findViewById(R.id.layoutTreasureChest);
        treasureDimOverlay     = findViewById(R.id.treasureDimOverlay);
        tvChestPrize           = findViewById(R.id.tvChestPrize);
        tvChestEffectDesc      = findViewById(R.id.tvChestEffectDesc);
        btnChestOpen           = findViewById(R.id.btnChestOpen);
        btnChestSkip           = findViewById(R.id.btnChestSkip);
        // 画面ロックUI
        lockOverlay            = findViewById(R.id.lockOverlay);
        unlockProgressFill     = findViewById(R.id.unlockProgressFill);
        btnScreenLock          = findViewById(R.id.btnScreenLock);
        setupScreenLock();

        // >>>test_make>>>
        // 探索率エリアを10回連続タップ → 全データリセット（開発者デバッグ用）
        tvExplorationRate.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - debugExpLastTapMs > DEBUG_TAP_WINDOW_MS) {
                debugExpTapCount = 1;
            } else {
                debugExpTapCount++;
            }
            debugExpLastTapMs = now;
            if (debugExpTapCount >= DEBUG_TAP_THRESHOLD) {
                debugExpTapCount = 0;
                onDebugFullReset();
            }
        });
        // <<<test_make<<<

        // RecyclerView
        cityAdapter = new CityAdapter(this, gameData.cities);
        rvCities.setLayoutManager(new LinearLayoutManager(this));
        rvCities.setAdapter(cityAdapter);
        rvCities.setNestedScrollingEnabled(false);

        // >>>test_make>>>
        // 開拓日誌リスト: 市町カード10連続タップ → その市町の4スポット全達成
        cityAdapter.setDebugCityAchieveListener(cityId -> {
            CityInfo city = gameData.findCityById(cityId);
            if (city == null) return;
            if (city.isCompleted) {
                Toast.makeText(this,
                        "[DEBUG] " + city.name + " は既にコンプリート済みです",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // 未達成のスポットを順番に達成フラグONにする
            for (SpotInfo spot : city.spots) {
                if (!spot.isVisited) {
                    onDebugSpotAchieve(spot.id);
                }
            }
        });
        // <<<test_make<<<

        // ソートボタン
        Button btnSortDistance = findViewById(R.id.btnSortDistance);
        Button btnSortProgress = findViewById(R.id.btnSortProgress);
        btnSortDistance.setOnClickListener(v ->
                cityAdapter.setSortMode(CityAdapter.SortMode.BY_DISTANCE));
        btnSortProgress.setOnClickListener(v ->
                cityAdapter.setSortMode(CityAdapter.SortMode.BY_PROGRESS));
        btnSortDistance.setOnLongClickListener(v -> { showResetDialog(); return true; });

        if (isHeroMode) applyHeroTheme();

        // ズームボタン
        Button btnZoomIn  = findViewById(R.id.btnZoomIn);
        Button btnZoomOut = findViewById(R.id.btnZoomOut);
        btnZoomIn.setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn());
        });
        btnZoomOut.setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut());
        });

        // 地図初期化
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // 位置情報
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) onLocationUpdate(loc);
            }
        };

        // [プランB] ルックアップテーブルの準備
        initializeLookupTable();
        requestLocationPermission();

        // ── 探索モード中は画面を常にONに保つ（システムのスリープ調整を無効化）──
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // =========================================================================
    // [プランB] ルックアップテーブル初期化
    // =========================================================================
    private void initializeLookupTable() {
        if (MeshLookupTable.isAlreadyGenerated(this) && !db.isMeshLookupEmpty()) {
            // 2回目以降: SQLite から高速ロード
            new Thread(() -> {
                lookupTable.loadFromDb(db);
                loadCompletedCityMeshesFromDb();
                totalShigaMeshCount = lookupTable.getTotalShigaMeshCount();
                runOnUiThread(this::updateExplorationRateUI);
            }).start();
        } else {
            // 初回: 生成ダイアログを表示してバックグラウンドで生成
            showLookupGenerationDialog();
        }
    }

    @SuppressWarnings("deprecation")
    private void showLookupGenerationDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("初回セットアップ");
        dialog.setMessage("滋賀県のメッシュデータを生成中...\n（初回のみ、約2〜5秒かかります）");
        dialog.setCancelable(false);
        dialog.show();

        lookupTable.generateAsync(this, db, () -> {
            // 完了後に UI スレッドで実行
            loadCompletedCityMeshesFromDb();
            totalShigaMeshCount = lookupTable.getTotalShigaMeshCount();
            updateExplorationRateUI();
            dialog.dismiss();
            Toast.makeText(this,
                "メッシュデータ生成完了！\n滋賀全体 " + totalShigaMeshCount + " マス",
                Toast.LENGTH_LONG).show();
        });
    }

    /** 既コンプリート済み市町のメッシュIDをDBから読み込んでメモリに展開 */
    private void loadCompletedCityMeshesFromDb() {
        completedCityMeshes.clear();
        for (String cityId : completedCityIds) {
            completedCityMeshes.addAll(db.getMeshIdsForCity(cityId));
        }
    }

    // =========================================================================
    // onMapReady
    // =========================================================================
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        // 地図右上の「現在地」ボタンがステータスバーと重ならないよう下にずらす
        applyMapTopPaddingForStatusBar();

        LatLng shigaCenter = new LatLng(35.18, 136.07);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder().target(shigaCenter).zoom(9.5f).build()));

        // 霧プロバイダー（プランB版：全メッシュIDセットを渡す）
        fogProvider = new FogTileProvider(visitedMeshes, completedCityMeshes);
        fogOverlay  = mMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(fogProvider).zIndex(100));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        setupMaouFogEffect();
        addSpotMarkersToMap();
        // >>>test_make>>>
        setupDebugInteractions(); // デバッグモード: タップ達成・ホールドリセット
        // <<<test_make<<<
    }

    /**
     * 地図上部にステータスバー分のパディングを設定する。
     * これにより、Googleマップ標準の「現在地」ボタン（右上）や
     * コンパス・ロゴなどが端末のステータスバー（時刻・電波・バッテリー表示）と
     * 重ならない位置に自動的に配置される。
     * onMapReady と ウィンドウインセット取得の両方から呼ばれる可能性があるため、
     * mMap が未準備の場合は何もしない。
     */
    private void applyMapTopPaddingForStatusBar() {
        if (mMap == null) return;
        mMap.setPadding(0, statusBarInsetTopPx, 0, 0);
    }

    // =========================================================================
    // 位置情報更新
    // =========================================================================
    private void onLocationUpdate(Location loc) {
        myLat = loc.getLatitude();
        myLon  = loc.getLongitude();

        long meshId = MeshCalculator.calcMeshId(myLat, myLon);

        // 滋賀県内のメッシュのみ記録する（県外の移動はカウントしない）
        if (!db.isMeshLookupEmpty() && db.isMeshInShiga(meshId)) {
            boolean newMesh = db.addVisitedMesh(meshId);
            if (newMesh) {
                visitedMeshes.add(meshId);

                // ── ブースト中は半径内の周辺メッシュも一括記録 ───────────────
                if (boostRadius > 0f) {
                    recordNearbyMeshes(myLat, myLon, boostRadius);
                }

                fogProvider.updateSelfWalkedMeshes(visitedMeshes);
                fogOverlay.clearTileCache();
                updateExplorationRateUI();

                // ── ブーストのカウントダウン ─────────────────────────
                if (boostRemainingMeshes > 0) {
                    boostRemainingMeshes--;
                    if (boostRemainingMeshes <= 0) {
                        boostRadius = 0f;
                        runOnUiThread(() -> {
                            updateBoostStatusUI();
                            updateMyRadiusCircle(myLat, myLon);
                        });
                    } else {
                        runOnUiThread(this::updateBoostStatusUI);
                    }
                }

                // >>>test_make_takara>>>
                // ---- 宝箱発生確率 ----
                // ★本番リリース前にテスト行を削除し、本番行のコメントを外すこと★
                 float takaraChance = 0.03f + (float)(Math.random() * 0.04f); // 本番: 3%〜7%
                //float takaraChance = 0.10f + (float)(Math.random() * 0.20f);   // テスト: 30%〜50%
                // <<<test_make_takara<<<
                if (Math.random() < takaraChance && !isTreasureVisible) {
                    int[] radii = {400, 500, 600};
                    pendingBoostRadius = radii[(int)(Math.random() * radii.length)];
                    runOnUiThread(() -> showTreasureChest(pendingBoostRadius));
                }
            }
        }

        CityInfo nearestCity = gameData.findNearestCity(myLat, myLon);
        updateDashboard(nearestCity);
        checkSpotCheckins();
        cityAdapter.setMyLocation(myLat, myLon);
        runOnUiThread(() -> updateMyRadiusCircle(myLat, myLon));
    }

    // =========================================================================
    // ブースト時：現在地周辺のメッシュを一括記録
    // 5次メッシュ ≈ 250m四方 → ブースト半径をメッシュ単位に換算してスキャン
    // =========================================================================
    private void recordNearbyMeshes(double lat, double lon, float radiusM) {
        // 1メッシュあたりの緯度・経度幅（概算）
        // 5次メッシュ ≈ 250m  →  緯度幅 ≈ 0.00208度 / 経度幅 ≈ 0.003125度
        final double meshLatStep = 2.0 / 3.0 / 8.0 / 10.0 / 2.0 / 2.0; // LAT_5TH
        final double meshLonStep = 1.0       / 8.0 / 10.0 / 2.0 / 2.0; // LON_5TH

        // 半径から走査するメッシュ数を算出（1m ≈ 1/111000度）
        double latStep  = meshLatStep;
        double lonStep  = meshLonStep;
        int scanLat = (int)(radiusM / 111000.0 / latStep) + 2;
        int scanLon = (int)(radiusM / (111000.0 * Math.cos(Math.toRadians(lat))) / lonStep) + 2;

        boolean updated = false;
        for (int dy = -scanLat; dy <= scanLat; dy++) {
            for (int dx = -scanLon; dx <= scanLon; dx++) {
                double cLat = lat + dy * latStep;
                double cLon = lon + dx * lonStep;
                // 中心からの距離チェック
                float[] dist = new float[1];
                android.location.Location.distanceBetween(lat, lon, cLat, cLon, dist);
                if (dist[0] > radiusM) continue;

                long nearMeshId = MeshCalculator.calcMeshId(cLat, cLon);
                if (!db.isMeshInShiga(nearMeshId)) continue;
                if (visitedMeshes.contains(nearMeshId)) continue;

                boolean added = db.addVisitedMesh(nearMeshId);
                if (added) {
                    visitedMeshes.add(nearMeshId);
                    updated = true;
                }
            }
        }
        if (updated) {
            fogProvider.updateSelfWalkedMeshes(visitedMeshes);
            fogOverlay.clearTileCache();
            runOnUiThread(this::updateExplorationRateUI);
        }
    }

    // =========================================================================
    // スポット チェックイン判定
    // =========================================================================
    private void checkSpotCheckins() {
        // 有効チェックイン半径（ブースト中はブースト半径、通常は300m）
        float activeRadius = (boostRadius > 0f) ? boostRadius : SpotInfo.CHECKIN_RADIUS_M;

        for (CityInfo city : gameData.cities) {
            if (city.isCompleted) continue;
            for (SpotInfo spot : city.spots) {
                if (spot.isVisited) continue;
                if (!spot.isInRange(myLat, myLon, activeRadius)) continue;

                spot.isVisited = true;
                db.addVisitedSpot(spot.id);

                runOnUiThread(() -> {
                    Marker m = spotMarkers.get(spot.id);
                    if (m != null) {
                        m.setIcon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_YELLOW));
                        m.setSnippet("✓ チェックイン済");
                    }
                });

                Toast.makeText(this,
                        "📍 " + spot.name + " にチェックイン！", Toast.LENGTH_SHORT).show();

                // スポット単体の到達エフェクト
                showSpotCelebration(spot, city);

                if (city.checkAndUpdateCompletion()) {
                    onCityCompleted(city);
                }
                cityAdapter.refreshData(gameData.cities);
            }
        }
    }

    // =========================================================================
    // [プランB] 市町100%達成時の処理
    // =========================================================================
    private void onCityCompleted(CityInfo city) {
        completedCityIds.add(city.id);

        // バックグラウンドで重い処理を行う
        new Thread(() -> {
            // ① その市町の全メッシュIDを取得（DB from mesh_city_lookup）
            Set<Long> cityMeshIds = db.getMeshIdsForCity(city.id);
            int cityMeshCount = cityMeshIds.size();

            // ② コンプリート記録
            db.markCityCompleted(city.id, cityMeshCount);

            // ③ completedCityMeshes には「まだ自分が歩いていないマス」だけを追加する。
            //    歩済みのマスは visitedMeshes に残したまま → 緑マスが消えないようにする。
            //    ※ 二重カウント防止のため addAll 前に visitedMeshes との差分を取る。
            Set<Long> newCityMeshes = new HashSet<>(cityMeshIds);
            newCityMeshes.removeAll(visitedMeshes); // 自分が歩いたマスは除く
            completedCityMeshes.addAll(newCityMeshes);

            // ④ 霧プロバイダーを更新（霧を一気解放）
            //    selfWalkedMeshes は変更なし（緑マス維持）
            fogProvider.addCompletedCityMeshes(newCityMeshes);

            // Activity が破棄済みの場合はUI操作をスキップ（クラッシュ防止）
            if (isFinishing() || isDestroyed()) return;

            runOnUiThread(() -> {
                // UIスレッドでも再確認（クラッシュ防止）
                if (isFinishing() || isDestroyed()) return;

                fogOverlay.clearTileCache();
                updateExplorationRateUI();

                // 演出
                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vib != null && vib.hasVibrator()) {
                    long[] pattern = {0, 200, 100, 200, 100, 300};
                    vib.vibrate(VibrationEffect.createWaveform(pattern, -1));
                }
                showConquestOverlay(
                        "【" + city.name + " 制覇！】",
                        "称号：" + city.name + "の開拓者 をアンロック！\n"
                        + "+" + cityMeshCount + " マス解放！");

                if (gameData.isAllCitiesCompleted()) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::onGrandEnding, 3500);
                }
            });
        }).start();
    }

    // =========================================================================
    // [プランB] 探索率UIの更新
    // =========================================================================
    private void updateExplorationRateUI() {
        if (tvExplorationRate == null || totalShigaMeshCount == 0) return;

        // 手動踏破 + コンプリート済み市町の合計
        int exploredCount = visitedMeshes.size() + completedCityMeshes.size();
        float rate = (float) exploredCount / totalShigaMeshCount * 100f;

        String text = String.format("探索済 %.1f%%  (%,d / %,d マス)",
                rate, exploredCount, totalShigaMeshCount);
        tvExplorationRate.setText(text);
    }

    // =========================================================================
    // スポット到達 お祝いエフェクト（チェックイン達成時の演出）
    //
    // 演出シーケンス:
    //   [0ms]    金色フラッシュ（画面全体が一瞬光る）
    //   [0ms]    お祝いバイブレーション（短-短-長のリズム）
    //   [100ms]  ポップアップがバウンスしながら登場
    //   [700ms]  星マークが1回脈動
    //   [3500ms] フェードアウトして消える
    // =========================================================================
    private void showSpotCelebration(SpotInfo spot, CityInfo city) {
        runOnUiThread(() -> {
            if (layoutSpotCelebration == null) return;

            int visitedCount = city.getVisitedCount();
            int totalCount   = city.spots.size();

            // ── コンテンツをセット ──────────────────────────────────────
            tvCelebSpotName.setText(spot.name);
            tvCelebMessage.setText("チェックイン達成！");
            tvCelebStars.setText(buildStarLine(visitedCount, totalCount));
            tvCelebProgress.setText(city.name + "  " + visitedCount + " / " + totalCount + " スポット");

            // ── ① お祝いバイブレーション（短-短-長） ───────────────────
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                long[] pattern    = {0, 80, 50, 80, 50, 250};
                int[]  amplitudes = {0, 180, 0, 200, 0, 255};
                vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1));
            }

            // ── ② 金色フラッシュ（瞬間光） ─────────────────────────────
            if (celebFlashView != null) {
                celebFlashView.setAlpha(0.85f);
                celebFlashView.setVisibility(View.VISIBLE);
                celebFlashView.animate()
                        .alpha(0f)
                        .setDuration(350)
                        .withEndAction(() -> celebFlashView.setVisibility(View.GONE))
                        .start();
            }

            // ── ③ ポップアップ バウンス登場（100ms後から開始） ──────────
            layoutSpotCelebration.setVisibility(View.VISIBLE);
            layoutSpotCelebration.setAlpha(0f);
            layoutSpotCelebration.setScaleX(0.2f);
            layoutSpotCelebration.setScaleY(0.2f);

            // バウンスキーフレーム（0.2 → 1.15 → 0.92 → 1.04 → 1.0）
            android.animation.Keyframe kf0 = android.animation.Keyframe.ofFloat(0f,    0.2f);
            android.animation.Keyframe kf1 = android.animation.Keyframe.ofFloat(0.55f, 1.15f);
            android.animation.Keyframe kf2 = android.animation.Keyframe.ofFloat(0.75f, 0.92f);
            android.animation.Keyframe kf3 = android.animation.Keyframe.ofFloat(0.90f, 1.04f);
            android.animation.Keyframe kf4 = android.animation.Keyframe.ofFloat(1.0f,  1.00f);

            android.animation.PropertyValuesHolder pvhX =
                    android.animation.PropertyValuesHolder.ofKeyframe("scaleX", kf0, kf1, kf2, kf3, kf4);
            android.animation.PropertyValuesHolder pvhY =
                    android.animation.PropertyValuesHolder.ofKeyframe("scaleY", kf0, kf1, kf2, kf3, kf4);

            ObjectAnimator bounceAnim = ObjectAnimator.ofPropertyValuesHolder(
                    layoutSpotCelebration, pvhX, pvhY);
            bounceAnim.setDuration(500);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(
                    layoutSpotCelebration, "alpha", 0f, 1f);
            fadeIn.setDuration(200);

            AnimatorSet appear = new AnimatorSet();
            appear.playTogether(bounceAnim, fadeIn);
            appear.setStartDelay(100);
            appear.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    // ── ④ 星マーク脈動（1回）────────────────────────────
                    if (tvCelebStars != null) {
                        AnimatorSet pulse = new AnimatorSet();
                        pulse.playTogether(
                                ObjectAnimator.ofFloat(tvCelebStars, "scaleX", 1f, 1.4f, 1f),
                                ObjectAnimator.ofFloat(tvCelebStars, "scaleY", 1f, 1.4f, 1f));
                        pulse.setDuration(400);
                        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
                        pulse.start();
                    }

                    // ── ⑤ 3秒後にフェードアウト消去 ─────────────────────
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                        layoutSpotCelebration.animate()
                                .alpha(0f)
                                .scaleX(0.85f)
                                .scaleY(0.85f)
                                .setDuration(400)
                                .withEndAction(() -> {
                                    layoutSpotCelebration.setVisibility(View.GONE);
                                    layoutSpotCelebration.setScaleX(1f);
                                    layoutSpotCelebration.setScaleY(1f);
                                })
                                .start(),
                    3000);
                }
            });
            appear.start();
        });
    }

    private String buildStarLine(int visited, int total) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) sb.append(i < visited ? "★" : "☆");
        return sb.toString();
    }

    // =========================================================================
    // ダッシュボード更新
    // =========================================================================
    private void updateDashboard(CityInfo city) {
        if (city == null) return;
        runOnUiThread(() -> {
            tvCurrentCity.setText("現在地：" + city.name);
            int visited = city.getVisitedCount();
            int total   = city.spots.size();
            int pct     = city.getProgressPercent();
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < total; i++) stars.append(i < visited ? "★" : "☆");
            tvStars.setText(stars);
            tvPercent.setText(visited + "/" + total);
            pbCity.setProgress(pct);
        });
    }

    // =========================================================================
    // グランドエンディング
    // =========================================================================
    private void onGrandEnding() {
        db.setHeroEndingAchieved();
        isHeroMode = true;

        // ── ① 琵琶湖含む全エリアの霧を完全解放 ──────────────────────
        if (fogProvider != null) {
            fogProvider.clearAllFog();
        }
        if (fogOverlay != null) {
            fogOverlay.clearTileCache();
        }

        // ── ② 英雄テーマを適用 ───────────────────────────────────────
        runOnUiThread(this::applyHeroTheme);

        // ── ③ バイブレーション（達成の喜び） ─────────────────────────
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null && vib.hasVibrator()) {
            long[] pattern = {0, 500, 100, 500, 100, 500, 100, 1000};
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }

        // ── ④ 少し間を置いてからエンディング画面へ遷移 ───────────────
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            Intent intent = new Intent(MainActivity.this, StoryEpilogueActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1500);
    }

    // =========================================================================
    // 制覇ポップアップ
    // =========================================================================
    private void showConquestOverlay(String title, String subtitle) {
        runOnUiThread(() -> {
            tvConquestTitle.setText(title);
            tvConquestSub.setText(subtitle);
            overlayConquestLayout.setVisibility(View.VISIBLE);
            overlayConquestLayout.setAlpha(0f);
            overlayConquestLayout.setScaleX(0.5f);
            overlayConquestLayout.setScaleY(0.5f);

            AnimatorSet anim = new AnimatorSet();
            anim.playTogether(
                    ObjectAnimator.ofFloat(overlayConquestLayout, "alpha",  0f, 1f),
                    ObjectAnimator.ofFloat(overlayConquestLayout, "scaleX", 0.5f, 1f),
                    ObjectAnimator.ofFloat(overlayConquestLayout, "scaleY", 0.5f, 1f));
            anim.setDuration(500);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();

            if (confettiView != null) {
                confettiView.setVisibility(View.VISIBLE);
                ValueAnimator blink = ValueAnimator.ofFloat(0f, 1f, 0.3f, 1f, 0.3f, 1f, 0f);
                blink.setDuration(3500);
                blink.addUpdateListener(a -> confettiView.setAlpha((Float) a.getAnimatedValue()));
                blink.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator a) {
                        if (confettiView != null) confettiView.setVisibility(View.GONE);
                    }
                });
                blink.start();
            }

            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    runOnUiThread(this::hideConquestOverlay), 5000);
            overlayConquestLayout.setOnClickListener(v -> hideConquestOverlay());
        });
    }

    private void hideConquestOverlay() {
        if (overlayConquestLayout.getVisibility() != View.VISIBLE) return;
        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(
                ObjectAnimator.ofFloat(overlayConquestLayout, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(overlayConquestLayout, "scaleY", 1f, 0.8f));
        anim.setDuration(300);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                overlayConquestLayout.setVisibility(View.GONE);
            }
        });
        anim.start();
    }

    // =========================================================================
    // 宝箱システム
    // =========================================================================

    /**
     * 宝箱ポップアップを表示する（派手なバウンス登場 + 暗転オーバーレイ）
     * @param radius ブースト半径 (400 / 500 / 600m)
     */
    private void showTreasureChest(int radius) {
        if (layoutTreasureChest == null) return;
        isTreasureVisible = true;

        // 報酬テキストをセット
        tvChestPrize.setText("🚀 探索半径 " + radius + "m ブースト！");
        // 効果説明テキスト（半径は動的に変える）
        if (tvChestEffectDesc != null) {
            tvChestEffectDesc.setText(
                "・チェックイン範囲が通常 300m → " + radius + "m に拡大\n"
                + "・新しいマスを 30マス 踏むまで有効\n"
                + "・スポットに近づきやすくなります！");
        }

        // 暗転オーバーレイを表示
        if (treasureDimOverlay != null) {
            treasureDimOverlay.setAlpha(0f);
            treasureDimOverlay.setVisibility(View.VISIBLE);
            treasureDimOverlay.animate().alpha(1f).setDuration(300).start();
        }

        // ポップアップ表示（初期状態）
        layoutTreasureChest.setVisibility(View.VISIBLE);
        layoutTreasureChest.setAlpha(0f);
        layoutTreasureChest.setScaleX(0.2f);
        layoutTreasureChest.setScaleY(0.2f);

        // バウンス登場アニメーション（0.2 → 1.15 → 0.95 → 1.05 → 1.0）
        android.animation.Keyframe kf0 = android.animation.Keyframe.ofFloat(0f,    0.2f);
        android.animation.Keyframe kf1 = android.animation.Keyframe.ofFloat(0.55f, 1.15f);
        android.animation.Keyframe kf2 = android.animation.Keyframe.ofFloat(0.75f, 0.95f);
        android.animation.Keyframe kf3 = android.animation.Keyframe.ofFloat(0.88f, 1.05f);
        android.animation.Keyframe kf4 = android.animation.Keyframe.ofFloat(1.0f,  1.00f);
        android.animation.PropertyValuesHolder pvhX =
            android.animation.PropertyValuesHolder.ofKeyframe("scaleX", kf0, kf1, kf2, kf3, kf4);
        android.animation.PropertyValuesHolder pvhY =
            android.animation.PropertyValuesHolder.ofKeyframe("scaleY", kf0, kf1, kf2, kf3, kf4);

        ObjectAnimator bounceAnim = ObjectAnimator.ofPropertyValuesHolder(
                layoutTreasureChest, pvhX, pvhY);
        bounceAnim.setDuration(550);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(layoutTreasureChest, "alpha", 0f, 1f);
        fadeIn.setDuration(200);

        AnimatorSet appear = new AnimatorSet();
        appear.playTogether(bounceAnim, fadeIn);
        appear.setStartDelay(80);
        appear.start();

        // 「開ける！」ボタン
        btnChestOpen.setOnClickListener(v -> {
            applyBoost(radius);
            hideTreasureChest();
        });
        // 「見送る」ボタン
        btnChestSkip.setOnClickListener(v -> hideTreasureChest());
    }

    /** 宝箱ポップアップを閉じる */
    private void hideTreasureChest() {
        if (layoutTreasureChest == null) return;

        // 暗転オーバーレイをフェードアウト
        if (treasureDimOverlay != null) {
            treasureDimOverlay.animate()
                    .alpha(0f).setDuration(250)
                    .withEndAction(() -> treasureDimOverlay.setVisibility(View.GONE))
                    .start();
        }

        // ポップアップをシュリンク退場
        layoutTreasureChest.animate()
                .alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(250)
                .withEndAction(() -> {
                    layoutTreasureChest.setVisibility(View.GONE);
                    layoutTreasureChest.setScaleX(1f);
                    layoutTreasureChest.setScaleY(1f);
                    isTreasureVisible = false;
                }).start();
    }

    /**
     * ブーストを適用する（30マス有効）
     * @param radius ブースト半径 (m)
     */
    private void applyBoost(int radius) {
        boostRadius          = (float) radius;
        boostRemainingMeshes = 30;   // 30マスで切れる
        updateBoostStatusUI();
        updateMyRadiusCircle(myLat, myLon);
        Toast.makeText(this,
                "🚀 探索半径が " + radius + "m に拡大！（30マス有効）",
                Toast.LENGTH_SHORT).show();
    }

    /** ブーストステータスバナーを更新する */
    private void updateBoostStatusUI() {
        if (tvBoostStatus == null) return;
        if (boostRemainingMeshes > 0 && boostRadius > 0f) {
            tvBoostStatus.setText(
                    "🚀 " + (int)boostRadius + "m ブースト中（残" + boostRemainingMeshes + "マス）");
            tvBoostStatus.setVisibility(View.VISIBLE);
        } else {
            tvBoostStatus.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // 英雄テーマ
    // =========================================================================
    private void applyHeroTheme() {
        View root = findViewById(R.id.rootLayout);
        if (root != null) root.setBackgroundColor(getResources().getColor(R.color.hero_bg, null));
        setTitle("★ 滋賀開拓ラリー ～英雄版～ ★");
    }

    // =========================================================================
    // 画面ロック機能
    // =========================================================================

    /**
     * 画面ロックの初期設定。
     * ロックボタンクリックでロックON、オーバーレイ上の長押し(3秒)で解除。
     */
    private void setupScreenLock() {
        if (btnScreenLock == null || lockOverlay == null) return;

        // ロックボタン：クリックでロックON
        btnScreenLock.setOnClickListener(v -> enableScreenLock());

        // ロックオーバーレイ：長押しで解除（タッチ以外の操作は全て無効化）
        View btnUnlock = lockOverlay.findViewById(R.id.btnUnlock);
        if (btnUnlock != null) {
            btnUnlock.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startUnlockHold();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cancelUnlockHold();
                        break;
                }
                return true;
            });
        }

        // ロックオーバーレイ自体のタッチを消費（誤操作防止）
        lockOverlay.setOnTouchListener((v, event) -> true);
    }

    /** 画面ロックを有効化する */
    private void enableScreenLock() {
        if (isScreenLocked) return;
        isScreenLocked = true;

        // WakeLock 取得：スリープさせずGPSを継続取得
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && wakeLock == null) {
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ShigaRally::ExploreWakeLock"
            );
            wakeLock.acquire(12 * 60 * 60 * 1000L); // 最大12時間
        }

        // ロックボタンアイコンを施錠マークに変更
        if (btnScreenLock != null) btnScreenLock.setText("\uD83D\uDD12");

        // オーバーレイをフェードインで表示
        lockOverlay.setAlpha(0f);
        lockOverlay.setVisibility(View.VISIBLE);
        lockOverlay.animate().alpha(1f).setDuration(400).start();

        Toast.makeText(this, "\uD83D\uDD12 画面ロック有効：GPS・探索は継続します", Toast.LENGTH_SHORT).show();
    }

    /** 画面ロックを解除する */
    private void disableScreenLock() {
        if (!isScreenLocked) return;
        isScreenLocked = false;

        // WakeLock 解放
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        // ボタンアイコンを開錠マークに戻す
        if (btnScreenLock != null) btnScreenLock.setText("\uD83D\uDD13");

        // オーバーレイをフェードアウトで非表示
        lockOverlay.animate().alpha(0f).setDuration(300).withEndAction(() ->
            lockOverlay.setVisibility(View.GONE)).start();

        Toast.makeText(this, "\uD83D\uDD13 画面ロック解除", Toast.LENGTH_SHORT).show();
    }

    /** 長押し解除開始 */
    private void startUnlockHold() {
        if (isUnlockHolding) return;
        isUnlockHolding = true;

        // 進捗バーを幅0にリセット
        if (unlockProgressFill != null) {
            unlockProgressFill.getLayoutParams().width = 0;
            unlockProgressFill.requestLayout();
        }

        unlockAnimator = ValueAnimator.ofFloat(0f, 1f);
        unlockAnimator.setDuration(UNLOCK_HOLD_MS);
        unlockAnimator.addUpdateListener(anim -> {
            if (unlockProgressFill == null) return;
            float fraction = (float) anim.getAnimatedValue();
            View parent = (View) unlockProgressFill.getParent();
            int parentWidth = (parent != null) ? parent.getWidth() : 540;
            unlockProgressFill.getLayoutParams().width = (int)(parentWidth * fraction);
            unlockProgressFill.requestLayout();
        });
        unlockAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isUnlockHolding) {
                    isUnlockHolding = false;
                    disableScreenLock();
                }
            }
        });
        unlockAnimator.start();
    }

    /** 長押し解除キャンセル */
    private void cancelUnlockHold() {
        if (!isUnlockHolding) return;
        isUnlockHolding = false;
        if (unlockAnimator != null) {
            unlockAnimator.cancel();
            unlockAnimator = null;
        }
        // 進捗バーをリセット
        if (unlockProgressFill != null) {
            unlockProgressFill.getLayoutParams().width = 0;
            unlockProgressFill.requestLayout();
        }
    }

    // =========================================================================
    // 現在地 125m 緑円 + チェックインリング（ブースト対応）
    // =========================================================================
    private void updateMyRadiusCircle(double lat, double lon) {
        if (mMap == null) return;
        LatLng pos = new LatLng(lat, lon);
        double activeRadius = (boostRadius > 0f) ? boostRadius : 300.0;

        if (myRadiusCircle == null) {
            myRadiusCircle = mMap.addCircle(new CircleOptions()
                    .center(pos).radius(125)
                    .strokeWidth(4f)
                    .strokeColor(Color.argb(220, 0, 230, 80))
                    .fillColor(Color.argb(25, 0, 255, 80))
                    .zIndex(200));
        } else { myRadiusCircle.setCenter(pos); }

        if (checkinRadiusCircle == null) {
            checkinRadiusCircle = mMap.addCircle(new CircleOptions()
                    .center(pos).radius(activeRadius)
                    .strokeWidth(2f)
                    .strokeColor(boostRadius > 0f
                            ? Color.argb(200, 255, 220, 0)   // ブースト中：ゴールド
                            : Color.argb(140, 100, 200, 255)) // 通常：水色
                    .fillColor(boostRadius > 0f
                            ? Color.argb(15, 255, 220, 0)
                            : Color.argb(8, 100, 200, 255))
                    .zIndex(199));
        } else {
            checkinRadiusCircle.setCenter(pos);
            checkinRadiusCircle.setRadius(activeRadius);
            checkinRadiusCircle.setStrokeColor(boostRadius > 0f
                    ? Color.argb(200, 255, 220, 0)
                    : Color.argb(140, 100, 200, 255));
            checkinRadiusCircle.setFillColor(boostRadius > 0f
                    ? Color.argb(15, 255, 220, 0)
                    : Color.argb(8, 100, 200, 255));
        }
    }

    // =========================================================================
    // 名所スポット ▼ マーカー
    // =========================================================================
    private void addSpotMarkersToMap() {
        if (mMap == null) return;
        for (CityInfo city : gameData.cities) {
            for (SpotInfo spot : city.spots) {
                float hue = spot.isVisited
                        ? BitmapDescriptorFactory.HUE_YELLOW
                        : BitmapDescriptorFactory.HUE_AZURE;
                String snippet = spot.isVisited ? "✓ チェックイン済"
                        : city.name + "  ▼ 半径300m以内でチェックイン";
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(spot.lat, spot.lon))
                        .title(spot.name).snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        .zIndex(150));
                if (marker != null) {
                    spotMarkers.put(spot.id, marker);
                    // >>>test_make>>>
                    markerToSpotId.put(marker.getId(), spot.id); // デバッグ用逆引き
                    // <<<test_make<<<
                }
            }
        }
    }

    // >>>test_make>>>
    // =========================================================================
    // デバッグモード: 10連続タップ → 達成ON / 10秒ホールド → 達成OFF
    // =========================================================================
    private void setupDebugInteractions() {
        if (mMap == null) return;

        // ---- 10連続タップ → 達成フラグON ----
        mMap.setOnMarkerClickListener(marker -> {
            String spotId = markerToSpotId.get(marker.getId());
            if (spotId == null) return false; // スポット以外のマーカー

            long now  = System.currentTimeMillis();
            Long last = debugLastTapTime.get(spotId);
            // 前回タップから DEBUG_TAP_WINDOW_MS 以内なら継続カウント、超えたらリセット
            int count = (last != null && (now - last) < DEBUG_TAP_WINDOW_MS)
                    ? debugTapCount.getOrDefault(spotId, 0) + 1
                    : 1;
            debugTapCount.put(spotId, count);
            debugLastTapTime.put(spotId, now);

            // >>>test_make>>>
            // マーカー表示はユーザーモードのまま変更しない（DEBUG表示は出さない）
            // <<<test_make<<<

            if (count >= DEBUG_TAP_THRESHOLD) {
                debugTapCount.put(spotId, 0);
                onDebugSpotAchieve(spotId); // 達成フラグON
            }
            return false; // >>>test_make>>> false=通常のマーカー動作（infoWindow表示）を維持 <<<test_make<<<
        });

        // ---- 10秒ホールド → 達成フラグOFF ----
        // マップビューのタッチリスナーで ACTION_DOWN 時にタイマー開始、
        // 指を離したら (ACTION_UP/CANCEL) または大きく動かしたらキャンセル
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);
        View mapView = (mapFragment != null) ? mapFragment.getView() : null;
        if (mapView != null) {
            mapView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        debugHoldStartX = event.getX();
                        debugHoldStartY = event.getY();
                        startDebugHoldTimer(event.getX(), event.getY());
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        // 指が DEBUG_HOLD_SLOP 以上動いたらキャンセル
                        if (Math.abs(event.getX() - debugHoldStartX) > DEBUG_HOLD_SLOP ||
                            Math.abs(event.getY() - debugHoldStartY) > DEBUG_HOLD_SLOP) {
                            cancelDebugHold();
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        cancelDebugHold();
                        break;
                }
                return false; // false = マップにイベントを流す
            });
        }
    }

    /** ホールドタイマーをスタート（10秒後に最近傍スポットをリセット） */
    private void startDebugHoldTimer(float screenX, float screenY) {
        cancelDebugHold();
        debugHoldRunnable = () -> {
            if (mMap == null) return;
            // 画面座標 → LatLng に変換してスポットを探す
            android.graphics.Point pt =
                    new android.graphics.Point((int) screenX, (int) screenY);
            LatLng latLng = mMap.getProjection().fromScreenLocation(pt);
            SpotInfo target = findNearestSpotToLatLng(latLng, 0.006); // 約600m以内
            if (target != null) {
                onDebugSpotReset(target.id);
            } else {
                runOnUiThread(() -> Toast.makeText(this,
                        "[DEBUG] 近くにスポットが見つかりません", Toast.LENGTH_SHORT).show());
            }
        };
        debugHoldHandler.postDelayed(debugHoldRunnable, DEBUG_HOLD_MS);
    }

    /** ホールドタイマーをキャンセル */
    private void cancelDebugHold() {
        if (debugHoldRunnable != null) {
            debugHoldHandler.removeCallbacks(debugHoldRunnable);
            debugHoldRunnable = null;
        }
    }

    /** LatLng から指定距離(度)以内の最近傍スポットを返す */
    private SpotInfo findNearestSpotToLatLng(LatLng latLng, double thresholdDeg) {
        SpotInfo nearest = null;
        double minDist = thresholdDeg;
        for (CityInfo city : gameData.cities) {
            for (SpotInfo spot : city.spots) {
                double dist = Math.hypot(
                        spot.lat - latLng.latitude,
                        spot.lon - latLng.longitude);
                if (dist < minDist) { minDist = dist; nearest = spot; }
            }
        }
        return nearest;
    }

    /** [DEBUG] 名所の達成フラグをONにする */
    private void onDebugSpotAchieve(String spotId) {
        SpotInfo spot = gameData.findSpotById(spotId);
        if (spot == null) return;
        if (spot.isVisited) {
            Toast.makeText(this,
                    "[DEBUG] " + spot.name + " は既に達成済みです",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        spot.isVisited = true;
        db.addVisitedSpot(spotId);

        runOnUiThread(() -> {
            Marker m = spotMarkers.get(spotId);
            if (m != null) {
                m.setIcon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_YELLOW));
                m.setSnippet("✓ チェックイン済"); // >>>test_make>>> ユーザーモード表示を維持
            }
            Toast.makeText(this,
                    "[DEBUG] ★ " + spot.name + " 達成フラグ ON",
                    Toast.LENGTH_SHORT).show();

            CityInfo city = gameData.findCityById(spot.cityId);
            if (city != null) {
                // >>>test_make>>> 通常チェックインと同じ演出を実行
                showSpotCelebration(spot, city);
                // <<<test_make<<<
                if (city.checkAndUpdateCompletion()) {
                    onCityCompleted(city);
                }
            }
            cityAdapter.refreshData(gameData.cities);
            if (myLat != 0) updateDashboard(gameData.findNearestCity(myLat, myLon));
        });
    }

    /** [DEBUG] 名所の達成フラグをOFF（リセット）する */
    private void onDebugSpotReset(String spotId) {
        SpotInfo spot = gameData.findSpotById(spotId);
        if (spot == null) return;
        if (!spot.isVisited) {
            runOnUiThread(() -> Toast.makeText(this,
                    "[DEBUG] " + spot.name + " は未達成です",
                    Toast.LENGTH_SHORT).show());
            return;
        }

        spot.isVisited = false;
        db.removeVisitedSpot(spotId); // DB から削除

        // 所属する市町のコンプリートフラグもリセット
        CityInfo city = gameData.findCityById(spot.cityId);
        if (city != null) city.isCompleted = false;

        runOnUiThread(() -> {
            Marker m = spotMarkers.get(spotId);
            if (m != null) {
                m.setIcon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE));
                m.setSnippet(city != null
                        ? city.name + "  ▼ 半径300m以内でチェックイン"
                        : "未チェックイン [DEBUG]");
            }
            Toast.makeText(this,
                    "[DEBUG] ☆ " + spot.name + " 達成フラグ OFF（リセット）",
                    Toast.LENGTH_LONG).show();
            cityAdapter.refreshData(gameData.cities);
            if (myLat != 0) updateDashboard(gameData.findNearestCity(myLat, myLon));
        });
    }

    // >>>test_make>>>
    /**
     * [DEBUG] 探索率エリア10連打 → 全データ完全リセット
     * 名所訪問フラグOFF・探索マス=0・市町コンプリートOFF・霧を初期状態に戻す
     */
    private void onDebugFullReset() {
        // DB の訪問済みデータを全削除
        db.resetAll();

        // in-memory のゲームデータをリセット
        visitedMeshes.clear();
        completedCityMeshes.clear();
        completedCityIds.clear();
        isHeroMode = false;

        // GameData のスポット・市町フラグをリセット
        GameData.resetInstance();
        gameData = GameData.getInstance();

        // マーカーを全て水色（未訪問）に戻す
        for (CityInfo city : gameData.cities) {
            for (SpotInfo spot : city.spots) {
                Marker m = spotMarkers.get(spot.id);
                if (m != null) {
                    m.setIcon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE));
                    m.setSnippet(city.name + "  ▼ 半径300m以内でチェックイン");
                }
            }
        }

        // 霧を初期状態に戻す
        fogProvider.updateSelfWalkedMeshes(visitedMeshes);
        fogProvider.resetCompletedCityMeshes();
        fogOverlay.clearTileCache();

        // UI をリセット
        cityAdapter.refreshData(gameData.cities);
        updateDashboard(gameData.cities.get(0));
        updateExplorationRateUI();

        View root = findViewById(R.id.rootLayout);
        if (root != null) root.setBackgroundColor(
                getResources().getColor(R.color.bg_normal, null));
        setTitle("滋賀開拓ラリー");

        Toast.makeText(this,
                "[DEBUG] 全データリセット完了（名所訪問フラグOFF・探索マス=0）",
                Toast.LENGTH_LONG).show();
    }
    // <<<test_make<<<

    // =========================================================================
    // 魔王の霧エフェクト
    // =========================================================================
    private void setupMaouFogEffect() {
        if (mMap == null) return;

        double[][] layers = {
            {SHIGA_CENTER.latitude, SHIGA_CENTER.longitude, 120000, 12},
            {SHIGA_CENTER.latitude, SHIGA_CENTER.longitude,  85000, 20},
            {SHIGA_CENTER.latitude, SHIGA_CENTER.longitude,  55000, 28},
            {35.50, 136.25, 30000, 32}, {34.90, 136.00, 25000, 30},
            {35.28, 135.88, 22000, 28}, {35.10, 136.42, 20000, 26},
            {35.32, 136.08, 18000, 30},
            {MAOU_CASTLE.latitude, MAOU_CASTLE.longitude,  8000, 45},
            {MAOU_CASTLE.latitude, MAOU_CASTLE.longitude,  4000, 65},
            {MAOU_CASTLE.latitude, MAOU_CASTLE.longitude,  1800, 85},
            {MAOU_CASTLE.latitude, MAOU_CASTLE.longitude,   700,110},
        };

        maouBaseAlphas = new int[layers.length];
        for (int i = 0; i < layers.length; i++) {
            maouBaseAlphas[i] = (int) layers[i][3];
            Circle c = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(layers[i][0], layers[i][1]))
                    .radius(layers[i][2]).strokeWidth(0f)
                    .fillColor(Color.argb(maouBaseAlphas[i], 12, 0, 45))
                    .zIndex(90));
            maouFogCircles.add(c);
        }

        // OOM対策: setFillColor() は Google Maps への IPC 通信を伴うため
        // 毎フレーム（60fps）呼ぶとメモリが枯渇する。
        // 前回更新からアニメーション値が 0.025 以上変化したときだけ更新することで
        // 呼び出し頻度を約 1/6 に抑える（5秒サイクルで約40回→約7回に削減）。
        final float[] lastFogScale = {-1f};
        maouFogAnimator = ValueAnimator.ofFloat(0.45f, 1.0f, 0.45f);
        maouFogAnimator.setDuration(5000);
        maouFogAnimator.setRepeatCount(ValueAnimator.INFINITE);
        maouFogAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        maouFogAnimator.addUpdateListener(anim -> {
            float s = (float) anim.getAnimatedValue();
            // 変化量がしきい値未満なら IPC を発行しない（OOM 対策）
            if (Math.abs(s - lastFogScale[0]) < 0.025f) return;
            lastFogScale[0] = s;
            for (int i = 0; i < maouFogCircles.size(); i++) {
                int a = Math.min(200, (int)(maouBaseAlphas[i] * s));
                maouFogCircles.get(i).setFillColor(Color.argb(a, 12, 0, 45));
            }
        });
        maouFogAnimator.start();
    }

    // =========================================================================
    // 位置情報パーミッション
    // =========================================================================
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
            if (mMap != null) mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(this,
                    "位置情報の許可が必要です。設定から許可してください。", Toast.LENGTH_LONG).show();
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000).setMinUpdateDistanceMeters(10).build();
        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) onLocationUpdate(loc);
        });
    }

    // =========================================================================
    // デバッグリセット
    // =========================================================================
    private void showResetDialog() {
        new AlertDialog.Builder(this)
            .setTitle("デバッグ: データリセット")
            .setMessage("全ての訪問履歴を削除します。よろしいですか？")
            .setPositiveButton("リセット", (d, w) -> {
                db.resetAll();
                visitedMeshes.clear();
                completedCityMeshes.clear();
                completedCityIds.clear();
                isHeroMode = false;
                fogProvider.updateSelfWalkedMeshes(visitedMeshes);
                fogProvider.resetCompletedCityMeshes();
                fogOverlay.clearTileCache();
                GameData.resetInstance();
                gameData = GameData.getInstance();
                cityAdapter.refreshData(gameData.cities);
                updateDashboard(gameData.cities.get(0));
                updateExplorationRateUI();
                View root = findViewById(R.id.rootLayout);
                if (root != null) root.setBackgroundColor(
                        getResources().getColor(R.color.bg_normal, null));
                setTitle("滋賀開拓ラリー");
                Toast.makeText(this, "データをリセットしました", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("キャンセル", null).show();
    }

    // =========================================================================
    // ライフサイクル
    // =========================================================================
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) startLocationUpdates();
        if (maouFogAnimator != null && !maouFogAnimator.isRunning()) maouFogAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedClient != null && locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
        if (maouFogAnimator != null) maouFogAnimator.pause();
    }

    // =========================================================================
    // ③ クラッシュ対策: onDestroy でリソースを確実に解放
    // =========================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // LocationCallback の確実な解放
        // （onPause で解除済みだが、システムが onPause をスキップするケースに備えて二重解除）
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        // debugHoldHandler に溜まった遅延 Runnable をすべてキャンセル
        // （Handler が Activity の参照を保持し続けることによるリークを防止）
        if (debugHoldRunnable != null) {
            debugHoldHandler.removeCallbacks(debugHoldRunnable);
            debugHoldRunnable = null;
        }
        debugHoldHandler.removeCallbacksAndMessages(null);

        // ValueAnimator（魔王の霧）を停止・解放
        // （INFINITE アニメーションが Activity 破棄後も動き続けるとリークになる）
        if (maouFogAnimator != null) {
            maouFogAnimator.cancel();
            maouFogAnimator = null;
        }

        // 画面ロック WakeLock の解放（リーク防止）
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        // 探索モード用 WakeLock の解放
        if (exploreWakeLock != null && exploreWakeLock.isHeld()) {
            exploreWakeLock.release();
            exploreWakeLock = null;
        }
    }
}
