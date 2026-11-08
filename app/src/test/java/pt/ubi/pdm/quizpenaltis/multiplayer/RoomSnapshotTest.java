package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RoomSnapshotTest {

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Sala completa e válida, com os tipos que o Firebase devolve (Long, Boolean, String, Map). */
    private static Map<String, Object> fullRoom() {
        return map(
                "meta", map("host", "uidH", "guest", "uidG", "status", "playing", "v", 1L,
                        "created", 1000L, "winner", "guest", "reason", "score"),
                "players", map(
                        "host", map("uid", "uidH", "name", "Ana", "player", "Ronaldo",
                                "cost", 300L, "ready", true, "seen", 5000L),
                        "guest", map("uid", "uidG", "name", "Rui", "player", "Messi",
                                "cost", 250L, "ready", false, "seen", 6000L)),
                "toss", map("guestChoice", "heads", "first", "host"),
                "turns", map(
                        "0", map("shooter", "host", "q", 42L, "answer", 1L,
                                "shot", map("zone", 7L, "nonce", 123L),
                                "defend", map("zone", 2L, "nonce", 456L)),
                        "1", map("shooter", "guest", "q", 9L)));
    }

    @Test public void salaCompletaLidaCorretamente() {
        RoomSnapshot s = RoomSnapshot.parse(fullRoom());
        assertEquals("playing", s.status());
        assertEquals("uidH", s.hostUid());
        assertEquals("uidG", s.guestUid());
        assertEquals(Integer.valueOf(1), s.version());
        assertEquals(Long.valueOf(1000L), s.created());
        assertEquals(Role.GUEST, s.winner());
        assertEquals("score", s.reason());
        assertEquals("heads", s.guestChoice());
        assertEquals(Role.HOST, s.first());

        PlayerInfo h = s.host();
        assertNotNull(h);
        assertEquals("uidH", h.uid);
        assertEquals("Ana", h.name);
        assertEquals("Ronaldo", h.player);
        assertEquals(300, h.cost);
        assertTrue(h.ready);
        assertEquals(5000L, h.seen);
        PlayerInfo g = s.guest();
        assertNotNull(g);
        assertEquals("Rui", g.name);
        assertFalse(g.ready);

        TurnSnapshot t0 = s.turn(0);
        assertNotNull(t0);
        assertEquals(Role.HOST, t0.shooter);
        assertEquals(Integer.valueOf(42), t0.q);
        assertEquals(Integer.valueOf(1), t0.answer);
        assertEquals(Integer.valueOf(7), t0.shotZone);
        assertEquals(Long.valueOf(123L), t0.shotNonce);
        assertEquals(Integer.valueOf(2), t0.defendZone);
        assertEquals(Long.valueOf(456L), t0.defendNonce);

        TurnSnapshot t1 = s.turn(1);
        assertNotNull(t1);
        assertEquals(Role.GUEST, t1.shooter);
        assertEquals(Integer.valueOf(9), t1.q);
        assertNull(t1.answer);
        assertNull(t1.shotZone);
        assertNull(t1.defendNonce);

        assertNull(s.turn(2));
        assertNull(s.turn(-1));
        assertEquals(1, s.highestTurn());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> turn0(Map<String, Object> room) {
        return (Map<String, Object>) ((Map<String, Object>) room.get("turns")).get("0");
    }

    @Test public void zonaDeRemateForaDoIntervaloFicaNula() {
        Map<String, Object> room = fullRoom();
        turn0(room).put("shot", map("zone", 99L, "nonce", 123L));
        TurnSnapshot t = RoomSnapshot.parse(room).turn(0);
        assertNull(t.shotZone);
        assertEquals(Long.valueOf(123L), t.shotNonce);
    }

    @Test public void zonaDeDefesaForaDoIntervaloFicaNula() {
        Map<String, Object> room = fullRoom();
        turn0(room).put("defend", map("zone", 5L, "nonce", -1L));
        TurnSnapshot t = RoomSnapshot.parse(room).turn(0);
        assertNull(t.defendZone);
        assertNull(t.defendNonce);
    }

    @Test public void respostaForaDoIntervaloFicaNula() {
        Map<String, Object> room = fullRoom();
        turn0(room).put("answer", 7L);
        assertNull(RoomSnapshot.parse(room).turn(0).answer);
    }

    @Test public void respostaTempoEsgotadoEValida() {
        Map<String, Object> room = fullRoom();
        turn0(room).put("answer", -1L);
        assertEquals(Integer.valueOf(-1), RoomSnapshot.parse(room).turn(0).answer);
    }

    @Test public void numerosNaoInteirosOuDeTipoErradoFicamNulos() {
        Map<String, Object> room = fullRoom();
        turn0(room).put("q", 1.5);
        turn0(room).put("answer", "1");
        turn0(room).put("shooter", "ninguem");
        turn0(room).put("shot", "nao e mapa");
        TurnSnapshot t = RoomSnapshot.parse(room).turn(0);
        assertNull(t.q);
        assertNull(t.answer);
        assertNull(t.shooter);
        assertNull(t.shotZone);
        assertNull(t.shotNonce);
        assertEquals(Integer.valueOf(2), t.defendZone);
    }

    @Test public void nonceAcimaDe2Elevado53FicaNulo() {
        Map<String, Object> room = fullRoom();
        turn0(room).put("shot", map("zone", 3L, "nonce", Nonce.MAX + 1));
        assertNull(RoomSnapshot.parse(room).turn(0).shotNonce);
    }

    @SuppressWarnings("unchecked")
    @Test public void nomeCompridoPassaPorSanitize() {
        Map<String, Object> room = fullRoom();
        Map<String, Object> host = (Map<String, Object>)
                ((Map<String, Object>) room.get("players")).get("host");
        String longName = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn";
        host.put("name", longName);
        PlayerInfo h = RoomSnapshot.parse(room).host();
        assertEquals(PlayerName.sanitize(longName), h.name);
        assertEquals(12, h.name.length());
    }

    @Test public void valoresHostisDoMetaETossSaoIgnorados() {
        Map<String, Object> room = fullRoom();
        room.put("meta", map("status", "hacked", "v", "1", "winner", "eu", "reason", "porque sim",
                "host", 5L));
        room.put("toss", map("guestChoice", "lado", "first", 1L));
        RoomSnapshot s = RoomSnapshot.parse(room);
        assertNull(s.status());
        assertNull(s.version());
        assertNull(s.winner());
        assertNull(s.reason());
        assertNull(s.hostUid());
        assertNull(s.guestChoice());
        assertNull(s.first());
    }

    @Test public void jogadorQueNaoEMapaFicaNulo() {
        Map<String, Object> room = fullRoom();
        room.put("players", map("host", "x", "guest", 3L));
        RoomSnapshot s = RoomSnapshot.parse(room);
        assertNull(s.host());
        assertNull(s.guest());
    }

    @Test public void camposDoJogadorEmFaltaTemValoresSeguros() {
        Map<String, Object> room = fullRoom();
        room.put("players", map("host", map("cost", "muito", "ready", "sim")));
        PlayerInfo h = RoomSnapshot.parse(room).host();
        assertNotNull(h);
        assertNull(h.uid);
        assertNull(h.player);
        assertEquals(MultiplayerRules.DEFAULT_NAME, h.name);
        assertEquals(0, h.cost);
        assertFalse(h.ready);
        assertEquals(0L, h.seen);
    }

    @Test public void turnosComoListaTambemSaoLidos() {
        Map<String, Object> room = fullRoom();
        List<Object> turns = new ArrayList<>(Arrays.asList(
                map("shooter", "host", "q", 1L), null, map("shooter", "host", "q", 3L)));
        room.put("turns", turns);
        RoomSnapshot s = RoomSnapshot.parse(room);
        assertEquals(Integer.valueOf(1), s.turn(0).q);
        assertNull(s.turn(1));
        assertEquals(Integer.valueOf(3), s.turn(2).q);
        assertEquals(2, s.highestTurn());
    }

    @Test public void chavesDeTurnoInvalidasSaoIgnoradas() {
        Map<String, Object> room = fullRoom();
        room.put("turns", map("abc", map("q", 1L), "-3", map("q", 2L), "5", "lixo"));
        RoomSnapshot s = RoomSnapshot.parse(room);
        assertEquals(-1, s.highestTurn());
        assertNull(s.turn(5));
    }

    @Test public void vazioNuloOuTipoErradoNaoLancam() {
        for (Object v : new Object[]{null, new HashMap<String, Object>(), "texto", 42L,
                new ArrayList<Object>(), map("meta", "x", "players", 1L, "turns", true)}) {
            RoomSnapshot s = RoomSnapshot.parse(v);
            assertNotNull(s);
            assertNull(s.status());
            assertNull(s.host());
            assertNull(s.guest());
            assertNull(s.first());
            assertNull(s.turn(0));
            assertEquals(-1, s.highestTurn());
        }
    }

    @Test public void mapaComChavesNaoTextoNaoLanca() {
        Map<Object, Object> weird = new HashMap<>();
        weird.put(1L, "x");
        weird.put("meta", map("status", "waiting"));
        assertEquals("waiting", RoomSnapshot.parse(weird).status());
    }
}
