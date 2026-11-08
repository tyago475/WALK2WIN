package pt.ubi.pdm.quizpenaltis.multiplayer;

/** Quem remata em cada turno: alterna a partir de quem começa. */
public final class TurnOrder {
    private TurnOrder() {}

    public static Role shooterOf(int turn, Role first) {
        return turn % 2 == 0 ? first : first.other();
    }
}
