package pt.ubi.pdm.quizpenaltis.multiplayer;

import pt.ubi.pdm.quizpenaltis.game.GoalZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Zonas verdes de um remate, determinadas por uma semente partilhada. */
public final class GreenZones {
    private GreenZones() {}

    public static Set<GoalZone> pick(long seed, int count) {
        List<GoalZone> all = new ArrayList<>(java.util.Arrays.asList(GoalZone.values()));
        Collections.shuffle(all, new Random(seed));
        Set<GoalZone> result = EnumSet.noneOf(GoalZone.class);
        result.addAll(all.subList(0, count));
        return result;
    }

    /** Semente comum: nenhum dos dois a controla sozinho. */
    public static long combine(long shooterNonce, long defenderNonce) {
        return shooterNonce ^ defenderNonce;
    }
}
