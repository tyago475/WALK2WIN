package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import org.junit.Test;

public class PlayerNameTest {
    @Test public void aparaEspacos() { assertEquals("Ana", PlayerName.sanitize("  Ana  ")); }

    @Test public void vazioNuloOuSimbolosDaoPadrao() {
        assertEquals("Jogador", PlayerName.sanitize(""));
        assertEquals("Jogador", PlayerName.sanitize(null));
        assertEquals("Jogador", PlayerName.sanitize("<>!"));
    }

    @Test public void limitaTamanho() {
        assertEquals(12, PlayerName.sanitize("abcdefghijklmnopqrstuvwxyzabcdefghijklmn").length());
    }

    @Test public void mantemAcentos() { assertEquals("João", PlayerName.sanitize("João!")); }
}
