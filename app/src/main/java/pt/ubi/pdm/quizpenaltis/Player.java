package pt.ubi.pdm.quizpenaltis;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Um jogador que se pode "comprar" com pontos. */
public class Player {
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
}
