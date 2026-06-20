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

import androidx.appcompat.app.AppCompatActivity;

/**
 * しがのあさぼらけ — ストーリーエピローグ（エンディング）画面
 *
 * 全19市町を制覇した時に表示される。
 * テキストを段階的にフェードインさせ、霧が晴れる朝の演出を行う。
 * 「冒険を続ける」ボタンで MainActivity へ戻る。
 */
public class StoryEpilogueActivity extends AppCompatActivity {

    // 各テキストブロックの遅延時間（ms）
    private static final long DELAY_TITLE       =  300L;
    private static final long DELAY_GLOW        =  800L;
    private static final long DELAY_TEXT1       = 1500L;
    private static final long DELAY_TEXT2       = 3500L;
    private static final long DELAY_TEXT3       = 6000L;
    private static final long DELAY_EMOJI       = 8000L;
    private static final long DELAY_DIVIDER     = 8800L;
    private static final long DELAY_TITLE_CARD  = 9200L;
    private static final long DELAY_TEXT4       = 11000L;
    private static final long DELAY_BUTTON      = 13000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // 朝焼けグロウアニメーター
    private ValueAnimator glowAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epilogue);

        // ステータスバーを透過させて没入感を高める
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // ビュー参照
        View         glowOverlay       = findViewById(R.id.epilogueGlowOverlay);
        View         titlePanel        = findViewById(R.id.epilogueTitlePanel);
        View         dividerTop        = findViewById(R.id.epilogueDividerTop);
        View         text1             = findViewById(R.id.epilogueText1);
        View         text2             = findViewById(R.id.epilogueText2);
        View         text3             = findViewById(R.id.epilogueText3);
        View         morningEmoji      = findViewById(R.id.epilogueMorningEmoji);
        View         divider           = findViewById(R.id.epilogueDivider);
        View         titleCard         = findViewById(R.id.layoutEpilogueTitle);
        View         text4             = findViewById(R.id.epilogueText4);
        Button       btnContinue       = findViewById(R.id.btnContinueAdventure);

        // ── タイトルパネル登場 ──────────────────────────────────────
        scheduleAppear(titlePanel, DELAY_TITLE, 900);
        scheduleAppear(dividerTop, DELAY_TITLE + 700, 600);

        // ── 朝焼けグロウ アニメーション（遅延起動） ────────────────────
        if (glowOverlay != null) {
            handler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                glowAnimator = ValueAnimator.ofFloat(0f, 0.7f, 0.45f, 0.7f, 0.45f);
                glowAnimator.setDuration(12000L);
                glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
                glowAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                glowAnimator.addUpdateListener(
                        anim -> glowOverlay.setAlpha((Float) anim.getAnimatedValue()));
                glowAnimator.start();
            }, DELAY_GLOW);
        }

        // ── テキスト段階的フェードイン ──────────────────────────────────
        scheduleAppear(text1,       DELAY_TEXT1,      900);
        scheduleAppear(text2,       DELAY_TEXT2,      900);
        scheduleAppear(text3,       DELAY_TEXT3,      900);
        scheduleAppear(morningEmoji,DELAY_EMOJI,      700);
        scheduleAppear(divider,     DELAY_DIVIDER,    600);
        scheduleAppear(titleCard,   DELAY_TITLE_CARD, 1000);
        scheduleAppear(text4,       DELAY_TEXT4,      900);

        // ── 「冒険を続ける」ボタン登場 ─────────────────────────────────
        handler.postDelayed(() -> {
            if (btnContinue == null) return;
            if (isFinishing() || isDestroyed()) return;

            btnContinue.setVisibility(View.VISIBLE);
            btnContinue.setAlpha(0f);
            btnContinue.setScaleX(0.7f);
            btnContinue.setScaleY(0.7f);

            AnimatorSet anim = new AnimatorSet();
            anim.playTogether(
                    ObjectAnimator.ofFloat(btnContinue, "alpha",  0f, 1f),
                    ObjectAnimator.ofFloat(btnContinue, "scaleX", 0.7f, 1.05f, 1.0f),
                    ObjectAnimator.ofFloat(btnContinue, "scaleY", 0.7f, 1.05f, 1.0f));
            anim.setDuration(700);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();

            // ボタン光彩パルス（繰り返し）
            ValueAnimator btnPulse = ValueAnimator.ofFloat(1.0f, 1.04f, 1.0f);
            btnPulse.setDuration(1800);
            btnPulse.setRepeatCount(ValueAnimator.INFINITE);
            btnPulse.setInterpolator(new AccelerateDecelerateInterpolator());
            btnPulse.addUpdateListener(a -> {
                float s = (Float) a.getAnimatedValue();
                if (btnContinue != null) {
                    btnContinue.setScaleX(s);
                    btnContinue.setScaleY(s);
                }
            });
            btnPulse.setStartDelay(900);
            btnPulse.start();

        }, DELAY_BUTTON);

        // ── ボタンクリックリスナー ──────────────────────────────────────
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> returnToMainActivity());
        }
    }

    /**
     * 指定ビューをフェードイン＋上スライドで登場させる
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
     * フェードアウト演出で MainActivity へ戻る
     */
    private void returnToMainActivity() {
        View root = findViewById(R.id.epilogueRoot);
        if (root != null) {
            root.animate()
                    .alpha(0f)
                    .setDuration(600)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(this::startMainActivity)
                    .start();
        } else {
            startMainActivity();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(StoryEpilogueActivity.this, MainActivity.class);
        // 既存のMainActivityスタックをクリアして新規起動
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        // バックキーで誤って抜けないよう、ボタン経由のみで戻れる
        returnToMainActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (glowAnimator != null) {
            glowAnimator.cancel();
            glowAnimator = null;
        }
    }
}
