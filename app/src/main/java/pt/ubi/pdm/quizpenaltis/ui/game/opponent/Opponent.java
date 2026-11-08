package pt.ubi.pdm.quizpenaltis.ui.game.opponent;

import pt.ubi.pdm.quizpenaltis.game.DefenseZone;
import pt.ubi.pdm.quizpenaltis.game.GoalZone;
import pt.ubi.pdm.quizpenaltis.model.Player;
import pt.ubi.pdm.quizpenaltis.model.Question;
import pt.ubi.pdm.quizpenaltis.util.Callback;

/**
 * O outro lado do jogo (computador ou, mais tarde, um amigo pela rede).
 * O {@code GameActivity} só trata do ecrã e pergunta aqui o que o adversário faz.
 *
 * <p>Tudo é assíncrono: as respostas chegam pelos {@link Callback}s, na thread principal.
 * Uma implementação local (o bot) pode chamá-los logo, dentro da própria chamada.
 * {@code turn} é o número da ronda (uma pergunta), a começar em 0.
 */
public interface Opponent {

    /** Carta do adversário (imagem no cabeçalho). */
    Player player();

    /** Nome mostrado no ecrã. */
    String displayName();

    /** A pergunta da ronda {@code turn}. */
    void questionFor(int turn, Callback<Question> cb);

    /**
     * O utilizador respondeu: {@code optionIndex} é a opção original da pergunta
     * (0 = A, 1 = B, 2 = C), ou {@link #NO_ANSWER} se o tempo acabou.
     */
    void onUserAnswered(int turn, int optionIndex);

    /** Na vez do adversário: acertou ou não na pergunta. */
    void opponentAnswers(int turn, Callback<Boolean> correct);

    /** O utilizador rematou para {@code zone}: foi golo? */
    void userShoots(int turn, GoalZone zone, Callback<Boolean> scored);

    /** O utilizador defende em {@code chosen}: para onde rematou o adversário e se foi defendido. */
    void userDefends(int turn, DefenseZone chosen, Callback<DefenseOutcome> outcome);

    /** Liberta recursos (ligações, listeners). Chamado no fim do ecrã. */
    void close();

    /** Valor de {@code optionIndex} quando o utilizador não respondeu a tempo. */
    int NO_ANSWER = -1;

    /** Resultado de uma defesa. */
    final class DefenseOutcome {
        public final DefenseZone opponentShot;
        public final boolean defended;

        public DefenseOutcome(DefenseZone opponentShot, boolean defended) {
            this.opponentShot = opponentShot;
            this.defended = defended;
        }
    }
}
