package tfsapps.shigadevelopment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 滋賀開拓ラリー メニュー画面
 *
 * 起動フロー：
 *   MenuActivity（タイトル/メニュー）
 *     ├── [探索] → MainActivity（地図ゲーム画面）
 *     └── [ヘルプ] → HelpActivity（遊び方説明）
 */
public class MenuActivity extends AppCompatActivity {

    private LinearLayout btnExplore;
    private LinearLayout btnHelp;
    private TextView tvMenuTitle;
    private TextView tvMenuSubtitle;
    private TextView tvMenuCatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // ビュー参照
        btnExplore    = findViewById(R.id.btnExplore);
        btnHelp       = findViewById(R.id.btnHelp);
        tvMenuTitle   = findViewById(R.id.tvMenuTitle);
        tvMenuSubtitle = findViewById(R.id.tvMenuSubtitle);
        tvMenuCatch   = findViewById(R.id.tvMenuCatch);

        // ボタンクリックリスナー
        btnExplore.setOnClickListener(v -> {
            animateButton(v, () -> {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        btnHelp.setOnClickListener(v -> {
            animateButton(v, () -> {
                Intent intent = new Intent(MenuActivity.this, HelpActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        });

        // 起動時のフェードイン演出
        playEntranceAnimation();
    }

    /**
     * 起動時アニメーション：タイトル・サブタイトル・キャッチコピー・ボタンが
     * 順番にフェードインしながら上からスライドして登場する
     */
    private void playEntranceAnimation() {
        // 初期状態：全要素を非表示
        View[] views = {tvMenuTitle, tvMenuSubtitle, tvMenuCatch, btnExplore, btnHelp};
        for (View v : views) {
            v.setAlpha(0f);
            v.setTranslationY(-40f);
        }

        // 段階的にフェードイン（各要素に遅延をつけて登場）
        long[] delays = {100, 300, 500, 700, 900};
        for (int i = 0; i < views.length; i++) {
            final View v = views[i];
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delays[i])
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }

        // タイトルの輝きアニメーション（繰り返し）
        if (tvMenuTitle != null) {
            ValueAnimator glowAnim = ValueAnimator.ofFloat(0.85f, 1.0f, 0.85f);
            glowAnim.setDuration(2500);
            glowAnim.setRepeatCount(ValueAnimator.INFINITE);
            glowAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            glowAnim.addUpdateListener(anim -> {
                float scale = (float) anim.getAnimatedValue();
                tvMenuTitle.setScaleX(scale);
                tvMenuTitle.setScaleY(scale);
            });
            glowAnim.setStartDelay(1200);
            glowAnim.start();
        }
    }

    /**
     * ボタン押下時のバウンスアニメーション
     * @param view  アニメーション対象ビュー
     * @param after アニメーション完了後の処理
     */
    private void animateButton(View view, Runnable after) {
        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(
            ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.93f, 1.05f, 1.0f),
            ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.93f, 1.05f, 1.0f)
        );
        anim.setDuration(250);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                after.run();
            }
        });
        anim.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // MainActivity から戻ってきたとき、ボタンが見えていることを保証
        if (btnExplore != null) btnExplore.setAlpha(1f);
        if (btnHelp    != null) btnHelp.setAlpha(1f);
    }
}
