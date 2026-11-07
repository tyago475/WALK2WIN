package pt.ubi.pdm.quizpenaltis.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Um jogador que se pode "comprar" com pontos. */
public class Player {

    /** Escalão da carta (como no Ultimate Team): quanto mais caro, mais valioso. */
    public enum Tier { BRONZE, SILVER, GOLD }

    private static final int BRONZE_MAX_COST = 300;
    private static final int SILVER_MAX_COST = 600;

    private final String name;
    private final int cost;
    private final int imageResource;
    private final int difficultyRes;

    public Player(String name, int cost, @DrawableRes int imageResource, @StringRes int difficultyRes) {
        this.name = name;
        this.cost = cost;
        this.imageResource = imageResource;
        this.difficultyRes = difficultyRes;
    }

    public String getName() { return name; }
    public int getCost() { return cost; }
    @DrawableRes public int getImageResource() { return imageResource; }
    @StringRes public int getDifficultyRes() { return difficultyRes; }

    public Tier getTier() {
        if (cost <= BRONZE_MAX_COST) return Tier.BRONZE;
        if (cost <= SILVER_MAX_COST) return Tier.SILVER;
        return Tier.GOLD;
    }
}
