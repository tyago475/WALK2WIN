package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Random;

public class RoomCodeTest {
    @Test public void generateProduzCodigosValidosSemIeO() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            String c = RoomCode.generate(r);
            assertEquals(RoomCode.LENGTH, c.length());
            assertTrue(RoomCode.isValid(c));
            assertEquals(-1, c.indexOf('I'));
            assertEquals(-1, c.indexOf('O'));
        }
    }

    @Test public void normalizeEValidacao() {
        assertFalse(RoomCode.isValid(RoomCode.normalize(" ab cd ")));
        assertEquals("ABCD", RoomCode.normalize("abcd"));
        assertEquals("", RoomCode.normalize(null));
    }

    @Test public void codigosInvalidos() {
        assertFalse(RoomCode.isValid("AB1D"));
        assertFalse(RoomCode.isValid("ABC"));
        assertFalse(RoomCode.isValid("ABCDE"));
        assertFalse(RoomCode.isValid(null));
    }
}
