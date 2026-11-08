package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnSnapshotTest {
    @Test public void atiradorEscreveShotDefensorEscreveDefend() {
        assertEquals("shot", TurnSnapshot.commitField(Role.HOST, Role.HOST));
        assertEquals("defend", TurnSnapshot.commitField(Role.GUEST, Role.HOST));
        assertEquals("shot", TurnSnapshot.commitField(Role.GUEST, Role.GUEST));
        assertEquals("defend", TurnSnapshot.commitField(Role.HOST, Role.GUEST));
    }

    @Test public void campoDoAdversarioEOOutro() {
        assertEquals("defend", TurnSnapshot.opponentCommitField(Role.HOST, Role.HOST));
        assertEquals("shot", TurnSnapshot.opponentCommitField(Role.GUEST, Role.HOST));
    }

    @Test public void semPapelOuAtiradorDevolveNull() {
        assertNull(TurnSnapshot.commitField(null, Role.HOST));
        assertNull(TurnSnapshot.commitField(Role.HOST, null));
        assertNull(TurnSnapshot.opponentCommitField(null, null));
    }
}
