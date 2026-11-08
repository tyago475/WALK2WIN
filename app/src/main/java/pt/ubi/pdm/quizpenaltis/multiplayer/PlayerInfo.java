package pt.ubi.pdm.quizpenaltis.multiplayer;

/**
 * Registo de um jogador na sala ({@code players/{host|guest}}).
 * Vindo da rede: {@code name} já passou por {@link PlayerName#sanitize}; {@code cost} e {@code player}
 * NÃO são confiáveis (as regras não sabem o catálogo nem o saldo).
 */
public final class PlayerInfo {
    public final String uid;
    public final String name;
    /** Nome do jogador no {@code PlayerCatalog} (vindo da rede: null se não tiver 1..40 caracteres). */
    public final String player;
    /** Vindo da rede: 0 se faltar ou estiver fora de 0..1000 (o intervalo das regras). */
    public final int cost;
    public final boolean ready;
    /** Último sinal de vida (hora do servidor, ms); 0 se não existir. */
    public final long seen;

    public PlayerInfo(String uid, String name, String player, int cost, boolean ready, long seen) {
        this.uid = uid;
        this.name = name;
        this.player = player;
        this.cost = cost;
        this.ready = ready;
        this.seen = seen;
    }
}
