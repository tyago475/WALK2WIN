package pt.ubi.pdm.quizpenaltis.data.questions;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import pt.ubi.pdm.quizpenaltis.data.db.AppDatabase;
import pt.ubi.pdm.quizpenaltis.model.Question;

/**
 * Escolhe perguntas sem repetir até todas terem saído.
 * Na primeira execução carrega as perguntas de {@code assets/questions.json} para a base de dados.
 *
 * Todo o acesso à BD corre numa única thread (executor), por isso a inicialização
 * termina sempre antes do primeiro pedido de pergunta. A pergunta seguinte fica sempre
 * pré-carregada, para a UI não esperar pela BD a cada ronda.
 */
public class QuestionManager {
    private static final String TAG = "QuestionManager";
    private static final long REQUEST_TIMEOUT_SECONDS = 3;

    private final Context appContext;
    private final AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Só são tocadas na thread do executor.
    private final List<Integer> availableIds = new ArrayList<>();
    private final List<Integer> usedIds = new ArrayList<>();

    /** Próxima pergunta já lida da BD; null enquanto não estiver pronta. */
    private final AtomicReference<Question> prefetched = new AtomicReference<>();

    public QuestionManager(Context context) {
        appContext = context.getApplicationContext();
        database = AppDatabase.getDatabase(appContext);
        executor.execute(this::initialize);
    }

    private void initialize() {
        try {
            if (database.questionDao().getQuestionCount() == 0) {
                database.questionDao().insertAllQuestions(QuestionLoader.loadFromAssets(appContext));
            }
            availableIds.addAll(database.questionDao().getAllQuestionIds());
            Collections.shuffle(availableIds);
            Log.d(TAG, "Perguntas disponíveis: " + availableIds.size());
            prefetch();
        } catch (Exception e) {
            // Sem perguntas na BD o jogo continua com a pergunta de reserva.
            Log.e(TAG, "Erro ao inicializar perguntas", e);
        }
    }

    /**
     * Devolve a próxima pergunta (nunca null). Normalmente é imediato (já estava pré-carregada);
     * só espera pela BD, com limite de tempo, se ainda não houver nenhuma pronta.
     */
    public Question getRandomQuestion() {
        Question ready = prefetched.getAndSet(null);
        if (ready != null) {
            executor.execute(this::prefetch);
            return ready;
        }
        try {
            Question q = executor.submit(this::nextQuestion).get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return q != null ? q : fallbackQuestion();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter pergunta", e);
            return fallbackQuestion();
        }
    }

    /** Corre na thread do executor. */
    private void prefetch() {
        if (prefetched.get() != null) return;
        try {
            prefetched.set(nextQuestion());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao pré-carregar pergunta", e);
        }
    }

    private Question nextQuestion() {
        if (availableIds.isEmpty()) {
            availableIds.addAll(usedIds);
            usedIds.clear();
            Collections.shuffle(availableIds);
        }
        if (availableIds.isEmpty()) return null;

        int id = availableIds.remove(0);
        usedIds.add(id);
        return database.questionDao().getQuestionById(id);
    }

    /** As 3 opções da pergunta por ordem aleatória. */
    public List<String> getShuffledOptions(Question question) {
        List<String> options = Arrays.asList(
                question.getOptionA(), question.getOptionB(), question.getOptionC());
        Collections.shuffle(options);
        return options;
    }

    public void close() {
        executor.shutdown();
    }

    private Question fallbackQuestion() {
        return new Question("Qual é a capital de Portugal?",
                "A) Porto", "B) Lisboa", "C) Coimbra", "B");
    }
}
