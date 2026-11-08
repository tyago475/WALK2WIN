package pt.ubi.pdm.quizpenaltis.multiplayer;

import java.util.Locale;
import java.util.Random;

/** Código de sala: 4 letras sem I nem O (para não confundir com 1 e 0). */
public final class RoomCode {
    private RoomCode() {}

    public static final int LENGTH = 4;
    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    public static String generate(Random r) {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String code) {
        if (code == null || code.length() != LENGTH) return false;
        for (int i = 0; i < code.length(); i++) {
            if (ALPHABET.indexOf(code.charAt(i)) < 0) return false;
        }
        return true;
    }
}
