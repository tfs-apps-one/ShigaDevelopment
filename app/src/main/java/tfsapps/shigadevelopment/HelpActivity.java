package tfsapps.shigadevelopment;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

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

        // 戻るボタン
        LinearLayout btnBack = findViewById(R.id.btnHelpBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right);
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right);
    }
}
