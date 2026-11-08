package pt.ubi.pdm.quizpenaltis.ui.game.opponent;

import static org.junit.Assert.*;

import pt.ubi.pdm.quizpenaltis.game.GameRules;
import pt.ubi.pdm.quizpenaltis.game.GoalZone;
import pt.ubi.pdm.quizpenaltis.model.Player;

import org.junit.Test;

import java.util.Random;

public class BotOpponentTest {

    private static final Player BOT = new Player("Bot", 100, 0, 0);

    /** Random que devolve sempre o mesmo nextDouble(). */
    private static Random fixedDouble(double value) {
        return new Random() {
            @Override public double nextDouble() { return value; }
        };
    }

    private static boolean answers(double roll) {
        boolean[] result = new boolean[1];
        new BotOpponent(BOT, null, 500, fixedDouble(roll)).opponentAnswers(0, v -> result[0] = v);
        return result[0];
    }

    @Test public void acertaAbaixoDaProbabilidade() {
        assertTrue(answers(0.0));
        assertTrue(answers(GameRules.BOT_CORRECT_PROBABILITY - 0.01));
    }

    @Test public void falhaAPartirDaProbabilidade() {
        assertFalse(answers(GameRules.BOT_CORRECT_PROBABILITY));
        assertFalse(answers(0.99));
    }

    @Test public void zonasVerdesDependemDoCustoDoUtilizador() {
        for (int cost : new int[]{0, 100, 500, 1000}) {
            BotOpponent bot = new BotOpponent(BOT, null, cost, new Random(42));
            assertEquals(GameRules.greenZonesForCost(cost), bot.drawGreenZones().size());
        }
    }

    @Test public void semZonasVerdesNuncaMarca() {
        BotOpponent bot = new BotOpponent(BOT, null, 0, new Random(1));
        for (GoalZone zone : GoalZone.values()) {
            bot.userShoots(0, zone, scored -> assertFalse(scored));
        }
    }

    @Test public void nomeECartaSaoDoJogadorDoBot() {
        BotOpponent bot = new BotOpponent(BOT, null, 500, new Random());
        assertSame(BOT, bot.player());
        assertEquals("Bot", bot.displayName());
    }
}
