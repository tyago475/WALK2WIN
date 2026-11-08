package pt.ubi.pdm.quizpenaltis.ui.help;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.util.SystemBars;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_help);

        SystemBars.applyPadding(findViewById(R.id.helpScroll));
        findViewById(R.id.btnBackToMain).setOnClickListener(v -> finish());
    }
}
