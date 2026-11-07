package pt.ubi.pdm.quizpenaltis.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PointsBalanceTest {

    @Test
    public void noStepsNoPoints() {
        assertEquals(0, PointsBalance.available(0, 0));
    }

    @Test
    public void tenStepsAreOnePoint() {
        assertEquals(150, PointsBalance.available(1500, 0));
        assertEquals(0, PointsBalance.available(9, 0)); // arredonda para baixo
    }

    @Test
    public void spentPointsAreSubtracted() {
        assertEquals(200, PointsBalance.available(3000, 100));
    }

    @Test
    public void balanceNeverGoesNegative() {
        assertEquals(0, PointsBalance.available(500, 100));
    }

    @Test
    public void canSpendExactBalance() {
        assertTrue(PointsBalance.canSpend(3000, 0, 300));
    }

    @Test
    public void cannotSpendMoreThanBalance() {
        assertFalse(PointsBalance.canSpend(3000, 100, 300)); // saldo 200 < 300
    }

    @Test
    public void spendingTwiceUsesUpTheBalance() {
        int spent = 0;
        assertTrue(PointsBalance.canSpend(2000, spent, 100));
        spent += 100;
        assertTrue(PointsBalance.canSpend(2000, spent, 100));
        spent += 100;
        assertFalse(PointsBalance.canSpend(2000, spent, 100)); // 200 ganhos, 200 gastos
    }

    @Test
    public void zeroOrNegativeCostIsRejected() {
        assertFalse(PointsBalance.canSpend(5000, 0, 0));
        assertFalse(PointsBalance.canSpend(5000, 0, -100));
    }
}
