package pt.ubi.pdm.quizpenaltis.ui.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.model.Player;

/** Carrossel de cartas de jogador do ecrã de seleção. */
public class PlayerPagerAdapter extends RecyclerView.Adapter<PlayerPagerAdapter.PlayerViewHolder> {

    private static final float ALPHA_LOCKED = 0.55f;
    private static final float SELECTED_SCALE = 1.04f;
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
        boolean isSelected = position == selectedPosition && canSelect;

        holder.cardPlayer.setBackgroundResource(tierBackground(player.getTier()));
        holder.ivPlayerPhoto.setImageResource(player.getImageResource());
        holder.tvPlayerName.setText(player.getName());
        holder.tvPlayerPrice.setText(String.valueOf(player.getCost()));
        holder.tvPlayerStatus.setText(player.getDifficultyRes());

        holder.cardPlayer.setAlpha(canSelect ? 1.0f : ALPHA_LOCKED);
        holder.ivLock.setVisibility(canSelect ? View.GONE : View.VISIBLE);
        holder.selectedRing.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        holder.selectedBadge.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

        float scale = isSelected ? SELECTED_SCALE : 1.0f;
        holder.cardPlayer.animate().scaleX(scale).scaleY(scale).setDuration(SCALE_ANIMATION_MS).start();

        // O cartão inteiro é o botão. Mesmo bloqueado, o toque avisa quantos passos faltam.
        holder.cardPlayer.setOnClickListener(v -> {
            if (listener == null) return;
            if (canSelect) setSelectedPosition(holder.getBindingAdapterPosition());
            listener.onPlayerSelected(player);
        });
    }

    @DrawableRes
    private static int tierBackground(Player.Tier tier) {
        switch (tier) {
            case BRONZE: return R.drawable.ds_card_tier_bronze;
            case SILVER: return R.drawable.ds_card_tier_silver;
            default: return R.drawable.ds_card_tier_gold;
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        final View cardPlayer, selectedRing, selectedBadge, ivLock;
        final ImageView ivPlayerPhoto;
        final TextView tvPlayerName, tvPlayerPrice, tvPlayerStatus;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPlayer = itemView.findViewById(R.id.cardPlayer);
            selectedRing = itemView.findViewById(R.id.selectedRing);
            selectedBadge = itemView.findViewById(R.id.tvSelectedBadge);
            ivLock = itemView.findViewById(R.id.ivLock);
            ivPlayerPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvPlayerPrice = itemView.findViewById(R.id.tvPlayerPrice);
            tvPlayerStatus = itemView.findViewById(R.id.tvPlayerStatus);
        }
    }
}
