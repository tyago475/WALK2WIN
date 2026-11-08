package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

import pt.ubi.pdm.quizpenaltis.game.DefenseZone;
import pt.ubi.pdm.quizpenaltis.game.GoalZone;

import java.util.EnumSet;
import java.util.Set;

public class ShotResolutionTest {
    private final Set<GoalZone> verdes = EnumSet.of(GoalZone.TOP_CENTER, GoalZone.BOTTOM_LEFT);

    @Test public void verdeSemCoberturaEGolo() {
        assertTrue(ShotResolution.isGoal(GoalZone.TOP_CENTER, DefenseZone.BOTTOM_RIGHT, verdes));
    }

    @Test public void verdeCobertaEDefendida() {
        assertFalse(ShotResolution.isGoal(GoalZone.TOP_CENTER, DefenseZone.CENTER, verdes));
    }

    @Test public void naoVerdeFalhaSempre() {
        assertFalse(ShotResolution.isGoal(GoalZone.MIDDLE_CENTER, DefenseZone.TOP_LEFT, verdes));
    }
}
