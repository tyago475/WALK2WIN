package pt.ubi.pdm.quizpenaltis;

import android.annotation.SuppressLint;
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
import java.util.ArrayList;
import java.util.List;

public class PlayerSelectionActivity extends AppCompatActivity {

    private static final int STEPS_MULTIPLIER = 10;

    private TextView tvAvailablePoints, tvSelectedPlayer;
    private Button btnConfirmSelection, btnBack;
    private ViewPager2 viewPagerPlayers;
    private TabLayout tabLayoutDots;

    private int availablePoints;
    private String selectedPlayer = null;
    private int selectedPlayerCost = 0;

    // Dados dos jogadores
    private final String[] playerNames = {
            "Maguire",          // 100 - Mais barato
            "Ricardo Esgaio",   // 200
            "Taremi",           // 300
            "João Félix",       // 400
            "João Moutinho",    // 500
            "Lamine Yamal",     // 600
            "Nuno Mendes",      // 700
            "Kylian Mbappe",    // 800
            "Messi",            // 900
            "CR7"               // 1000 - Mais caro
    };

    private final int[] playerCosts = {
            100,   // Maguire
            200,   // Ricardo Esgaio
            300,   // Taremi
            400,   // João Félix
            500,   // João Moutinho
            600,   // Lamine Yamal
            700,   // Nuno Mendes
            800,   // Kylian Mbappe
            900,   // Messi
            1000   // CR7
    };

    private final int[] playerImages = {
            R.drawable.maguire,          // Maguire
            R.drawable.ricardo_esgaio,   // Ricardo Esgaio
            R.drawable.taremi,           // Taremi
            R.drawable.joao_felix,       // João Félix
            R.drawable.joao_moutinho,    // João Moutinho
            R.drawable.lamine_yamal,     // Lamine Yamal
            R.drawable.nuno_mendes,      // Nuno Mendes
            R.drawable.mbappe,           // Kylian Mbappe
            R.drawable.messi,            // Messi
            R.drawable.cr7               // CR7
    };

    private final String[] playerDifficulties = {
            "Muito Difícil",    // Maguire - 100
            "Difícil",          // Ricardo Esgaio - 200
            "Difícil",          // Taremi - 300
            "Médio",            // João Félix - 400
            "Médio",            // João Moutinho - 500
            "Médio",            // Lamine Yamal - 600
            "Fácil",            // Nuno Mendes - 700
            "Fácil",            // Kylian Mbappe - 800
            "Muito Fácil",      // Messi - 900
            "Super Fácil 🐐"    // CR7 - 1000
    };

    private PlayerPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_player_selection);

        // Receber pontos da MainActivity
        Intent intent = getIntent();
        availablePoints = intent.getIntExtra("PONTOS_DISPONIVEIS", 1000);

        initializeViews();
        setupViewPager();
        updateUI();

        Toast.makeText(this, "Desliza para ver os jogadores!", Toast.LENGTH_SHORT).show();
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
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playerNames.length; i++) {
            players.add(new Player(playerNames[i], playerCosts[i], playerImages[i], playerDifficulties[i]));
        }

        adapter = new PlayerPagerAdapter(this, players, availablePoints);
        viewPagerPlayers.setAdapter(adapter);

        // Configurar os dots indicator
        new TabLayoutMediator(tabLayoutDots, viewPagerPlayers, (tab, position) -> {
            // Tab vazia para mostrar apenas os dots
        }).attach();

        // Listener para quando o jogador é selecionado
        adapter.setOnPlayerSelectedListener((playerName, playerCost) -> {
            if (playerCost <= availablePoints) {
                selectedPlayer = playerName;
                selectedPlayerCost = playerCost;
                updateUI();
                Toast.makeText(PlayerSelectionActivity.this,
                        playerName + " selecionado! ✅", Toast.LENGTH_SHORT).show();
            } else {
                showInsufficientPoints(playerCost);
            }
        });
    }

    private void updateUI() {
        tvAvailablePoints.setText("Pontos Disponíveis: " + availablePoints);

        if (selectedPlayer != null) {
            tvSelectedPlayer.setText("Jogador selecionado: " + selectedPlayer);
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
        if (selectedPlayer != null) {
            String difficulty = "Médio";
            for (int i = 0; i < playerNames.length; i++) {
                if (playerNames[i].equals(selectedPlayer)) {
                    difficulty = playerDifficulties[i];
                    break;
                }
            }

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("SELECTED_PLAYER", selectedPlayer);
            intent.putExtra("DIFFICULTY", difficulty);
            intent.putExtra("PLAYER_COST", selectedPlayerCost);
            startActivity(intent);

            // Fecha a activity de seleção
            finish();
        }
    }
    private void showInsufficientPoints(int requiredPoints) {
        int missingSteps = (requiredPoints - availablePoints) * STEPS_MULTIPLIER;
        String message = String.format(
                "Pontos insuficientes! Ande mais %d passos!",
                missingSteps
        );
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}