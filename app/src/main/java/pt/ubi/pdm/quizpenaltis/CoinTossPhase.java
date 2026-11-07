package pt.ubi.pdm.quizpenaltis;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;

public class CoinTossPhase {

    private GameActivity activity;
    private LinearLayout layoutCoinToss;
    private Button btnHeads;
    private Button btnTails;
    private TextView tvCoinTossTitle;
    private TextView tvCoinTossMessage;
    private TextView tvCoinAnimation;

    private CoinTossListener listener;

    public interface CoinTossListener {
        void onCoinTossResult(boolean userWonToss);
    }

    public CoinTossPhase(GameActivity activity, CoinTossListener listener) {
        this.activity = activity;
        this.listener = listener;
        initializeViews();
    }

    private void initializeViews() {
        layoutCoinToss = (LinearLayout) activity.findViewById(R.id.coinTossPhase);

        if (layoutCoinToss == null) {
            android.util.Log.e("CoinTossPhase", "ERRO: layoutCoinToss não encontrado!");
            return;
        }

        btnHeads = layoutCoinToss.findViewById(R.id.btnHeads);
        btnTails = layoutCoinToss.findViewById(R.id.btnTails);
        tvCoinTossTitle = layoutCoinToss.findViewById(R.id.tvCoinTossTitle);
        tvCoinTossMessage = layoutCoinToss.findViewById(R.id.tvCoinTossMessage);
        tvCoinAnimation = layoutCoinToss.findViewById(R.id.tvCoinAnimation);

        setupListeners();
    }

    private void setupListeners() {
        if (btnHeads != null) {
            btnHeads.setOnClickListener(v -> handleCoinTossChoice(true));
        }

        if (btnTails != null) {
            btnTails.setOnClickListener(v -> handleCoinTossChoice(false));
        }
    }

    private void handleCoinTossChoice(boolean userChoseHeads) {
        setButtonsEnabled(false);

        boolean coinIsHeads = Math.random() > 0.5;
        boolean userWonToss = (userChoseHeads && coinIsHeads) || (!userChoseHeads && !coinIsHeads);

        showCoinTossResult(userChoseHeads, coinIsHeads, userWonToss);

        new Handler().postDelayed(() -> {
            if (listener != null) {
                listener.onCoinTossResult(userWonToss);
            }
        }, 2500);
    }

    private void showCoinTossResult(boolean userChoseHeads, boolean coinIsHeads, boolean userWonToss) {
        activity.runOnUiThread(() -> {
            String userChoice = userChoseHeads ? "CARA" : "COROA";
            String coinResult = coinIsHeads ? "CARA" : "COROA";
            String resultText = userWonToss ? "VOCÊ GANHOU! 🎉" : "COMPUTADOR GANHOU! 😞";
            String whoStarts = userWonToss ? "Você começa primeiro!" : "Computador começa primeiro!";

            if (tvCoinTossTitle != null) {
                tvCoinTossTitle.setText("RESULTADO DO LANÇAMENTO");
            }
            if (tvCoinTossMessage != null) {
                tvCoinTossMessage.setText(
                        "Sua escolha: " + userChoice + "\n" +
                                "Resultado: " + coinResult + "\n\n" +
                                resultText + "\n\n" +
                                whoStarts
                );
            }

            animateCoinFlip(coinIsHeads);

            if (userChoseHeads) {
                if (btnHeads != null) {
                    if (coinIsHeads) {
                        btnHeads.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                        btnHeads.setText("✅ CARA");
                    } else {
                        btnHeads.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                        btnHeads.setText("❌ CARA");
                    }
                }
                if (btnTails != null) {
                    btnTails.setBackgroundColor(activity.getResources().getColor(android.R.color.darker_gray));
                    btnTails.setText("COROA");
                }
            } else {
                if (btnTails != null) {
                    if (!coinIsHeads) {
                        btnTails.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                        btnTails.setText("✅ COROA");
                    } else {
                        btnTails.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                        btnTails.setText("❌ COROA");
                    }
                }
                if (btnHeads != null) {
                    btnHeads.setBackgroundColor(activity.getResources().getColor(android.R.color.darker_gray));
                    btnHeads.setText("CARA");
                }
            }
        });
    }

    private void animateCoinFlip(boolean finalIsHeads) {
        if (tvCoinAnimation != null) {
            tvCoinAnimation.animate()
                    .rotationYBy(360)
                    .setDuration(1000)
                    .start();

            new Handler().postDelayed(() -> {
                activity.runOnUiThread(() -> {
                    // Cara 😊 ou Coroa 👑
                    tvCoinAnimation.setText(finalIsHeads ? "😊" : "👑");

                    tvCoinAnimation.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(200)
                            .withEndAction(() -> {
                                tvCoinAnimation.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(200)
                                        .start();
                            })
                            .start();
                });
            }, 500);
        }
    }

    public void show() {
        if (layoutCoinToss != null) {
            activity.runOnUiThread(() -> {
                layoutCoinToss.setVisibility(View.VISIBLE);
                resetUI();
            });
        }
    }

    public void hide() {
        if (layoutCoinToss != null) {
            activity.runOnUiThread(() -> {
                layoutCoinToss.setVisibility(View.GONE);
            });
        }
    }

    private void resetUI() {
        if (tvCoinTossTitle != null) {
            tvCoinTossTitle.setText("LANÇAMENTO DA MOEDA");
        }
        if (tvCoinTossMessage != null) {
            tvCoinTossMessage.setText("Escolha Cara ou Coroa para determinar quem começa primeiro:");
        }

        setButtonsEnabled(true);

        if (btnHeads != null) {
            btnHeads.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_red_dark));
            btnHeads.setText("CARA");
        }
        if (btnTails != null) {
            btnTails.setBackgroundColor(activity.getResources().getColor(android.R.color.holo_red_dark));
            btnTails.setText("COROA");
        }
        if (tvCoinAnimation != null) {
            tvCoinAnimation.setText("🪙");
            tvCoinAnimation.setRotationY(0);
            tvCoinAnimation.setScaleX(1.0f);
            tvCoinAnimation.setScaleY(1.0f);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnHeads != null) {
            btnHeads.setEnabled(enabled);
        }
        if (btnTails != null) {
            btnTails.setEnabled(enabled);
        }
    }
}