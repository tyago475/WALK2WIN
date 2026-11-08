package pt.ubi.pdm.quizpenaltis.multiplayer;

/** Papel de um jogador numa sala: quem criou (HOST) ou quem entrou (GUEST). */
public enum Role {
    HOST, GUEST;

    public Role other() {
        return this == HOST ? GUEST : HOST;
    }

    /** Chave usada na base de dados ("host" / "guest"). */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Devolve null se a chave for inválida. */
    public static Role fromKey(String key) {
        for (Role r : values()) {
            if (r.key().equals(key)) return r;
        }
        return null;
    }
}
