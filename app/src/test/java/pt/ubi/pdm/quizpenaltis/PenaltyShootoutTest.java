package pt.ubi.pdm.quizpenaltis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import pt.ubi.pdm.quizpenaltis.PenaltyShootout.Status;

public class PenaltyShootoutTest {

    @Test
    public void newGameContinues() {
        assertEquals(Status.CONTINUE, new PenaltyShootout().evaluate());
    }

    @Test
    public void endsEarlyWhenTrailingPlayerCannotCatchUp() {
        PenaltyShootout s = new PenaltyShootout();
        // bot marca 3 seguidos, jogador falha 3 -> jogador tem 2 remates, máximo 2 < 3
        for (int i = 0; i < 3; i++) {
            s.recordPlayerShot(false);
            s.recordBotShot(true);
        }
        assertEquals(Status.FINISHED, s.evaluate());
        assertFalse(s.playerWon());
    }

    @Test
    public void endsEarlyWhenBotCannotCatchUp() {
        PenaltyShootout s = new PenaltyShootout();
        for (int i = 0; i < 3; i++) {
            s.recordPlayerShot(true);
            s.recordBotShot(false);
        }
        assertEquals(Status.FINISHED, s.evaluate());
        assertTrue(s.playerWon());
    }

    @Test
    public void tieAfterFiveEachGoesToSuddenDeath() {
        PenaltyShootout s = new PenaltyShootout();
        for (int i = 0; i < 5; i++) {
            s.recordPlayerShot(i % 2 == 0);
            s.recordBotShot(i % 2 == 0);
        }
        assertEquals(Status.SUDDEN_DEATH, s.evaluate());
    }

    @Test
    public void winnerAfterFiveEachFinishes() {
        PenaltyShootout s = new PenaltyShootout();
        for (int i = 0; i < 5; i++) {
            s.recordPlayerShot(true);
            s.recordBotShot(i < 4);
        }
        assertEquals(Status.FINISHED, s.evaluate());
    }

    @Test
    public void suddenDeathNeedsBothToShootBeforeDeciding() {
        PenaltyShootout s = new PenaltyShootout();
        s.startSuddenDeath();
        s.recordPlayerShot(true);
        assertEquals(Status.CONTINUE, s.evaluate()); // bot ainda não rematou
        s.recordBotShot(false);
        assertEquals(Status.FINISHED, s.evaluate());
        assertTrue(s.playerWon());
    }

    @Test
    public void suddenDeathContinuesWhileTied() {
        PenaltyShootout s = new PenaltyShootout();
        s.startSuddenDeath();
        s.recordPlayerShot(true);
        s.recordBotShot(true);
        assertEquals(Status.CONTINUE, s.evaluate());
    }

    @Test
    public void greenZonesGrowWithCost() {
        assertEquals(0, GameRules.greenZonesForCost(50));
        assertEquals(1, GameRules.greenZonesForCost(100));
        assertEquals(14, GameRules.greenZonesForCost(1000));
    }

    @Test
    public void stepsToPoints() {
        assertEquals(150, Constants.stepsToPoints(1500));
    }
}
