package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

public class NonceTest {
    @Test public void maximoE2Elevado53Menos1() { assertEquals(9007199254740991L, Nonce.MAX); }

    @Test public void valoresGeradosFicamEm0AteMax() {
        Random r = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            long n = Nonce.random(r);
            assertTrue(Nonce.isValid(n));
        }
    }

    @Test public void mascaraTiraOsBitsDeCima() {
        Random allOnes = new Random() {
            @Override public long nextLong() { return -1L; }
        };
        assertEquals(Nonce.MAX, Nonce.random(allOnes));
    }

    @Test public void validacao() {
        assertTrue(Nonce.isValid(0));
        assertTrue(Nonce.isValid(Nonce.MAX));
        assertFalse(Nonce.isValid(-1));
        assertFalse(Nonce.isValid(Nonce.MAX + 1));
    }
    @Test public void semArgumentoUsaSecureRandomEFicaNoIntervalo() {
        for (int i = 0; i < 1000; i++) assertTrue(Nonce.isValid(Nonce.random()));
    }
}
