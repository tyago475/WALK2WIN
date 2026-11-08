package pt.ubi.pdm.quizpenaltis.multiplayer;

import pt.ubi.pdm.quizpenaltis.game.DefenseZone;
import pt.ubi.pdm.quizpenaltis.game.GoalZone;

import java.util.Set;

/** Resolução de um remate: golo se a zona é verde e a defesa não a cobre. */
public final class ShotResolution {
    private ShotResolution() {}

    public static boolean isGoal(GoalZone shot, DefenseZone defense, Set<GoalZone> greens) {
        return greens.contains(shot) && DefenseZone.covering(shot) != defense;
    }
}
