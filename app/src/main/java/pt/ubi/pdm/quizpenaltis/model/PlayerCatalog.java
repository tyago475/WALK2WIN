package pt.ubi.pdm.quizpenaltis.model;

import pt.ubi.pdm.quizpenaltis.R;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Fonte única dos jogadores. Para acrescentar um jogador basta
 * acrescentar uma linha aqui.
 */
public final class PlayerCatalog {
    private PlayerCatalog() {}

    public static final List<Player> ALL = Collections.unmodifiableList(Arrays.asList(
            new Player("Maguire",        100,  R.drawable.maguire,        R.string.difficulty_very_hard),
            new Player("Ricardo Esgaio", 200,  R.drawable.ricardo_esgaio, R.string.difficulty_hard),
            new Player("Taremi",         300,  R.drawable.taremi,         R.string.difficulty_hard),
            new Player("João Félix",     400,  R.drawable.joao_felix,     R.string.difficulty_medium),
            new Player("João Moutinho",  500,  R.drawable.joao_moutinho,  R.string.difficulty_medium),
            new Player("Lamine Yamal",   600,  R.drawable.lamine_yamal,   R.string.difficulty_medium),
            new Player("Nuno Mendes",    700,  R.drawable.nuno_mendes,    R.string.difficulty_easy),
            new Player("Kylian Mbappe",  800,  R.drawable.mbappe,         R.string.difficulty_easy),
            new Player("Messi",          900,  R.drawable.messi,          R.string.difficulty_very_easy),
            new Player("CR7",            1000, R.drawable.cr7,            R.string.difficulty_super_easy)
    ));

    private static final Random RANDOM = new Random();

    /** Custo do jogador mais barato: o mínimo de pontos para poder jogar. */
    public static int cheapestCost() {
        int min = Integer.MAX_VALUE;
        for (Player p : ALL) min = Math.min(min, p.getCost());
        return min;
    }

    /** Procura pelo nome; devolve o primeiro jogador se não existir. */
    public static Player byName(String name) {
        for (Player p : ALL) {
            if (p.getName().equals(name)) return p;
        }
        return ALL.get(ALL.size() - 1);
    }

    public static Player random() {
        return ALL.get(RANDOM.nextInt(ALL.size()));
    }
}
