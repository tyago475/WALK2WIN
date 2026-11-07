package pt.ubi.pdm.quizpenaltis.data.questions;

import pt.ubi.pdm.quizpenaltis.data.db.AppDatabase;
import pt.ubi.pdm.quizpenaltis.model.Question;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuestionManager {
    private static final String TAG = "QuestionManager";
    private AppDatabase database;
    private ExecutorService executor;
    private Random random;
    private List<Integer> availableQuestionIds;
    private List<Integer> usedQuestionIds;
    private boolean isInitialized = false;

    public QuestionManager(Context context) {
        database = AppDatabase.getDatabase(context);
        executor = Executors.newSingleThreadExecutor();
        random = new Random();
        availableQuestionIds = new ArrayList<>();
        usedQuestionIds = new ArrayList<>();


        initializeAvailableQuestionsSync();
    }

    private void initializeAvailableQuestionsSync() {
        executor.execute(() -> {
            try {
                // Verificar se a base de dados está vazia
                int count = database.questionDao().getQuestionCount();
                Log.d(TAG, "Question count in database: " + count);

                if (count == 0) {
                    Log.d(TAG, "Database is empty, inserting initial questions...");
                    insertInitialQuestions();
                } else {
                    Log.d(TAG, "Database already has " + count + " questions");
                }

                // Obter todos os IDs de perguntas disponíveis
                List<Integer> allIds = database.questionDao().getAllQuestionIds();
                Log.d(TAG, "Retrieved " + allIds.size() + " question IDs from database");

                // Atualizar as listas
                availableQuestionIds.clear();
                availableQuestionIds.addAll(allIds);
                Collections.shuffle(availableQuestionIds);
                isInitialized = true;

                Log.d(TAG, "Available questions initialized: " + availableQuestionIds.size());

            } catch (Exception e) {
                Log.e(TAG, "Error initializing questions: " + e.getMessage(), e);

                // Fallback imediato
                availableQuestionIds.clear();
                for (int i = 1; i <= 50; i++) {
                    availableQuestionIds.add(i);
                }
                Collections.shuffle(availableQuestionIds);
                isInitialized = true;
                Log.d(TAG, "Using fallback questions: " + availableQuestionIds.size());
            }
        });
    }


    public Question getRandomQuestion() {
        if (!isInitialized) {
            Log.w(TAG, "QuestionManager not initialized yet, using fallback");
            return createFallbackQuestion();
        }

        try {

            return executor.submit(() -> {
                synchronized (this) {
                    Log.d(TAG, "Getting random question - Available: " + availableQuestionIds.size());


                    if (availableQuestionIds.isEmpty()) {
                        Log.d(TAG, "No available questions, resetting...");
                        resetUsedQuestionsSync();
                    }


                    if (availableQuestionIds.isEmpty()) {
                        Log.e(TAG, "CRITICAL: No questions available!");
                        return createFallbackQuestion();
                    }


                    int questionId = availableQuestionIds.remove(0);
                    usedQuestionIds.add(questionId);

                    Log.d(TAG, "Selected question ID: " + questionId);


                    Question question = database.questionDao().getQuestionById(questionId);

                    if (question == null) {
                        Log.e(TAG, "Question with ID " + questionId + " not found!");
                        return createFallbackQuestion();
                    }

                    Log.d(TAG, "Successfully retrieved question: " +
                            question.getQuestion().substring(0, Math.min(30, question.getQuestion().length())) + "...");
                    return question;
                }
            }).get();

        } catch (Exception e) {
            Log.e(TAG, "Error in getRandomQuestion: " + e.getMessage(), e);
            return createFallbackQuestion();
        }
    }


    private void resetUsedQuestionsSync() {
        synchronized (this) {
            Log.d(TAG, "Resetting used questions. Before - Available: " +
                    availableQuestionIds.size() + ", Used: " + usedQuestionIds.size());

            availableQuestionIds.addAll(usedQuestionIds);
            usedQuestionIds.clear();
            Collections.shuffle(availableQuestionIds);

            Log.d(TAG, "After reset - Available: " + availableQuestionIds.size() +
                    ", Used: " + usedQuestionIds.size());
        }
    }

    // Reset público para novo jogo
    public void resetUsedQuestions() {
        executor.execute(this::resetUsedQuestionsSync);
    }

    // Embaralha as opções de resposta
    public List<String> getShuffledOptions(Question question) {
        if (question == null) {
            Log.e(TAG, "Question is null in getShuffledOptions!");
            return Arrays.asList("A) Erro", "B) Sem", "C) Pergunta");
        }

        List<String> options = Arrays.asList(
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC()
        );
        Collections.shuffle(options);
        return options;
    }

    // Verificar se está inicializado
    public boolean isInitialized() {
        return isInitialized;
    }

    // Fechar o executor
    public void close() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    // Inserir perguntas iniciais
    private void insertInitialQuestions() {
        try {

            List<Question> initialQuestions = new ArrayList<>();

            // GEOGRAFIA (25 perguntas)
            initialQuestions.add(new Question("Qual é a capital do Brasil?",
                    "A) São Paulo", "B) Brasília", "C) Rio de Janeiro", "B"));
            initialQuestions.add(new Question("Qual é o maior país do mundo em área territorial?",
                    "A) China", "B) Rússia", "C) Canadá", "B"));
            initialQuestions.add(new Question("Em que continente fica o Egito?",
                    "A) África", "B) Ásia", "C) Europa", "A"));
            initialQuestions.add(new Question("Qual é o rio mais longo do mundo?",
                    "A) Amazonas", "B) Nilo", "C) Yangtzé", "B"));
            initialQuestions.add(new Question("Qual é a capital do Japão?",
                    "A) Osaka", "B) Kyoto", "C) Tóquio", "C"));
            initialQuestions.add(new Question("Em que país fica a cidade de Barcelona?",
                    "A) Itália", "B) Espanha", "C) França", "B"));
            initialQuestions.add(new Question("Qual é o menor país do mundo?",
                    "A) Mónaco", "B) Vaticano", "C) San Marino", "B"));
            initialQuestions.add(new Question("Qual é o deserto mais quente do mundo?",
                    "A) Saara", "B) Kalahari", "C) Gobi", "A"));
            initialQuestions.add(new Question("Qual é a montanha mais alta do mundo?",
                    "A) K2", "B) Monte Everest", "C) Mont Blanc", "B"));
            initialQuestions.add(new Question("Quantos oceanos existem no planeta Terra?",
                    "A) 4", "B) 5", "C) 6", "B"));
            initialQuestions.add(new Question("Qual é a capital da Argentina?",
                    "A) Buenos Aires", "B) Córdoba", "C) Rosário", "A"));
            initialQuestions.add(new Question("Em que país fica a Grande Muralha?",
                    "A) Japão", "B) China", "C) Índia", "B"));
            initialQuestions.add(new Question("Qual é o maior oceano do mundo?",
                    "A) Atlântico", "B) Índico", "C) Pacífico", "C"));
            initialQuestions.add(new Question("Qual é a capital da Austrália?",
                    "A) Sydney", "B) Melbourne", "C) Camberra", "C"));
            initialQuestions.add(new Question("Quantos estados tem o Brasil?",
                    "A) 26", "B) 27", "C) 28", "B"));
            initialQuestions.add(new Question("Qual é o país mais populoso do mundo?",
                    "A) Índia", "B) China", "C) Estados Unidos", "B"));
            initialQuestions.add(new Question("Em que continente fica o México?",
                    "A) América do Norte", "B) América do Sul", "C) América Central", "A"));
            initialQuestions.add(new Question("Qual é a capital do Canadá?",
                    "A) Toronto", "B) Vancouver", "C) Ottawa", "C"));
            initialQuestions.add(new Question("Qual é o maior lago de África?",
                    "A) Lago Vitória", "B) Lago Tanganica", "C) Lago Niassa", "A"));
            initialQuestions.add(new Question("Quantos países fazem fronteira com Portugal?",
                    "A) 1", "B) 2", "C) 3", "A"));
            initialQuestions.add(new Question("Qual é a capital da Itália?",
                    "A) Roma", "B) Milão", "C) Veneza", "A"));
            initialQuestions.add(new Question("Em que país fica o Monte Fuji?",
                    "A) China", "B) Coreia do Sul", "C) Japão", "C"));
            initialQuestions.add(new Question("Qual é o maior arquipélago do mundo?",
                    "A) Filipinas", "B) Indonésia", "C) Japão", "B"));
            initialQuestions.add(new Question("Qual é a capital da Rússia?",
                    "A) São Petersburgo", "B) Moscovo", "C) Kiev", "B"));
            initialQuestions.add(new Question("Quantos fusos horários tem a Rússia?",
                    "A) 9", "B) 10", "C) 11", "C"));

            // HISTÓRIA (25 perguntas)
            initialQuestions.add(new Question("Em que ano terminou a Segunda Guerra Mundial?",
                    "A) 1945", "B) 1939", "C) 1950", "A"));
            initialQuestions.add(new Question("Quem foi o primeiro presidente de Portugal?",
                    "A) Teófilo Braga", "B) Manuel de Arriaga", "C) Sidónio Pais", "B"));
            initialQuestions.add(new Question("Quem descobriu o Brasil?",
                    "A) Pedro Álvares Cabral", "B) Cristóvão Colombo", "C) Vasco da Gama", "A"));
            initialQuestions.add(new Question("Em que ano começou a Primeira Guerra Mundial?",
                    "A) 1914", "B) 1918", "C) 1939", "A"));
            initialQuestions.add(new Question("Quem foi o primeiro homem a pisar na Lua?",
                    "A) Neil Armstrong", "B) Buzz Aldrin", "C) Yuri Gagarin", "A"));
            initialQuestions.add(new Question("Qual foi o último imperador do Brasil?",
                    "A) Dom Pedro I", "B) Dom Pedro II", "C) Dom João VI", "B"));
            initialQuestions.add(new Question("Em que ano foi a Revolução dos Cravos em Portugal?",
                    "A) 1974", "B) 1975", "C) 1976", "A"));
            initialQuestions.add(new Question("Quem foi o líder da União Soviética durante a 2ª Guerra?",
                    "A) Lenin", "B) Stalin", "C) Khrushchev", "B"));
            initialQuestions.add(new Question("Em que ano o Brasil se tornou independente?",
                    "A) 1822", "B) 1825", "C) 1815", "A"));
            initialQuestions.add(new Question("Quem foi o primeiro rei de Portugal?",
                    "A) D. Afonso Henriques", "B) D. Sancho I", "C) D. Dinis", "A"));
            initialQuestions.add(new Question("Em que século Vasco da Gama chegou à Índia?",
                    "A) XV", "B) XVI", "C) XVII", "A"));
            initialQuestions.add(new Question("Quem foi o faraó mais famoso do Egito Antigo?",
                    "A) Ramsés II", "B) Tutancâmon", "C) Cleópatra", "B"));
            initialQuestions.add(new Question("Em que ano caiu o Muro de Berlim?",
                    "A) 1989", "B) 1991", "C) 1987", "A"));
            initialQuestions.add(new Question("Quem foi o primeiro presidente dos EUA?",
                    "A) Thomas Jefferson", "B) George Washington", "C) Abraham Lincoln", "B"));
            initialQuestions.add(new Question("Em que ano terminou a Guerra Colonial Portuguesa?",
                    "A) 1974", "B) 1975", "C) 1973", "A"));
            initialQuestions.add(new Question("Quem escreveu 'Os Lusíadas'?",
                    "A) Fernando Pessoa", "B) Luís de Camões", "C) Eça de Queirós", "B"));
            initialQuestions.add(new Question("Em que ano D. João VI fugiu para o Brasil?",
                    "A) 1807", "B) 1808", "C) 1810", "B"));
            initialQuestions.add(new Question("Quem foi o líder da Revolução Russa?",
                    "A) Stalin", "B) Lenin", "C) Trotsky", "B"));
            initialQuestions.add(new Question("Em que século ocorreu o Descobrimento do Brasil?",
                    "A) XV", "B) XVI", "C) XVII", "B"));
            initialQuestions.add(new Question("Quem foi o imperador francês que invadiu Portugal?",
                    "A) Luís XVI", "B) Napoleão", "C) Carlos Magno", "B"));
            initialQuestions.add(new Question("Em que ano foi proclamada a República Portuguesa?",
                    "A) 1910", "B) 1908", "C) 1912", "A"));
            initialQuestions.add(new Question("Quem foi o último imperador romano?",
                    "A) Nero", "B) Júlio César", "C) Rómulo Augusto", "C"));
            initialQuestions.add(new Question("Em que ano ocorreu o terramoto de Lisboa?",
                    "A) 1755", "B) 1750", "C) 1760", "A"));
            initialQuestions.add(new Question("Quem foi o primeiro papa da Igreja Católica?",
                    "A) São Pedro", "B) São Paulo", "C) São João", "A"));
            initialQuestions.add(new Question("Em que ano terminou a Idade Média?",
                    "A) 1453", "B) 1492", "C) 1500", "B"));

            // CIÊNCIA (25 perguntas)
            initialQuestions.add(new Question("Qual é o elemento químico com símbolo 'O'?",
                    "A) Ouro", "B) Oxigénio", "C) Ósmio", "B"));
            initialQuestions.add(new Question("Quantos planetas existem no sistema solar?",
                    "A) 8", "B) 9", "C) 7", "A"));
            initialQuestions.add(new Question("Qual é o planeta conhecido como Planeta Vermelho?",
                    "A) Vénus", "B) Marte", "C) Júpiter", "B"));
            initialQuestions.add(new Question("Quantos ossos tem o corpo humano adulto?",
                    "A) 206", "B) 210", "C) 200", "A"));
            initialQuestions.add(new Question("Qual é o gás mais abundante na atmosfera terrestre?",
                    "A) Oxigénio", "B) Nitrogénio", "C) Dióxido de Carbono", "B"));
            initialQuestions.add(new Question("Quem desenvolveu a teoria da relatividade?",
                    "A) Isaac Newton", "B) Albert Einstein", "C) Stephen Hawking", "B"));
            initialQuestions.add(new Question("Qual é o animal mais rápido do mundo?",
                    "A) Guepardo", "B) Leopardo", "C) Falcão-peregrino", "C"));
            initialQuestions.add(new Question("Quantos cromossomas tem uma célula humana normal?",
                    "A) 23", "B) 46", "C) 48", "B"));
            initialQuestions.add(new Question("Qual é o metal líquido à temperatura ambiente?",
                    "A) Chumbo", "B) Mercúrio", "C) Ferro", "B"));
            initialQuestions.add(new Question("Quantas patas tem uma aranha?",
                    "A) 6", "B) 8", "C) 10", "B"));
            initialQuestions.add(new Question("Qual é o maior órgão do corpo humano?",
                    "A) Coração", "B) Pele", "C) Pulmão", "B"));
            initialQuestions.add(new Question("Quem descobriu a penicilina?",
                    "A) Marie Curie", "B) Alexander Fleming", "C) Louis Pasteur", "B"));
            initialQuestions.add(new Question("Qual é a velocidade da luz no vácuo?",
                    "A) 300.000 km/s", "B) 150.000 km/s", "C) 450.000 km/s", "A"));
            initialQuestions.add(new Question("Quantas câmaras tem o coração humano?",
                    "A) 2", "B) 3", "C) 4", "C"));
            initialQuestions.add(new Question("Qual é o planeta mais próximo do Sol?",
                    "A) Vénus", "B) Mercúrio", "C) Marte", "B"));
            initialQuestions.add(new Question("Quantas luas tem Marte?",
                    "A) 1", "B) 2", "C) 3", "B"));
            initialQuestions.add(new Question("Qual é a unidade básica da vida?",
                    "A) Átomo", "B) Célula", "C) Molécula", "B"));
            initialQuestions.add(new Question("Quantos elementos tem a tabela periódica?",
                    "A) 118", "B) 120", "C) 116", "A"));
            initialQuestions.add(new Question("Qual é o gás que as plantas absorvem?",
                    "A) Oxigénio", "B) Nitrogénio", "C) Dióxido de Carbono", "C"));
            initialQuestions.add(new Question("Quantos anos tem a Terra aproximadamente?",
                    "A) 4,5 mil milhões", "B) 10 mil milhões", "C) 1 milhão", "A"));
            initialQuestions.add(new Question("Qual é o animal mais pesado do mundo?",
                    "A) Elefante", "B) Baleia-azul", "C) Girafa", "B"));
            initialQuestions.add(new Question("Quantos litros de sangue tem o corpo humano?",
                    "A) 3-4", "B) 5-6", "C) 7-8", "B"));
            initialQuestions.add(new Question("Qual é o planeta com anéis mais visíveis?",
                    "A) Júpiter", "B) Saturno", "C) Urano", "B"));
            initialQuestions.add(new Question("Quantos sentidos tem o ser humano?",
                    "A) 5", "B) 6", "C) 7", "A"));
            initialQuestions.add(new Question("Qual é a estrela mais próxima da Terra?",
                    "A) Sirius", "B) Sol", "C) Alpha Centauri", "B"));

            // DESPORTO (25 perguntas)
            initialQuestions.add(new Question("Quantos jogadores tem uma equipa de futebol em campo?",
                    "A) 10", "B) 11", "C) 12", "B"));
            initialQuestions.add(new Question("Em que ano Portugal ganhou o Euro?",
                    "A) 2004", "B) 2016", "C) 2012", "B"));
            initialQuestions.add(new Question("Qual é o desporto mais popular do mundo?",
                    "A) Basquetebol", "B) Futebol", "C) Ténis", "B"));
            initialQuestions.add(new Question("Quantos tempos tem um jogo de futebol?",
                    "A) 2", "B) 3", "C) 4", "A"));
            initialQuestions.add(new Question("Qual país ganhou mais Copas do Mundo de futebol?",
                    "A) Alemanha", "B) Brasil", "C) Itália", "B"));
            initialQuestions.add(new Question("Em que ano foram os primeiros Jogos Olímpicos modernos?",
                    "A) 1896", "B) 1900", "C) 1924", "A"));
            initialQuestions.add(new Question("Quantos jogadores tem uma equipa de basquetebol em campo?",
                    "A) 5", "B) 6", "C) 7", "A"));
            initialQuestions.add(new Question("Qual é a prova mais rápida do atletismo?",
                    "A) 100m", "B) 200m", "C) 400m", "A"));
            initialQuestions.add(new Question("Quantos Grand Slams tem o ténis?",
                    "A) 3", "B) 4", "C) 5", "B"));
            initialQuestions.add(new Question("Qual é a modalidade desportiva de Michael Phelps?",
                    "A) Atletismo", "B) Natação", "C) Ciclismo", "B"));
            initialQuestions.add(new Question("Quantos jogadores tem uma equipa de andebol?",
                    "A) 6", "B) 7", "C) 8", "B"));
            initialQuestions.add(new Question("Qual é o país de origem do judo?",
                    "A) China", "B) Coreia", "C) Japão", "C"));
            initialQuestions.add(new Question("Quantos sets tem um jogo de ténis?",
                    "A) 3 ou 5", "B) 2 ou 4", "C) 4 ou 6", "A"));
            initialQuestions.add(new Question("Qual é o clube com mais Champions League?",
                    "A) Barcelona", "B) Real Madrid", "C) Bayern Munique", "B"));
            initialQuestions.add(new Question("Quantos minutos dura uma partida de futebol?",
                    "A) 80", "B) 90", "C) 100", "B"));
            initialQuestions.add(new Question("Qual é o desporto nacional do Brasil?",
                    "A) Futebol", "B) Voleibol", "C) Capoeira", "A"));
            initialQuestions.add(new Question("Quantos jogadores tem uma equipa de rugby?",
                    "A) 13", "B) 14", "C) 15", "C"));
            initialQuestions.add(new Question("Qual país organizou os Jogos Olímpicos de 2016?",
                    "A) China", "B) Brasil", "C) Grécia", "B"));
            initialQuestions.add(new Question("Quantos buracos tem um campo de golfe padrão?",
                    "A) 16", "B) 18", "C) 20", "B"));
            initialQuestions.add(new Question("Qual é o desporto de Serena Williams?",
                    "A) Ténis", "B) Golfe", "C) Atletismo", "A"));
            initialQuestions.add(new Question("Quantos jogadores tem uma equipa de basebol?",
                    "A) 8", "B) 9", "C) 10", "B"));
            initialQuestions.add(new Question("Qual país inventou o futebol?",
                    "A) Brasil", "B) Inglaterra", "C) Itália", "B"));
            initialQuestions.add(new Question("Quantas voltas tem a F1 em Mónaco?",
                    "A) 76", "B) 78", "C) 80", "B"));
            initialQuestions.add(new Question("Qual é o desporto de Usain Bolt?",
                    "A) Atletismo", "B) Futebol", "C) Basquetebol", "A"));
            initialQuestions.add(new Question("Quantos jogadores tem uma equipa de voleibol?",
                    "A) 5", "B) 6", "C) 7", "B"));

            // ARTE E CULTURA (25 perguntas)
            initialQuestions.add(new Question("Quem pintou a Mona Lisa?",
                    "A) Van Gogh", "B) Picasso", "C) Leonardo da Vinci", "C"));
            initialQuestions.add(new Question("Qual é o instrumento musical nacional de Portugal?",
                    "A) Guitarra", "B) Cavaquinho", "C) Bandolim", "B"));
            initialQuestions.add(new Question("Quem escreveu 'Os Lusíadas'?",
                    "A) Fernando Pessoa", "B) Luís de Camões", "C) Eça de Queirós", "B"));
            initialQuestions.add(new Question("Qual é a sétima arte?",
                    "A) Pintura", "B) Cinema", "C) Música", "B"));
            initialQuestions.add(new Question("Quem compôs a '9ª Sinfonia'?",
                    "A) Mozart", "B) Beethoven", "C) Bach", "B"));
            initialQuestions.add(new Question("Qual é o estilo arquitetónico da Torre de Belém?",
                    "A) Gótico", "B) Manuelino", "C) Românico", "B"));
            initialQuestions.add(new Question("Quem escreveu 'Dom Quixote'?",
                    "A) Cervantes", "B) Shakespeare", "C) Camões", "A"));
            initialQuestions.add(new Question("Qual é a nacionalidade de Frida Kahlo?",
                    "A) Espanhola", "B) Mexicana", "C) Brasileira", "B"));
            initialQuestions.add(new Question("Quantas cordas tem uma guitarra clássica?",
                    "A) 5", "B) 6", "C) 7", "B"));
            initialQuestions.add(new Question("Quem pintou 'A Noite Estrelada'?",
                    "A) Picasso", "B) Van Gogh", "C) Monet", "B"));
            initialQuestions.add(new Question("Qual é o romance mais famoso de José Saramago?",
                    "A) Memorial do Convento", "B) Ensaio sobre a Cegueira", "C) O Ano da Morte", "A"));
            initialQuestions.add(new Question("Quantas teclas tem um piano standard?",
                    "A) 85", "B) 88", "C) 92", "B"));
            initialQuestions.add(new Question("Quem esculpiu 'O Pensador'?",
                    "A) Michelangelo", "B) Rodin", "C) Donatello", "B"));
            initialQuestions.add(new Question("Qual é o instrumento de Astor Piazzolla?",
                    "A) Guitarra", "B) Bandoneón", "C) Violino", "B"));
            initialQuestions.add(new Question("Quem escreveu 'Romeu e Julieta'?",
                    "A) Shakespeare", "B) Dickens", "C) Hemingway", "A"));
            initialQuestions.add(new Question("Qual é o estilo musical de Amália Rodrigues?",
                    "A) Fado", "B) Flamenco", "C) Tango", "A"));
            initialQuestions.add(new Question("Quantas sinfonias compôs Beethoven?",
                    "A) 7", "B) 8", "C) 9", "C"));
            initialQuestions.add(new Question("Quem pintou 'Guernica'?",
                    "A) Dalí", "B) Picasso", "C) Miró", "B"));
            initialQuestions.add(new Question("Qual é o romance de Machado de Assis?",
                    "A) Dom Casmurro", "B) O Cortiço", "C) Iracema", "A"));
            initialQuestions.add(new Question("Quantos atos tem uma ópera tradicional?",
                    "A) 2", "B) 3", "C) 4", "B"));
            initialQuestions.add(new Question("Quem compôs 'As Quatro Estações'?",
                    "A) Vivaldi", "B) Mozart", "C) Bach", "A"));
            initialQuestions.add(new Question("Qual é o país de origem do tango?",
                    "A) Argentina", "B) Brasil", "C) Espanha", "A"));
            initialQuestions.add(new Question("Quem escreveu '1984'?",
                    "A) Orwell", "B) Huxley", "C) Kafka", "A"));
            initialQuestions.add(new Question("Quantos violinos tem uma orquestra sinfónica?",
                    "A) 20-30", "B) 30-40", "C) 40-50", "B"));
            initialQuestions.add(new Question("Quem pintou 'A Última Ceia'?",
                    "A) Michelangelo", "B) Leonardo da Vinci", "C) Rafael", "B"));

            // ENTERTENIMENTO (25 perguntas)
            initialQuestions.add(new Question("Quem interpretou o Harry Potter nos filmes?",
                    "A) Daniel Radcliffe", "B) Rupert Grint", "C) Tom Felton", "A"));
            initialQuestions.add(new Question("Qual é o super-herói também conhecido como Homem-Aranha?",
                    "A) Peter Parker", "B) Bruce Wayne", "C) Clark Kent", "A"));
            initialQuestions.add(new Question("Em que ano foi lançado o primeiro iPhone?",
                    "A) 2005", "B) 2007", "C) 2010", "B"));
            initialQuestions.add(new Question("Qual é a rede social mais popular do mundo?",
                    "A) Facebook", "B) Instagram", "C) TikTok", "A"));
            initialQuestions.add(new Question("Quem é o criador da Microsoft?",
                    "A) Steve Jobs", "B) Bill Gates", "C) Mark Zuckerberg", "B"));
            initialQuestions.add(new Question("Quantos filmes tem a saga 'Star Wars' principal?",
                    "A) 8", "B) 9", "C) 10", "B"));
            initialQuestions.add(new Question("Qual é o jogo mais vendido de todos os tempos?",
                    "A) Minecraft", "B) GTA V", "C) Tetris", "A"));
            initialQuestions.add(new Question("Quem canta 'Shape of You'?",
                    "A) Ed Sheeran", "B) Justin Bieber", "C) Bruno Mars", "A"));
            initialQuestions.add(new Question("Quantas temporadas tem 'Game of Thrones'?",
                    "A) 7", "B) 8", "C) 9", "B"));
            initialQuestions.add(new Question("Qual é o filme com maior bilheteira da história?",
                    "A) Avatar", "B) Vingadores: Ultimato", "C) Titanic", "A"));
            initialQuestions.add(new Question("Quem é o criador da Apple?",
                    "A) Bill Gates", "B) Steve Jobs", "C) Mark Zuckerberg", "B"));
            initialQuestions.add(new Question("Quantos episódios tem 'Friends'?",
                    "A) 236", "B) 226", "C) 216", "A"));
            initialQuestions.add(new Question("Qual é o youtuber com mais subscritores?",
                    "A) PewDiePie", "B) MrBeast", "C) Dude Perfect", "B"));
            initialQuestions.add(new Question("Quem dirige a Marvel Studios?",
                    "A) J.J. Abrams", "B) Christopher Nolan", "C) Kevin Feige", "C"));
            initialQuestions.add(new Question("Quantos livros tem a saga 'Harry Potter'?",
                    "A) 6", "B) 7", "C) 8", "B"));
            initialQuestions.add(new Question("Qual é a série mais cara já produzida?",
                    "A) The Crown", "B) O Senhor dos Anéis", "C) Stranger Things", "B"));
            initialQuestions.add(new Question("Quem canta 'Bad Guy'?",
                    "A) Ariana Grande", "B) Billie Eilish", "C) Taylor Swift", "B"));
            initialQuestions.add(new Question("Quantos Oscars tem 'Titanic'?",
                    "A) 10", "B) 11", "C) 12", "B"));
            initialQuestions.add(new Question("Qual é o anime mais popular do mundo?",
                    "A) Dragon Ball", "B) Naruto", "C) One Piece", "C"));
            initialQuestions.add(new Question("Quem criou 'The Simpsons'?",
                    "A) Seth MacFarlane", "B) Matt Groening", "C) Trey Parker", "B"));
            initialQuestions.add(new Question("Quantas temporadas tem 'The Office' (US)?",
                    "A) 7", "B) 8", "C) 9", "C"));
            initialQuestions.add(new Question("Qual é o filme de animação da Disney mais recente?",
                    "A) Encanto", "B) Moana", "C) Frozen II", "A"));
            initialQuestions.add(new Question("Quem é o ator mais bem pago de Hollywood?",
                    "A) Tom Cruise", "B) Dwayne Johnson", "C) Robert Downey Jr.", "B"));
            initialQuestions.add(new Question("Quantas músicas tem o álbum 'Thriller'?",
                    "A) 7", "B) 8", "C) 9", "C"));
            initialQuestions.add(new Question("Qual é a plataforma de streaming mais popular?",
                    "A) Netflix", "B) Disney+", "C) Amazon Prime", "A"));


            Log.d(TAG, "Inserting " + initialQuestions.size() + " initial questions");
            database.questionDao().insertAllQuestions(initialQuestions);
            Log.d(TAG, "Initial questions inserted successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error inserting initial questions: " + e.getMessage(), e);
        }
    }

    private Question createFallbackQuestion() {
        Log.w(TAG, "Creating fallback question");
        return new Question(
                "Qual é a capital de Portugal?",
                "A) Porto",
                "B) Lisboa",
                "C) Coimbra",
                "B"
        );
    }

}