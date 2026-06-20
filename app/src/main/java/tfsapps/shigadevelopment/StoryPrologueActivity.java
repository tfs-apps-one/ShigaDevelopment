package tfsapps.shigadevelopment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * しがのあさぼらけ — ストーリープロローグ画面
 *
 * 探索モード（MainActivity）の初回起動時にのみ表示される。
 * テキストを段階的にフェードインさせ、世界観を演出する。
 * 「旅を始める」ボタンで MainActivity へ遷移し、
 * 以降は表示されない（DB フラグで管理）。
 */
public class StoryPrologueActivity extends AppCompatActivity {

    // 各テキストブロックの遅延時間（ms）
    private static final long DELAY_TITLE      =  400L;
    private static final long DELAY_TEXT1      = 1200L;
    private static final long DELAY_TEXT2      = 3200L;
    private static final long DELAY_TEXT3      = 5400L;
    private static final long DELAY_DIVIDER    = 7600L;
    private static final long DELAY_MISSION    = 8000L;
    private static final long DELAY_BUTTON     = 9800L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // 霧パルスアニメーター（背景紫霧の呼吸）
    private ValueAnimator fogAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prologue);

        // ステータスバーを透過させて没入感を高める
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // ビュー参照
        // ★ 新レイアウト: タイトルブロック全体がLinearLayout（id=tvPrologueShrineEmoji）
        View         fogOverlay      = findViewById(R.id.prologueFogOverlay);
        View         particleLayer   = findViewById(R.id.prologueParticleLayer);
        View         titlePanel      = findViewById(R.id.tvPrologueShrineEmoji); // タイトルパネル全体
        View         dividerTop      = findViewById(R.id.prologueDividerTop);
        View         tvPrologueText1 = findViewById(R.id.tvPrologueText1);
        View         tvPrologueText2 = findViewById(R.id.tvPrologueText2);
        View         nightEmoji      = findViewById(R.id.tvPrologueNightEmoji);
        View         tvPrologueText3 = findViewById(R.id.tvPrologueText3);
        View         divider         = findViewById(R.id.prologueDivider);
        LinearLayout layoutMission   = findViewById(R.id.layoutMission);
        Button       btnStartJourney = findViewById(R.id.btnStartJourney);
        Button       btnSkipPrologue = findViewById(R.id.btnSkipPrologue);

        // ── 背景霧パルスアニメーション ──────────────────────────────────
        if (fogOverlay != null) {
            fogAnimator = ValueAnimator.ofFloat(0.55f, 0.85f, 0.55f);
            fogAnimator.setDuration(4000L);
            fogAnimator.setRepeatCount(ValueAnimator.INFINITE);
            fogAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            fogAnimator.addUpdateListener(anim -> fogOverlay.setAlpha((Float) anim.getAnimatedValue()));
            fogAnimator.start();
        }

        // ── パーティクル（揺らぎアニメーション） ─────────────────────────
        if (particleLayer != null) {
            ValueAnimator particleAnim = ValueAnimator.ofFloat(-8f, 8f, -8f);
            particleAnim.setDuration(6000L);
            particleAnim.setRepeatCount(ValueAnimator.INFINITE);
            particleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            particleAnim.addUpdateListener(anim -> {
                float v = (Float) anim.getAnimatedValue();
                particleLayer.setTranslationX(v * 0.5f);
                particleLayer.setTranslationY(v);
            });
            particleAnim.start();
        }

        // ── テキスト段階的フェードイン ──────────────────────────────────
        scheduleAppear(titlePanel,      DELAY_TITLE,        700);
        scheduleAppear(dividerTop,      DELAY_TITLE + 600,  500);
        scheduleAppear(tvPrologueText1, DELAY_TEXT1,        800);
        scheduleAppear(tvPrologueText2, DELAY_TEXT2,        800);
        scheduleAppear(nightEmoji,      DELAY_TEXT2 + 1500, 700);
        scheduleAppear(tvPrologueText3, DELAY_TEXT3,        800);
        scheduleAppear(divider,         DELAY_DIVIDER,      600);
        scheduleAppear(layoutMission,   DELAY_MISSION,      900);

        // ── 「旅を始める」ボタン登場 ─────────────────────────────────────
        handler.postDelayed(() -> {
            if (btnStartJourney == null) return;
            btnStartJourney.setVisibility(View.VISIBLE);
            btnStartJourney.setAlpha(0f);
            btnStartJourney.setScaleX(0.7f);
            btnStartJourney.setScaleY(0.7f);
            AnimatorSet anim = new AnimatorSet();
            anim.playTogether(
                    ObjectAnimator.ofFloat(btnStartJourney, "alpha",  0f, 1f),
                    ObjectAnimator.ofFloat(btnStartJourney, "scaleX", 0.7f, 1.05f, 1.0f),
                    ObjectAnimator.ofFloat(btnStartJourney, "scaleY", 0.7f, 1.05f, 1.0f));
            anim.setDuration(700);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();

            // ボタン光彩パルス（繰り返し）
            ValueAnimator btnPulse = ValueAnimator.ofFloat(1.0f, 1.04f, 1.0f);
            btnPulse.setDuration(1600);
            btnPulse.setRepeatCount(ValueAnimator.INFINITE);
            btnPulse.setInterpolator(new AccelerateDecelerateInterpolator());
            btnPulse.addUpdateListener(a -> {
                float s = (Float) a.getAnimatedValue();
                btnStartJourney.setScaleX(s);
                btnStartJourney.setScaleY(s);
            });
            btnPulse.setStartDelay(800);
            btnPulse.start();

        }, DELAY_BUTTON);

        // ── スキップボタン（すぐに表示） ────────────────────────────────
        handler.postDelayed(() -> {
            if (btnSkipPrologue != null) {
                btnSkipPrologue.setVisibility(View.VISIBLE);
                btnSkipPrologue.animate().alpha(1f).setDuration(400).start();
            }
        }, 1000L);

        // ── ボタンクリックリスナー ──────────────────────────────────────
        if (btnStartJourney != null) {
            btnStartJourney.setOnClickListener(v -> launchMainActivity());
        }
        if (btnSkipPrologue != null) {
            btnSkipPrologue.setOnClickListener(v -> launchMainActivity());
        }
    }

    /**
     * 指定ビューをフェードイン＋上スライドで登場させる
     *
     * @param view     対象ビュー
     * @param delayMs  遅延時間 (ms)
     * @param durationMs アニメーション時間 (ms)
     */
    private void scheduleAppear(View view, long delayMs, long durationMs) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(30f);
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(0)
                    .setDuration(durationMs)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }, delayMs);
    }

    /**
     * DB にプロローグ表示済みフラグを立てて MainActivity へ遷移する
     */
    private void launchMainActivity() {
        // フラグを保存（次回以降はスキップ）
        DatabaseHelper.getInstance(this).setPrologueShown();

        // 霧が晴れる演出（白いフラッシュ）→ 遷移
        View root = findViewById(R.id.prologueRoot);
        if (root != null) {
            root.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(this::startMapActivity)
                    .start();
        } else {
            startMapActivity();
        }
    }

    private void startMapActivity() {
        Intent intent = new Intent(StoryPrologueActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (fogAnimator != null) {
            fogAnimator.cancel();
            fogAnimator = null;
        }
    }
}
