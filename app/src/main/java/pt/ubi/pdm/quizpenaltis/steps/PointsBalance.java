package pt.ubi.pdm.quizpenaltis.steps;

import pt.ubi.pdm.quizpenaltis.util.Constants;

/**
 * Conta dos pontos de hoje. Não usa nada do Android, por isso é fácil de testar.
 *
 * Pontos ganhos = passos de hoje ÷ {@link Constants#STEPS_PER_POINT}.
 * Saldo = ganhos − gastos. Tanto os passos como os gastos recomeçam a zero todos os dias.
 */
public final class PointsBalance {
    private PointsBalance() {}

    /** Pontos que o jogador ainda pode gastar (nunca negativo). */
    public static int available(int stepsToday, int pointsSpent) {
        return Math.max(0, Constants.stepsToPoints(stepsToday) - pointsSpent);
    }

    /** Há saldo para um custo positivo? */
    public static boolean canSpend(int stepsToday, int pointsSpent, int cost) {
        return cost > 0 && available(stepsToday, pointsSpent) >= cost;
    }
}
