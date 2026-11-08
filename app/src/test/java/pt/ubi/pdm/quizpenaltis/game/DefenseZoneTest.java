package pt.ubi.pdm.quizpenaltis.game;

import static org.junit.Assert.*;

import org.junit.Test;

public class DefenseZoneTest {
    @Test public void coberturaDeZonasConhecidas() {
        assertEquals(DefenseZone.CENTER, DefenseZone.covering(GoalZone.TOP_CENTER));
        assertEquals(DefenseZone.TOP_LEFT, DefenseZone.covering(GoalZone.TOP_LEFT_CENTER));
        assertEquals(DefenseZone.TOP_RIGHT, DefenseZone.covering(GoalZone.MIDDLE_RIGHT));
        assertEquals(DefenseZone.BOTTOM_LEFT, DefenseZone.covering(GoalZone.BOTTOM_LEFT));
        assertEquals(DefenseZone.BOTTOM_RIGHT, DefenseZone.covering(GoalZone.BOTTOM_RIGHT_CENTER));
    }

    @Test public void todasAsZonasTemCobertura() {
        for (GoalZone z : GoalZone.values()) assertNotNull(DefenseZone.covering(z));
    }
}
