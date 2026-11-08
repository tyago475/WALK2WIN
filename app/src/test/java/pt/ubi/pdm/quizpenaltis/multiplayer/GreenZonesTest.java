package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

public class GreenZonesTest {
    @Test public void mesmaSementeMesmoConjunto() {
        assertEquals(GreenZones.pick(42L, 7), GreenZones.pick(42L, 7));
        assertEquals(7, GreenZones.pick(42L, 7).size());
    }

    @Test public void contagens() {
        assertTrue(GreenZones.pick(5L, 0).isEmpty());
        assertEquals(14, GreenZones.pick(5L, 14).size());
    }

    @Test public void combineESimetrico() {
        assertEquals(GreenZones.combine(123L, 456L), GreenZones.combine(456L, 123L));
    }
}
