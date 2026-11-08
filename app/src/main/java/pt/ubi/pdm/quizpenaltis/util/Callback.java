package pt.ubi.pdm.quizpenaltis.util;

/** Resposta assíncrona com um valor (ex.: a pergunta que chega da rede, o resultado de um remate). */
public interface Callback<T> {
    void on(T value);
}
