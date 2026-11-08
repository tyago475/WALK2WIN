package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

public class ForfeitRuleTest {
    @Test public void limiteExatoNaoDesiste() {
        assertFalse(ForfeitRule.hasForfeited(100_000L + 30_000L, 100_000L));
    }

    @Test public void passadoUmMsDesiste() {
        assertTrue(ForfeitRule.hasForfeited(100_000L + 30_001L, 100_000L));
    }
}
