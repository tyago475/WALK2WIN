package pt.ubi.pdm.quizpenaltis;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PlayerPagerAdapter extends RecyclerView.Adapter<PlayerPagerAdapter.PlayerViewHolder> {

    private static final String TAG = "PlayerPagerAdapter";
    private Context context;
    private List<Player> players;
    private int availablePoints;
    private OnPlayerSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnPlayerSelectedListener {
        void onPlayerSelected(String playerName, int playerCost);
    }

    public PlayerPagerAdapter(Context context, List<Player> players, int availablePoints) {
        this.context = context;
        this.players = players != null ? players : new ArrayList<>();
        this.availablePoints = availablePoints;
    }

    public void setOnPlayerSelectedListener(OnPlayerSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition != -1 && oldPosition < getItemCount()) notifyItemChanged(oldPosition);
        if (position != -1 && position < getItemCount()) notifyItemChanged(position);
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_player_slide, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        try {
            // 🔥 VERIFICAÇÃO DE SEGURANÇA
            if (players == null || position < 0 || position >= players.size()) {
                Log.e(TAG, "Invalid player position: " + position);
                setupFallbackView(holder);
                return;
            }

            Player player = players.get(position);
            if (player == null) {
                Log.e(TAG, "Player is null at position: " + position);
                setupFallbackView(holder);
                return;
            }


            if (holder.ivPlayerPhoto != null) {
                int imageResource = player.getImageResource();
                if (imageResource != 0) {
                    holder.ivPlayerPhoto.setImageResource(imageResource);
                } else {

                    holder.ivPlayerPhoto.setImageResource(R.drawable.cr7); // ou outra imagem que você tenha
                }
            }


            if (holder.tvPlayerName != null) {
                String playerName = player.getName();
                holder.tvPlayerName.setText(playerName != null ? playerName : "Jogador");
            }


            if (holder.tvPlayerPrice != null) {
                holder.tvPlayerPrice.setText(String.valueOf(player.getCost()));
            }


            if (holder.tvPlayerStatus != null) {
                String difficulty = player.getDifficulty();
                holder.tvPlayerStatus.setText(difficulty != null ? difficulty : "Normal");
            }


            boolean canSelect = player.getCost() <= availablePoints;
            if (holder.itemView != null) {
                holder.itemView.setAlpha(canSelect ? 1.0f : 0.6f);
            }


            boolean isSelected = position == selectedPosition;
            updateSelectionUI(holder, isSelected, canSelect);


            if (holder.itemView != null) {
                holder.itemView.setOnClickListener(v -> {
                    if (canSelect && listener != null) {
                        setSelectedPosition(position);
                        listener.onPlayerSelected(player.getName(), player.getCost());
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onBindViewHolder: " + e.getMessage(), e);
            setupFallbackView(holder);
        }
    }

    private void setupFallbackView(@NonNull PlayerViewHolder holder) {
        try {
            if (holder.tvPlayerName != null) {
                holder.tvPlayerName.setText("Jogador");
            }
            if (holder.tvPlayerPrice != null) {
                holder.tvPlayerPrice.setText("0");
            }
            if (holder.tvPlayerStatus != null) {
                holder.tvPlayerStatus.setText("Normal");
            }
            if (holder.ivPlayerPhoto != null) {

                holder.ivPlayerPhoto.setImageResource(R.drawable.cr7);
            }
            if (holder.itemView != null) {
                holder.itemView.setAlpha(0.6f);
            }
            updateSelectionUI(holder, false, false);
        } catch (Exception e) {
            Log.e(TAG, "Error in setupFallbackView: " + e.getMessage(), e);
        }
    }

    private void updateSelectionUI(@NonNull PlayerViewHolder holder, boolean isSelected, boolean canSelect) {
        try {
            if (holder.cardPlayer != null) {
                if (isSelected && canSelect) {
                    holder.cardPlayer.setBackgroundResource(R.drawable.player_card_selected);
                    if (holder.tvSelectedBadge != null) {
                        holder.tvSelectedBadge.setVisibility(View.VISIBLE);
                    }

                    holder.cardPlayer.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
                } else {
                    holder.cardPlayer.setBackgroundResource(R.drawable.player_card_normal);
                    if (holder.tvSelectedBadge != null) {
                        holder.tvSelectedBadge.setVisibility(View.INVISIBLE);
                    }
                    holder.cardPlayer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateSelectionUI: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return players != null ? players.size() : 0;
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        CardView cardPlayer;
        ImageView ivPlayerPhoto;
        TextView tvPlayerName, tvPlayerPrice, tvPlayerStatus, tvSelectedBadge;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                cardPlayer = itemView.findViewById(R.id.cardPlayer);
                ivPlayerPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
                tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
                tvPlayerPrice = itemView.findViewById(R.id.tvPlayerPrice);
                tvPlayerStatus = itemView.findViewById(R.id.tvPlayerStatus);
                tvSelectedBadge = itemView.findViewById(R.id.tvSelectedBadge);

                // VERIFICAR SE TODOS OS VIEWS FORAM ENCONTRADOS
                if (cardPlayer == null) Log.e(TAG, "cardPlayer not found");
                if (ivPlayerPhoto == null) Log.e(TAG, "ivPlayerPhoto not found");
                if (tvPlayerName == null) Log.e(TAG, "tvPlayerName not found");
                if (tvPlayerPrice == null) Log.e(TAG, "tvPlayerPrice not found");
                if (tvPlayerStatus == null) Log.e(TAG, "tvPlayerStatus not found");
                if (tvSelectedBadge == null) Log.e(TAG, "tvSelectedBadge not found");

            } catch (Exception e) {
                Log.e(TAG, "Error in PlayerViewHolder constructor: " + e.getMessage(), e);
            }
        }
    }

    // MÉTODO PARA ATUALIZAR OS DADOS
    public void updatePlayers(List<Player> newPlayers) {
        this.players = newPlayers != null ? newPlayers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateAvailablePoints(int points) {
        this.availablePoints = points;
        notifyDataSetChanged();
    }
}