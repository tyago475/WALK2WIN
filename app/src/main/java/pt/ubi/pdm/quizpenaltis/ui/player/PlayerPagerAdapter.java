package pt.ubi.pdm.quizpenaltis.ui.player;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.model.Player;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Carrossel de jogadores do ecrã de seleção. */
public class PlayerPagerAdapter extends RecyclerView.Adapter<PlayerPagerAdapter.PlayerViewHolder> {

    private static final float ALPHA_LOCKED = 0.6f;
    private static final float SELECTED_SCALE = 1.02f;
    private static final long SCALE_ANIMATION_MS = 200;

    private final Context context;
    private final List<Player> players;
    private int availablePoints;
    private OnPlayerSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnPlayerSelectedListener {
        void onPlayerSelected(Player player);
    }

    public PlayerPagerAdapter(Context context, List<Player> players, int availablePoints) {
        this.context = context;
        this.players = players;
        this.availablePoints = availablePoints;
    }

    public void setOnPlayerSelectedListener(OnPlayerSelectedListener listener) {
        this.listener = listener;
    }

    /** Atualiza o saldo e redesenha, para os jogadores que já não se pagam ficarem bloqueados. */
    public void setAvailablePoints(int points) {
        availablePoints = points;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    private void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_player_slide, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        boolean canSelect = player.getCost() <= availablePoints;

        holder.ivPlayerPhoto.setImageResource(player.getImageResource());
        holder.tvPlayerName.setText(player.getName());
        holder.tvPlayerPrice.setText(String.valueOf(player.getCost()));
        holder.tvPlayerStatus.setText(player.getDifficultyRes());
        holder.itemView.setAlpha(canSelect ? 1.0f : ALPHA_LOCKED);

        updateSelectionUI(holder, position == selectedPosition && canSelect);

        holder.itemView.setOnClickListener(v -> {
            if (!canSelect || listener == null) return;
            setSelectedPosition(holder.getBindingAdapterPosition());
            listener.onPlayerSelected(player);
        });
    }

    private void updateSelectionUI(@NonNull PlayerViewHolder holder, boolean isSelected) {
        holder.cardPlayer.setBackgroundResource(
                isSelected ? R.drawable.player_card_selected : R.drawable.player_card_normal);
        holder.tvSelectedBadge.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        float scale = isSelected ? SELECTED_SCALE : 1.0f;
        holder.cardPlayer.animate().scaleX(scale).scaleY(scale).setDuration(SCALE_ANIMATION_MS).start();
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        final CardView cardPlayer;
        final ImageView ivPlayerPhoto;
        final TextView tvPlayerName, tvPlayerPrice, tvPlayerStatus, tvSelectedBadge;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPlayer = itemView.findViewById(R.id.cardPlayer);
            ivPlayerPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvPlayerPrice = itemView.findViewById(R.id.tvPlayerPrice);
            tvPlayerStatus = itemView.findViewById(R.id.tvPlayerStatus);
            tvSelectedBadge = itemView.findViewById(R.id.tvSelectedBadge);
        }
    }
}
