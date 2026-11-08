package pt.ubi.pdm.quizpenaltis.ui.game.opponent;

import pt.ubi.pdm.quizpenaltis.data.questions.QuestionManager;
import pt.ubi.pdm.quizpenaltis.game.DefenseZone;
import pt.ubi.pdm.quizpenaltis.game.GameRules;
import pt.ubi.pdm.quizpenaltis.game.GoalZone;
import pt.ubi.pdm.quizpenaltis.model.Player;
import pt.ubi.pdm.quizpenaltis.model.PlayerCatalog;
import pt.ubi.pdm.quizpenaltis.model.Question;
import pt.ubi.pdm.quizpenaltis.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * O computador: carta ao acaso, acerta com {@link GameRules#BOT_CORRECT_PROBABILITY},
 * remata para uma zona ao acaso e, quando o utilizador remata, decide o golo pelas
 * zonas verdes sorteadas conforme o custo do jogador do utilizador. Responde logo (sem rede).
 */
public class BotOpponent implements Opponent {

    private final Player bot;
    private final QuestionManager questions;
    private final int userCost;
    private final Random random;

    /** @param questions fornecido (e fechado) pelo ecrã; @param userCost custo do jogador do utilizador. */
    public BotOpponent(QuestionManager questions, int userCost) {
        this(PlayerCatalog.random(), questions, userCost, new Random());
    }

    BotOpponent(Player bot, QuestionManager questions, int userCost, Random random) {
        this.bot = bot;
        this.questions = questions;
        this.userCost = userCost;
        this.random = random;
    }

    @Override public Player player() { return bot; }

    @Override public String displayName() { return bot.getName(); }

    @Override
    public void questionFor(int turn, Callback<Question> cb) {
        cb.on(questions.getRandomQuestion());
    }

    @Override
    public void onUserAnswered(int turn, int optionIndex) {
        // O computador não precisa de saber.
    }

    @Override
    public void opponentAnswers(int turn, Callback<Boolean> correct) {
        correct.on(random.nextDouble() < GameRules.BOT_CORRECT_PROBABILITY);
    }

    @Override
    public void userShoots(int turn, GoalZone zone, Callback<Boolean> scored) {
        scored.on(drawGreenZones().contains(zone));
    }

    @Override
    public void userDefends(int turn, DefenseZone chosen, Callback<DefenseOutcome> outcome) {
        DefenseZone shot = DefenseZone.random();
        outcome.on(new DefenseOutcome(shot, chosen == shot));
    }

    @Override
    public void close() {
        // Nada a libertar: o QuestionManager pertence ao ecrã.
    }

    /** As zonas "golo" deste remate: {@link GameRules#greenZonesForCost} zonas ao acaso. */
    Set<GoalZone> drawGreenZones() {
        List<GoalZone> zones = new ArrayList<>(EnumSet.allOf(GoalZone.class));
        Collections.shuffle(zones, random);
        Set<GoalZone> green = EnumSet.noneOf(GoalZone.class);
        green.addAll(zones.subList(0, GameRules.greenZonesForCost(userCost)));
        return green;
    }
}
