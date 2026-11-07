package pt.ubi.pdm.quizpenaltis.ui.player;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.model.Player;
import pt.ubi.pdm.quizpenaltis.model.PlayerCatalog;
import pt.ubi.pdm.quizpenaltis.ui.game.GameActivity;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PlayerSelectionActivity extends AppCompatActivity {

    private TextView tvAvailablePoints, tvSelectedPlayer;
    private Button btnConfirmSelection, btnBack;
    private ViewPager2 viewPagerPlayers;
    private TabLayout tabLayoutDots;

    private int availablePoints;
    private Player selectedPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_player_selection);

        availablePoints = getIntent().getIntExtra(Constants.EXTRA_AVAILABLE_POINTS, 1000);

        initializeViews();
        setupViewPager();
        updateUI();

        Toast.makeText(this, R.string.select_swipe_hint, Toast.LENGTH_SHORT).show();
    }

    private void initializeViews() {
        tvAvailablePoints = findViewById(R.id.tvAvailablePoints);
        tvSelectedPlayer = findViewById(R.id.tvSelectedPlayer);
        btnConfirmSelection = findViewById(R.id.btnConfirmSelection);
        btnBack = findViewById(R.id.btnBack);
        viewPagerPlayers = findViewById(R.id.viewPagerPlayers);
        tabLayoutDots = findViewById(R.id.tabLayoutDots);

        btnBack.setOnClickListener(v -> finish());
        btnConfirmSelection.setOnClickListener(v -> confirmSelection());
    }

    private void setupViewPager() {
        PlayerPagerAdapter adapter = new PlayerPagerAdapter(this, PlayerCatalog.ALL, availablePoints);
        viewPagerPlayers.setAdapter(adapter);

        // Os "pontinhos" por baixo do carrossel
        new TabLayoutMediator(tabLayoutDots, viewPagerPlayers, (tab, position) -> { }).attach();

        adapter.setOnPlayerSelectedListener(player -> {
            if (player.getCost() <= availablePoints) {
                selectedPlayer = player;
                updateUI();
                Toast.makeText(this, getString(R.string.select_player_selected, player.getName()),
                        Toast.LENGTH_SHORT).show();
            } else {
                showInsufficientPoints(player.getCost());
            }
        });
    }

    private void updateUI() {
        tvAvailablePoints.setText(getString(R.string.select_available_points, availablePoints));

        if (selectedPlayer != null) {
            tvSelectedPlayer.setText(getString(R.string.select_selected_player, selectedPlayer.getName()));
            tvSelectedPlayer.setVisibility(View.VISIBLE);
            btnConfirmSelection.setEnabled(true);
            btnConfirmSelection.setAlpha(1.0f);
        } else {
            tvSelectedPlayer.setVisibility(View.GONE);
            btnConfirmSelection.setEnabled(false);
            btnConfirmSelection.setAlpha(0.5f);
        }
    }

    private void confirmSelection() {
        if (selectedPlayer == null) return;

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(Constants.EXTRA_SELECTED_PLAYER, selectedPlayer.getName());
        intent.putExtra(Constants.EXTRA_PLAYER_COST, selectedPlayer.getCost());
        startActivity(intent);
        finish();
    }

    private void showInsufficientPoints(int requiredPoints) {
        int missingSteps = (requiredPoints - availablePoints) * Constants.STEPS_PER_POINT;
        Toast.makeText(this, getString(R.string.select_insufficient_points, missingSteps),
                Toast.LENGTH_LONG).show();
    }
}
