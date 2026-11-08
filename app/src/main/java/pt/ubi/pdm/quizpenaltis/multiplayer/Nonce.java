package pt.ubi.pdm.quizpenaltis.multiplayer;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Número aleatório que cada jogador junta ao remate/defesa (a semente das zonas verdes é o XOR dos dois).
 * Fica em 0..2^53-1: acima disso um long perde precisão ao passar por double no Firebase (e as regras recusam).
 */
public final class Nonce {
    private Nonce() {}

    public static final long MAX = (1L << 53) - 1;

    private static final SecureRandom SECURE = new SecureRandom();

    /** O que se usa no jogo: SecureRandom, para o adversário não o conseguir adivinhar. */
    public static long random() {
        return random(SECURE);
    }

    /** Para testes (gerador determinista). No jogo usar {@link #random()}. */
    public static long random(Random r) {
        return r.nextLong() & MAX;
    }

    public static boolean isValid(long n) {
        return n >= 0 && n <= MAX;
    }
}
