package pt.ubi.pdm.quizpenaltis.multiplayer;

/**
 * Um turno ({@code turns/{n}}) tal como chegou da rede. Cada campo é null se não existir ou for inválido.
 * {@code shot} só é visível a quem defende depois de escrever {@code defend} (e vice-versa).
 */
public final class TurnSnapshot {
    public final Role shooter;
    /** Índice da pergunta (0..9999). */
    public final Integer q;
    /** Opção escolhida (0..2) ou -1 se o tempo acabou. NÃO verificada pelas regras: cada um confere. */
    public final Integer answer;
    /** 0..14 ({@code GoalZone}). */
    public final Integer shotZone;
    public final Long shotNonce;
    /** 0..4 ({@code DefenseZone}). */
    public final Integer defendZone;
    public final Long defendNonce;

    public TurnSnapshot(Role shooter, Integer q, Integer answer,
                        Integer shotZone, Long shotNonce, Integer defendZone, Long defendNonce) {
        this.shooter = shooter;
        this.q = q;
        this.answer = answer;
        this.shotZone = shotZone;
        this.shotNonce = shotNonce;
        this.defendZone = defendZone;
        this.defendNonce = defendNonce;
    }
    /** Campo onde {@code committer} se compromete neste turno: "shot" se for o atirador, "defend" se não. */
    public static String commitField(Role committer, Role shooter) {
        if (committer == null || shooter == null) return null;
        return committer == shooter ? "shot" : "defend";
    }

    /** Campo do adversário de {@code me} neste turno (só legível depois de {@code me} se comprometer). */
    public static String opponentCommitField(Role me, Role shooter) {
        return me == null ? null : commitField(me.other(), shooter);
    }
}
