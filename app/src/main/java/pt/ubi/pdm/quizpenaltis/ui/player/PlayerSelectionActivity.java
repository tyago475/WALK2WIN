package pt.ubi.pdm.quizpenaltis.ui.player;

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

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.model.Player;
import pt.ubi.pdm.quizpenaltis.model.PlayerCatalog;
import pt.ubi.pdm.quizpenaltis.steps.StepCounter;
import pt.ubi.pdm.quizpenaltis.ui.game.GameActivity;
import pt.ubi.pdm.quizpenaltis.util.Constants;

/** Escolha do jogador. Confirmar a escolha gasta os pontos do jogador escolhido. */
public class PlayerSelectionActivity extends AppCompatActivity {

    private TextView tvAvailablePoints, tvSelectedPlayer;
    private Button btnConfirmSelection, btnBack;
    private ViewPager2 viewPagerPlayers;
    private TabLayout tabLayoutDots;
    private PlayerPagerAdapter adapter;

    private int availablePoints;
    private Player selectedPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_player_selection);

        // O saldo vem sempre da fonte real (StepCounter), nunca de um valor copiado de outro ecrã
        availablePoints = StepCounter.getTodayPoints(this);

        initializeViews();
        setupViewPager();
        updateUI();

        Toast.makeText(this, R.string.select_swipe_hint, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBalance();
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
        adapter = new PlayerPagerAdapter(this, PlayerCatalog.ALL, availablePoints);
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

    /** Relê o saldo (pode ter mudado: passos novos, meia-noite) e retira escolhas que já não se pagam. */
    private void refreshBalance() {
        int current = StepCounter.getTodayPoints(this);
        if (current == availablePoints) return;

        availablePoints = current;
        adapter.setAvailablePoints(current);
        if (selectedPlayer != null && selectedPlayer.getCost() > current) {
            selectedPlayer = null;
            adapter.clearSelection();
        }
        updateUI();
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

        // Desativa já o botão: um segundo toque não pode cobrar outra vez
        btnConfirmSelection.setEnabled(false);

        if (!StepCounter.spendPoints(this, selectedPlayer.getCost())) {
            // O saldo mudou desde que o jogador foi escolhido (por exemplo, passou a meia-noite)
            int cost = selectedPlayer.getCost();
            selectedPlayer = null;
            adapter.clearSelection();
            availablePoints = StepCounter.getTodayPoints(this);
            adapter.setAvailablePoints(availablePoints);
            updateUI();
            showInsufficientPoints(cost);
            return;
        }

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(Constants.EXTRA_SELECTED_PLAYER, selectedPlayer.getName());
        intent.putExtra(Constants.EXTRA_PLAYER_COST, selectedPlayer.getCost());
        startActivity(intent);
        finish();
    }

    private void showInsufficientPoints(int requiredPoints) {
        int missingSteps = Math.max(0, requiredPoints - availablePoints) * Constants.STEPS_PER_POINT;
        Toast.makeText(this, getString(R.string.select_insufficient_points, missingSteps),
                Toast.LENGTH_LONG).show();
    }
}
