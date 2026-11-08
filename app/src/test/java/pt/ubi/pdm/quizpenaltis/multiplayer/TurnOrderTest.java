package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnOrderTest {
    @Test public void alternaComecandoPeloGuest() {
        Role[] esperado = {Role.GUEST, Role.HOST, Role.GUEST, Role.HOST, Role.GUEST, Role.HOST};
        for (int t = 0; t < esperado.length; t++) {
            assertEquals(esperado[t], TurnOrder.shooterOf(t, Role.GUEST));
        }
    }

    @Test public void roleKeys() {
        assertEquals("host", Role.HOST.key());
        assertEquals(Role.GUEST, Role.fromKey("guest"));
        assertNull(Role.fromKey("x"));
        assertEquals(Role.GUEST, Role.HOST.other());
    }
}
