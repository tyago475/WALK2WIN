package pt.ubi.pdm.quizpenaltis.game;

/**
 * Regras do desempate por penáltis. Não usa nada do Android, por isso
 * pode ser testada com testes unitários simples.
 *
 * Fluxo: depois de cada remate chama-se recordPlayerShot/recordBotShot e
 * depois evaluate() para saber se o jogo continua, acabou, ou vai para morte súbita.
 */
public class PenaltyShootout {

    public static final int REGULAR_PENALTIES = 5;

    public enum Status { CONTINUE, SUDDEN_DEATH, FINISHED }

    private int playerScore;
    private int botScore;
    private int playerShots;
    private int botShots;
    private boolean suddenDeath;

    public void recordPlayerShot(boolean scored) {
        playerShots++;
        if (scored) playerScore++;
    }

    public void recordBotShot(boolean scored) {
        botShots++;
        if (scored) botScore++;
    }

    public Status evaluate() {
        if (suddenDeath) {
            boolean sameShots = playerShots == botShots && playerShots > 0;
            return (sameShots && playerScore != botScore) ? Status.FINISHED : Status.CONTINUE;
        }

        if (playerShots >= REGULAR_PENALTIES && botShots >= REGULAR_PENALTIES) {
            return playerScore == botScore ? Status.SUDDEN_DEATH : Status.FINISHED;
        }

        // Quem está atrás já não consegue alcançar o outro?
        if (playerScore + (REGULAR_PENALTIES - playerShots) < botScore) return Status.FINISHED;
        if (botScore + (REGULAR_PENALTIES - botShots) < playerScore) return Status.FINISHED;

        return Status.CONTINUE;
    }

    /** Começa a morte súbita: os remates voltam a zero (a pontuação mantém-se). */
    public void startSuddenDeath() {
        suddenDeath = true;
        playerShots = 0;
        botShots = 0;
    }

    public boolean isSuddenDeath() { return suddenDeath; }
    public int getPlayerScore() { return playerScore; }
    public int getBotScore() { return botScore; }
    public int getPlayerShots() { return playerShots; }
    public int getBotShots() { return botShots; }
    public boolean playerWon() { return playerScore > botScore; }
}
