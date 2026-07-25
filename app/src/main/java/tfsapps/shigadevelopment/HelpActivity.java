package tfsapps.shigadevelopment;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 滋賀開拓ラリー ヘルプ画面
 *
 * 探索モードの遊び方をカード形式で説明する
 */
public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // ステータスバー（時刻・電波・バッテリー表示）とヘッダーバーの重なり対策。
        // Android 15(targetSdk 35)以降は edge-to-edge がデフォルトになるため、
        // ステータスバー分の高さだけヘッダーに paddingTop を動的に追加する。
        LinearLayout headerBar = findViewById(R.id.helpHeaderBar);
        ViewCompat.setOnApplyWindowInsetsListener(headerBar, (v, insets) -> {
            int statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarTop, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // 戻るボタン
        LinearLayout btnBack = findViewById(R.id.btnHelpBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> closeWithTransition());
        }

        // 戻るキー・戻るジェスチャー
        // targetSdk 36 (Android 16) からプリディクティブバックが既定で有効になるため、
        // 非推奨の onBackPressed() オーバーライドではなく OnBackPressedCallback を使う。
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeWithTransition();
            }
        });
    }

    private void closeWithTransition() {
        finish();
        overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right);
    }
}
