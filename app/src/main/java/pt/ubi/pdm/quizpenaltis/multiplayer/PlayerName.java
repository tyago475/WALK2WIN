package pt.ubi.pdm.quizpenaltis.multiplayer;

/** Limpeza do nome do jogador. */
public final class PlayerName {
    private PlayerName() {}

    public static String sanitize(String raw) {
        if (raw == null) return MultiplayerRules.DEFAULT_NAME;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') sb.append(c);
        }
        String name = sb.toString().trim();
        if (name.length() > MultiplayerRules.NAME_MAX_LENGTH) {
            name = name.substring(0, MultiplayerRules.NAME_MAX_LENGTH).trim();
        }
        return name.isEmpty() ? MultiplayerRules.DEFAULT_NAME : name;
    }
}
