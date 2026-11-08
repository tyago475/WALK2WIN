// Testes das regras de seguranca do Realtime Database (firebase/database.rules.json).
// Correm no emulador do Firebase: ver "Regras de seguranca do Firebase" no CLAUDE.md.
//
// Cada regra tem um teste "permite" e pelo menos um "recusa". Os "recusa" partem do
// mesmo estado que o "permite" correspondente e mudam so uma coisa, para que a recusa
// venha mesmo da regra testada (e nao de outra coisa qualquer).
'use strict';

const { describe, it, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const { ref, get, set, update, remove, serverTimestamp } = require('firebase/database');

const RULES_FILE = process.env.RULES_FILE || path.join(__dirname, '..', 'database.rules.json');
const PROJECT_ID = 'demo-walk2win';

const HOST = 'hostUid';
const GUEST = 'guestUid';
const OUTSIDER = 'outsiderUid';
const CODE = 'ABCD';
const ROOM = `rooms/${CODE}`;

const TWO_HOURS = 2 * 60 * 60 * 1000;
const MINUTE = 60 * 1000;

let testEnv;

function emulatorHostPort() {
  const env = process.env.FIREBASE_DATABASE_EMULATOR_HOST || '127.0.0.1:9000';
  const i = env.lastIndexOf(':');
  return { host: env.slice(0, i), port: Number(env.slice(i + 1)) };
}

/** Base de dados vista por `uid` (null = sem sessao). */
function db(uid) {
  return uid
    ? testEnv.authenticatedContext(uid).database()
    : testEnv.unauthenticatedContext().database();
}

/** Escreve o estado inicial ignorando as regras. */
async function seed(data) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await set(ref(ctx.database(), ROOM), data);
  });
}

/** Le um caminho ignorando as regras (para confirmar o que ficou escrito). */
async function peek(p) {
  let value;
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    value = (await get(ref(ctx.database(), p))).val();
  });
  return value;
}

function meta(overrides = {}) {
  return { host: HOST, status: 'waiting', v: 1, created: Date.now(), ...overrides };
}

function player(uid, overrides = {}) {
  return { uid, name: 'Ana', player: 'Ronaldo', cost: 100, ready: false, seen: Date.now(), ...overrides };
}

/** Sala criada pelo anfitriao, ainda sem convidado. */
function waitingRoom(metaOverrides = {}) {
  return { meta: meta(metaOverrides), players: { host: player(HOST) } };
}

/** Sala com os dois jogadores, em espera. */
function joinedRoom({ hostReady = false, guestReady = false, metaOverrides = {}, toss } = {}) {
  const room = {
    meta: meta({ guest: GUEST, ...metaOverrides }),
    players: {
      host: player(HOST, { ready: hostReady }),
      guest: player(GUEST, { name: 'Rui', ready: guestReady }),
    },
  };
  if (toss) room.toss = toss;
  return room;
}

/** Partida a decorrer: os dois prontos, moeda lancada (comeca o anfitriao). */
function playingRoom({ turns, hostSeen, guestSeen } = {}) {
  const room = joinedRoom({
    hostReady: true,
    guestReady: true,
    metaOverrides: { status: 'playing' },
    toss: { guestChoice: 'heads', first: 'host' },
  });
  if (hostSeen !== undefined) room.players.host.seen = hostSeen;
  if (guestSeen !== undefined) room.players.guest.seen = guestSeen;
  if (turns) room.turns = turns;
  return room;
}

/** Turno 0: remata o anfitriao (HOST), defende o convidado (GUEST). */
function turn0(extra = {}) {
  return { 0: { shooter: 'host', q: 3, ...extra } };
}

/** Turnos 0..last ja respondidos (resposta errada, sem remate). */
function answeredTurns(last) {
  const turns = {};
  for (let n = 0; n <= last; n++) {
    turns[n] = { shooter: n % 2 === 0 ? 'host' : 'guest', q: n, answer: 0 };
  }
  return turns;
}

function claim(winner, reason) {
  return { winner, reason, status: 'finished' };
}

before(async () => {
  const { host, port } = emulatorHostPort();
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    database: { host, port, rules: fs.readFileSync(RULES_FILE, 'utf8') },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearDatabase();
});

// ---------------------------------------------------------------------------
describe('autenticacao, raiz e lista de salas', () => {
  it('permite: utilizador autenticado le rooms/CODE/meta', async () => {
    await seed(waitingRoom());
    await assertSucceeds(get(ref(db(OUTSIDER), `${ROOM}/meta`)));
  });
  it('recusa: sem sessao nao le rooms/CODE/meta', async () => {
    await seed(waitingRoom());
    await assertFails(get(ref(db(null), `${ROOM}/meta`)));
  });
  it('recusa: ninguem le a raiz (nem o anfitriao)', async () => {
    await seed(waitingRoom());
    await assertFails(get(ref(db(HOST), '/')));
  });
  it('recusa: ninguem lista as salas (rooms)', async () => {
    await seed(waitingRoom());
    await assertFails(get(ref(db(HOST), 'rooms')));
    await assertFails(get(ref(db(OUTSIDER), 'rooms')));
  });
  it('recusa: escrever fora de rooms', async () => {
    await assertFails(set(ref(db(HOST), 'other/x'), 1));
  });
  it('recusa: sem sessao nao cria sala', async () => {
    await assertFails(set(ref(db(null), ROOM), { meta: meta({ created: serverTimestamp() }) }));
  });
});

// ---------------------------------------------------------------------------
describe('criar sala (meta)', () => {
  const fresh = (overrides = {}) => ({
    meta: { host: HOST, status: 'waiting', v: 1, created: serverTimestamp(), ...overrides },
  });

  it('permite: criar sala nova, com o criador como host', async () => {
    await assertSucceeds(set(ref(db(HOST), ROOM), fresh()));
    const m = await peek(`${ROOM}/meta`);
    if (m.host !== HOST || m.status !== 'waiting') throw new Error('meta inesperado');
  });
  it('permite: criar sala ja com o proprio players/host', async () => {
    await assertSucceeds(set(ref(db(HOST), ROOM), {
      ...fresh(),
      players: { host: player(HOST, { seen: serverTimestamp() }) },
    }));
  });
  it('recusa: sem sessao', async () => {
    await assertFails(set(ref(db(null), ROOM), fresh()));
  });
  it('recusa: host diferente de quem cria', async () => {
    await assertFails(set(ref(db(HOST), ROOM), fresh({ host: OUTSIDER })));
  });
  it('recusa: v diferente de 1', async () => {
    await assertFails(set(ref(db(HOST), ROOM), fresh({ v: 2 })));
  });
  it('recusa: comecar noutro estado que nao waiting', async () => {
    await assertFails(set(ref(db(HOST), ROOM), fresh({ status: 'playing' })));
  });
  it('recusa: created que nao seja a hora do servidor', async () => {
    await assertFails(set(ref(db(HOST), ROOM), fresh({ created: Date.now() - 3 * TWO_HOURS })));
  });
  it('recusa: criar ja com convidado ou vencedor', async () => {
    await assertFails(set(ref(db(HOST), ROOM), fresh({ guest: GUEST })));
    await assertFails(set(ref(db(HOST), ROOM), fresh({ winner: 'host' })));
  });
  it('recusa: codigo invalido (minusculas, I/O, 5 letras)', async () => {
    await assertFails(set(ref(db(HOST), 'rooms/abcd'), fresh()));
    await assertFails(set(ref(db(HOST), 'rooms/ABIO'), fresh()));
    await assertFails(set(ref(db(HOST), 'rooms/ABCDE'), fresh()));
  });
  it('recusa: campo desconhecido em meta', async () => {
    await assertFails(set(ref(db(HOST), ROOM), fresh({ extra: 1 })));
  });
  it('recusa: criar com players/guest ou turns', async () => {
    await assertFails(set(ref(db(HOST), ROOM), { ...fresh(), players: { guest: player(GUEST) } }));
    await assertFails(set(ref(db(HOST), ROOM), { ...fresh(), turns: turn0() }));
  });
  it('permite: recriar sala com mais de 2 h (qualquer autenticado)', async () => {
    await seed(playingRoom());
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await set(ref(ctx.database(), `${ROOM}/meta/created`), Date.now() - TWO_HOURS - MINUTE);
    });
    await assertSucceeds(set(ref(db(OUTSIDER), ROOM), {
      meta: { host: OUTSIDER, status: 'waiting', v: 1, created: serverTimestamp() },
    }));
    if ((await peek(`${ROOM}/players`)) !== null) throw new Error('jogadores antigos ficaram');
  });
  it('recusa: recriar sala com menos de 2 h (nem o proprio anfitriao)', async () => {
    await seed(playingRoom());
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await set(ref(ctx.database(), `${ROOM}/meta/created`), Date.now() - TWO_HOURS + MINUTE);
    });
    await assertFails(set(ref(db(OUTSIDER), ROOM), {
      meta: { host: OUTSIDER, status: 'waiting', v: 1, created: serverTimestamp() },
    }));
    await assertFails(set(ref(db(HOST), ROOM), fresh()));
  });
  it('recusa: recriar sala velha escrevendo so meta (ficariam os dados antigos)', async () => {
    await seed(playingRoom());
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await set(ref(ctx.database(), `${ROOM}/meta/created`), Date.now() - TWO_HOURS - MINUTE);
    });
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/meta`),
      { host: OUTSIDER, status: 'waiting', v: 1, created: serverTimestamp() }));
  });
  it('recusa: mudar host/v/created de uma sala existente', async () => {
    await seed(waitingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/host`), OUTSIDER));
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/v`), 1));
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/created`), serverTimestamp()));
  });
});

// ---------------------------------------------------------------------------
describe('entrar na sala (meta.guest)', () => {
  const joinUpdate = (uid) => ({
    'meta/guest': uid,
    'players/guest': player(uid, { name: 'Rui', seen: serverTimestamp() }),
  });

  it('permite: entrar numa sala em espera (meta.guest + players/guest de uma vez)', async () => {
    await seed(waitingRoom());
    await assertSucceeds(update(ref(db(GUEST), ROOM), joinUpdate(GUEST)));
  });
  it('permite: entrar escrevendo so meta/guest', async () => {
    await seed(waitingRoom());
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/meta/guest`), GUEST));
  });
  it('recusa: sem sessao', async () => {
    await seed(waitingRoom());
    await assertFails(set(ref(db(null), `${ROOM}/meta/guest`), GUEST));
  });
  it('recusa: lugar ja ocupado (segundo convidado)', async () => {
    await seed(joinedRoom());
    await assertFails(update(ref(db(OUTSIDER), ROOM), joinUpdate(OUTSIDER)));
  });
  it('recusa: o convidado reescrever meta.guest (uma so vez)', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/guest`), GUEST));
  });
  it('recusa: sair (apagar meta.guest)', async () => {
    await seed(joinedRoom());
    await assertFails(remove(ref(db(GUEST), `${ROOM}/meta/guest`)));
  });
  it('recusa: sala que ja nao esta em espera', async () => {
    // So meta/guest: o update com players/guest seria recusado tambem pela regra de players.
    await seed(waitingRoom({ status: 'playing' }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/guest`), GUEST));
    await assertFails(update(ref(db(GUEST), ROOM), joinUpdate(GUEST)));
  });
  it('recusa: sala com outra versao do protocolo (v != 1)', async () => {
    await seed(waitingRoom({ v: 2 }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/guest`), GUEST));
  });
  it('recusa: escrever o uid de outra pessoa como guest', async () => {
    await seed(waitingRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/guest`), OUTSIDER));
  });
  it('recusa: o anfitriao entrar como convidado da propria sala', async () => {
    await seed(waitingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/guest`), HOST));
  });
  it('recusa: entrar numa sala que nao existe', async () => {
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/guest`), GUEST));
  });
});

// ---------------------------------------------------------------------------
describe('ler a sala (so host e guest)', () => {
  const memberPaths = ['players', 'players/host', 'toss', 'turns/0/shooter', 'turns/0/q', 'turns/0/answer'];

  for (const p of memberPaths) {
    it(`permite: host e guest leem ${p}`, async () => {
      await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
      await assertSucceeds(get(ref(db(HOST), `${ROOM}/${p}`)));
      await assertSucceeds(get(ref(db(GUEST), `${ROOM}/${p}`)));
    });
    it(`recusa: terceiro e sem sessao nao leem ${p}`, async () => {
      await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
      await assertFails(get(ref(db(OUTSIDER), `${ROOM}/${p}`)));
      await assertFails(get(ref(db(null), `${ROOM}/${p}`)));
    });
  }
  it('recusa: terceiro nao le a sala inteira', async () => {
    await seed(playingRoom());
    await assertFails(get(ref(db(OUTSIDER), ROOM)));
  });
  it('recusa: ler a sala inteira ou turns de uma vez (nem membros: protegeria mal shot/defend)', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertFails(get(ref(db(HOST), ROOM)));
    await assertFails(get(ref(db(HOST), `${ROOM}/turns`)));
    await assertFails(get(ref(db(GUEST), `${ROOM}/turns/0`)));
  });
});

// ---------------------------------------------------------------------------
describe('players/$role', () => {
  it('permite: o convidado escreve o proprio registo', async () => {
    await seed(joinedRoom());
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/players/guest`),
      player(GUEST, { name: 'Rui', seen: serverTimestamp() })));
  });
  it('recusa: o convidado escreve o registo do anfitriao', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/host`),
      player(GUEST, { seen: serverTimestamp() })));
  });
  it('recusa: terceiro escreve players/guest (sala sem convidado ou com)', async () => {
    await seed(waitingRoom());
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/players/guest`),
      player(OUTSIDER, { seen: serverTimestamp() })));
    await seed(joinedRoom());
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/players/guest`),
      player(OUTSIDER, { seen: serverTimestamp() })));
  });
  it('recusa: uid no registo diferente de quem escreve', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/guest`),
      player(OUTSIDER, { seen: serverTimestamp() })));
  });
  it('recusa: papel que nao e host/guest', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/players/host2`),
      player(HOST, { seen: serverTimestamp() })));
  });
  it('recusa: registo incompleto ou com campo a mais', async () => {
    await seed(joinedRoom());
    const p = player(GUEST, { seen: serverTimestamp() });
    delete p.cost;
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/guest`), p));
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/guest`),
      { ...player(GUEST, { seen: serverTimestamp() }), admin: true }));
  });

  // Prepara a sala e devolve a escrita do registo do convidado com um campo alterado
  // (a asercao fica so sobre a escrita, nao sobre a preparacao).
  const field = async (name, value) => {
    await seed(joinedRoom());
    return () => set(ref(db(GUEST), `${ROOM}/players/guest`),
      player(GUEST, { seen: serverTimestamp(), [name]: value }));
  };
  it('permite: nome com 12 letras', async () => {
    await assertSucceeds((await field('name', 'ABCDEFGHIJKL'))());
  });
  it('recusa: nome com 13 letras', async () => {
    await assertFails((await field('name', 'ABCDEFGHIJKLM'))());
  });
  it('recusa: nome vazio ou que nao e texto', async () => {
    await assertFails((await field('name', ''))());
    await assertFails((await field('name', 42))());
  });
  it('permite: cost 0 e 1000', async () => {
    await assertSucceeds((await field('cost', 0))());
    await assertSucceeds((await field('cost', 1000))());
  });
  it('recusa: cost 1001, -1, 10.5 ou texto', async () => {
    await assertFails((await field('cost', 1001))());
    await assertFails((await field('cost', -1))());
    await assertFails((await field('cost', 10.5))());
    await assertFails((await field('cost', '100'))());
  });
  it('permite: ready booleano', async () => {
    await assertSucceeds((await field('ready', true))());
  });
  it('recusa: ready que nao e booleano', async () => {
    await assertFails((await field('ready', 'yes'))());
  });
  it('recusa: seen que nao e a hora do servidor (no registo)', async () => {
    await assertFails((await field('seen', Date.now()))());
  });
  it('recusa: player vazio', async () => {
    await assertFails((await field('player', ''))());
  });

  it('permite: seen = hora do servidor, pelo dono, durante a partida', async () => {
    await seed(playingRoom());
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/players/guest/seen`), serverTimestamp()));
  });
  it('recusa: seen com valor escolhido pelo cliente', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/guest/seen`), Date.now() + 60000));
  });
  it('recusa: escrever o seen do adversario', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/players/guest/seen`), serverTimestamp()));
  });
  it('recusa: apagar seen ou o proprio registo', async () => {
    await seed(joinedRoom());
    await assertFails(remove(ref(db(GUEST), `${ROOM}/players/guest/seen`)));
    await assertFails(remove(ref(db(GUEST), `${ROOM}/players/guest`)));
  });

  it('permite: marcar ready = true e voltar a false em espera', async () => {
    await seed(joinedRoom());
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/players/guest/ready`), true));
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/players/guest/ready`), false));
  });
  it('recusa: marcar ready do adversario', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/players/guest/ready`), true));
  });
  it('recusa: mudar ready depois de comecar', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/guest/ready`), false));
  });
  it('permite: mudar de jogador (cost) em espera e sem ready', async () => {
    await seed(joinedRoom());
    await assertSucceeds(update(ref(db(GUEST), `${ROOM}/players/guest`), { cost: 900, player: 'Messi' }));
  });
  it('recusa: mudar cost depois de ready (tem de desmarcar primeiro)', async () => {
    await seed(joinedRoom({ guestReady: true }));
    await assertFails(update(ref(db(GUEST), `${ROOM}/players/guest`), { cost: 900 }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/players/guest`),
      player(GUEST, { cost: 900, seen: serverTimestamp() })));
  });
  it('recusa: mudar cost durante a partida', async () => {
    await seed(playingRoom());
    await assertFails(update(ref(db(GUEST), `${ROOM}/players/guest`), { cost: 900 }));
  });
});

// ---------------------------------------------------------------------------
describe('meta.status waiting -> playing', () => {
  it('permite: anfitriao, com os dois ready', async () => {
    await seed(joinedRoom({ hostReady: true, guestReady: true }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/meta/status`), 'playing'));
  });
  it('recusa: o convidado (mesmo com os dois ready)', async () => {
    await seed(joinedRoom({ hostReady: true, guestReady: true }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/status`), 'playing'));
  });
  it('recusa: terceiro', async () => {
    await seed(joinedRoom({ hostReady: true, guestReady: true }));
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/meta/status`), 'playing'));
  });
  it('recusa: convidado nao ready', async () => {
    await seed(joinedRoom({ hostReady: true, guestReady: false }));
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/status`), 'playing'));
  });
  it('recusa: anfitriao nao ready', async () => {
    await seed(joinedRoom({ hostReady: false, guestReady: true }));
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/status`), 'playing'));
  });
  it('recusa: sem convidado', async () => {
    const room = waitingRoom();
    room.players.host.ready = true;
    await seed(room);
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/status`), 'playing'));
  });
  it('recusa: voltar de playing para waiting, ou valor desconhecido', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/status`), 'waiting'));
    await seed(joinedRoom({ hostReady: true, guestReady: true }));
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/status`), 'ready'));
  });
  it('recusa: finished sem winner', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/status`), 'finished'));
  });
});

// ---------------------------------------------------------------------------
describe('toss', () => {
  it('permite: o convidado escolhe heads/tails', async () => {
    await seed(joinedRoom());
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/toss/guestChoice`), 'tails'));
  });
  it('recusa: o anfitriao ou um terceiro escolhem', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/toss/guestChoice`), 'tails'));
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/toss/guestChoice`), 'tails'));
  });
  it('recusa: escolher duas vezes', async () => {
    await seed(joinedRoom({ toss: { guestChoice: 'heads' } }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/toss/guestChoice`), 'tails'));
  });
  it('recusa: valor que nao e heads/tails', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/toss/guestChoice`), 'edge'));
  });
  it('permite: o anfitriao escreve first depois de guestChoice', async () => {
    await seed(joinedRoom({ toss: { guestChoice: 'heads' } }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/toss/first`), 'guest'));
  });
  it('recusa: first antes de guestChoice', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/toss/first`), 'guest'));
  });
  it('recusa: o convidado escreve first', async () => {
    await seed(joinedRoom({ toss: { guestChoice: 'heads' } }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/toss/first`), 'guest'));
  });
  it('recusa: first duas vezes', async () => {
    await seed(joinedRoom({ toss: { guestChoice: 'heads', first: 'host' } }));
    await assertFails(set(ref(db(HOST), `${ROOM}/toss/first`), 'guest'));
  });
  it('recusa: first com valor que nao e host/guest', async () => {
    await seed(joinedRoom({ toss: { guestChoice: 'heads' } }));
    await assertFails(set(ref(db(HOST), `${ROOM}/toss/first`), 'bot'));
  });
  it('recusa: campo desconhecido em toss', async () => {
    await seed(joinedRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/toss/seed`), 1));
  });
});

// ---------------------------------------------------------------------------
describe('turns/$n: shooter e q', () => {
  it('permite: o anfitriao abre o turno 0 (set de {shooter, q})', async () => {
    await seed(playingRoom());
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'host', q: 3 }));
  });
  it('permite: o anfitriao abre o turno 1 com update (shooter = o outro)', async () => {
    await seed(playingRoom({ turns: answeredTurns(0) }));
    await assertSucceeds(update(ref(db(HOST), `${ROOM}/turns/1`), { shooter: 'guest', q: 7 }));
  });
  it('recusa: o convidado abre um turno', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0`), { shooter: 'host', q: 3 }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/q`), 3));
  });
  it('recusa: terceiro abre um turno', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/turns/0`), { shooter: 'host', q: 3 }));
  });
  it('recusa: reescrever shooter ou q (uma so vez)', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shooter`), 'host'));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/q`), 4));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'host', q: 4 }));
  });
  it('recusa: shooter que nao respeita a ordem (turno 0 = first)', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'guest', q: 3 }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/1`), { shooter: 'host', q: 3 }));
  });
  it('recusa: abrir turno antes de lancar a moeda', async () => {
    const room = playingRoom();
    delete room.toss;
    await seed(room);
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/1`), { shooter: 'guest', q: 3 }));
  });
  it('recusa: abrir turno com a sala em espera', async () => {
    await seed(joinedRoom({ toss: { guestChoice: 'heads', first: 'host' } }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'host', q: 3 }));
  });
  it('recusa: q negativo ou nao inteiro; indice de turno invalido', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'host', q: -1 }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'host', q: 1.5 }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/x0`), { shooter: 'host', q: 1 }));
  });
  it('recusa: abrir turno ja com answer/shot', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0`), { shooter: 'host', q: 3, answer: 1 }));
  });
});

// ---------------------------------------------------------------------------
describe('turns/$n/answer', () => {
  it('permite: o atirador responde (1) e com tempo esgotado (-1)', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0/answer`), 1));
    await seed(playingRoom({ turns: turn0() }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0/answer`), -1));
  });
  it('recusa: o defensor ou um terceiro respondem', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/answer`), 1));
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/turns/0/answer`), 1));
  });
  it('recusa: responder duas vezes', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 0 }) }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/answer`), 1));
  });
  it('recusa: answer 7, -2 ou 1.5', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/answer`), 7));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/answer`), -2));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/answer`), 1.5));
  });
  it('recusa: responder num turno que nao foi aberto', async () => {
    await seed(playingRoom());
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/answer`), 1));
  });
});

// ---------------------------------------------------------------------------
describe('turns/$n/shot e defend (escrita unica)', () => {
  const shot = { zone: 7, nonce: 123456789 };
  const defend = { zone: 4, nonce: 987654321 };

  it('permite: o atirador remata depois de responder', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0/shot`), shot));
  });
  it('permite: zona 0 e 14', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone: 14, nonce: 0 }));
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone: 0, nonce: 1 }));
  });
  it('recusa: segundo shot (nao se reescreve, nem a zona sozinha)', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, shot }) }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone: 3, nonce: 1 }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot/zone`), 3));
    await assertFails(remove(ref(db(HOST), `${ROOM}/turns/0/shot`)));
  });
  it('recusa: shot antes de answer', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), shot));
  });
  it('recusa: shot depois de tempo esgotado (answer -1)', async () => {
    await seed(playingRoom({ turns: turn0({ answer: -1 }) }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), shot));
  });
  it('recusa: o defensor ou um terceiro escrevem shot', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/shot`), shot));
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/turns/0/shot`), shot));
  });
  it('recusa: zona 99, 15, -1 ou 2.5', async () => {
    for (const zone of [99, 15, -1, 2.5]) {
      await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
      await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone, nonce: 1 }));
    }
  });
  it('recusa: shot sem nonce, nonce nao inteiro, ou campo a mais', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone: 7 }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone: 7, nonce: 0.5 }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/shot`), { zone: 7, nonce: 1, goal: true }));
  });

  it('permite: o defensor defende depois de answer (zona 4)', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/turns/0/defend`), defend));
  });
  it('permite: no turno 1 o anfitriao e quem defende', async () => {
    await seed(playingRoom({ turns: { ...answeredTurns(0), 1: { shooter: 'guest', q: 1, answer: 2 } } }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/1/defend`), defend));
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/turns/1/shot`), shot));
  });
  it('recusa: o atirador ou um terceiro escrevem defend', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/defend`), defend));
    await assertFails(set(ref(db(OUTSIDER), `${ROOM}/turns/0/defend`), defend));
  });
  it('recusa: segundo defend', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, defend }) }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/defend`), { zone: 0, nonce: 5 }));
  });
  it('recusa: defend antes de answer', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/defend`), defend));
  });
  it('recusa: defend zona 5 ou 99', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1 }) }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/defend`), { zone: 5, nonce: 1 }));
    await assertFails(set(ref(db(GUEST), `${ROOM}/turns/0/defend`), { zone: 99, nonce: 1 }));
  });
});

// ---------------------------------------------------------------------------
describe('revelacao depois do compromisso', () => {
  const shot = { zone: 7, nonce: 11 };
  const defend = { zone: 4, nonce: 22 };

  it('recusa: o defensor le shot antes de escrever defend', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, shot }) }));
    await assertFails(get(ref(db(GUEST), `${ROOM}/turns/0/shot`)));
  });
  it('permite: o defensor le shot depois de escrever defend', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, shot }) }));
    await assertSucceeds(set(ref(db(GUEST), `${ROOM}/turns/0/defend`), defend));
    const s = await assertSucceeds(get(ref(db(GUEST), `${ROOM}/turns/0/shot`)));
    if (s.val().zone !== 7) throw new Error('shot errado');
  });
  it('recusa: o atirador le defend antes de escrever shot', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, defend }) }));
    await assertFails(get(ref(db(HOST), `${ROOM}/turns/0/defend`)));
  });
  it('permite: o atirador le defend depois de escrever shot', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, defend }) }));
    await assertSucceeds(set(ref(db(HOST), `${ROOM}/turns/0/shot`), shot));
    await assertSucceeds(get(ref(db(HOST), `${ROOM}/turns/0/defend`)));
  });
  it('permite: cada um le sempre o seu proprio movimento', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, shot }) }));
    await assertSucceeds(get(ref(db(HOST), `${ROOM}/turns/0/shot`)));
    await seed(playingRoom({ turns: turn0({ answer: 1, defend }) }));
    await assertSucceeds(get(ref(db(GUEST), `${ROOM}/turns/0/defend`)));
  });
  it('recusa: terceiro nao le shot nem defend, mesmo com os dois escritos', async () => {
    await seed(playingRoom({ turns: turn0({ answer: 1, shot, defend }) }));
    await assertFails(get(ref(db(OUTSIDER), `${ROOM}/turns/0/shot`)));
    await assertFails(get(ref(db(OUTSIDER), `${ROOM}/turns/0/defend`)));
  });
});

// ---------------------------------------------------------------------------
describe('meta.winner / reason / finished', () => {
  const now = () => Date.now();

  it('permite: desistencia do adversario (sem sinal ha 31 s)', async () => {
    await seed(playingRoom({ guestSeen: now() - 31000 }));
    await assertSucceeds(update(ref(db(HOST), `${ROOM}/meta`), claim('host', 'forfeit')));
  });
  it('recusa: desistencia com o adversario sem sinal ha so 29 s', async () => {
    await seed(playingRoom({ guestSeen: now() - 29000 }));
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), claim('host', 'forfeit')));
  });
  it('permite: o convidado reclama desistencia do anfitriao (31 s)', async () => {
    await seed(playingRoom({ hostSeen: now() - 31000 }));
    await assertSucceeds(update(ref(db(GUEST), `${ROOM}/meta`), claim('guest', 'forfeit')));
  });
  it('recusa: o convidado reclama desistencia com o anfitriao vivo (seen agora)', async () => {
    await seed(playingRoom({ hostSeen: now() }));
    await assertFails(update(ref(db(GUEST), `${ROOM}/meta`), claim('guest', 'forfeit')));
  });
  it('recusa: desistencia baseada no proprio seen (adversario vivo, eu sem sinal)', async () => {
    await seed(playingRoom({ hostSeen: now() - 60000, guestSeen: now() }));
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), claim('host', 'forfeit')));
  });
  it('recusa: reclamar winner com o papel do outro', async () => {
    await seed(playingRoom({ hostSeen: now() - 60000, guestSeen: now() - 60000 }));
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), claim('guest', 'forfeit')));
    await assertFails(update(ref(db(GUEST), `${ROOM}/meta`), claim('host', 'forfeit')));
  });
  it('recusa: terceiro reclama winner', async () => {
    await seed(playingRoom({ hostSeen: now() - 60000, guestSeen: now() - 60000 }));
    await assertFails(update(ref(db(OUTSIDER), `${ROOM}/meta`), claim('host', 'forfeit')));
  });

  it('permite: vitoria por score depois de pelo menos 6 turnos', async () => {
    await seed(playingRoom({ turns: answeredTurns(5) }));
    await assertSucceeds(update(ref(db(GUEST), `${ROOM}/meta`), claim('guest', 'score')));
  });
  it('recusa: vitoria por score antes de 6 turnos (impossivel acabar antes)', async () => {
    await seed(playingRoom({ turns: answeredTurns(4) }));
    await assertFails(update(ref(db(GUEST), `${ROOM}/meta`), claim('guest', 'score')));
  });
  it('recusa: score com o papel do outro', async () => {
    await seed(playingRoom({ turns: answeredTurns(5) }));
    await assertFails(update(ref(db(GUEST), `${ROOM}/meta`), claim('host', 'score')));
  });

  it('recusa: winner sem status finished, ou sem reason', async () => {
    await seed(playingRoom({ turns: answeredTurns(5) }));
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), { winner: 'host', reason: 'score' }));
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), { winner: 'host', status: 'finished' }));
  });
  it('recusa: reason que nao e score/forfeit', async () => {
    await seed(playingRoom({ turns: answeredTurns(5), guestSeen: now() - 60000 }));
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), claim('host', 'quit')));
  });
  it('recusa: reason sozinho', async () => {
    await seed(playingRoom({ turns: answeredTurns(5) }));
    await assertFails(set(ref(db(HOST), `${ROOM}/meta/reason`), 'score'));
  });
  it('recusa: winner com a sala em espera', async () => {
    const room = joinedRoom({ hostReady: true, guestReady: true });
    room.players.guest.seen = now() - 60000;
    await seed(room);
    await assertFails(update(ref(db(HOST), `${ROOM}/meta`), claim('host', 'forfeit')));
  });
  it('recusa: segundo winner depois de acabada', async () => {
    const room = playingRoom({ turns: answeredTurns(5), hostSeen: now() - 60000 });
    room.meta = { ...room.meta, status: 'finished', winner: 'host', reason: 'score' };
    await seed(room);
    await assertFails(update(ref(db(GUEST), `${ROOM}/meta`), claim('guest', 'forfeit')));
    await assertFails(set(ref(db(GUEST), `${ROOM}/meta/winner`), 'guest'));
  });
  it('recusa: jogar depois de acabada', async () => {
    const room = playingRoom({ turns: turn0() });
    room.meta = { ...room.meta, status: 'finished', winner: 'host', reason: 'score' };
    await seed(room);
    await assertFails(set(ref(db(HOST), `${ROOM}/turns/0/answer`), 1));
  });
});

// ---------------------------------------------------------------------------
describe('apagar a sala', () => {
  it('permite: o anfitriao apaga a sala', async () => {
    await seed(playingRoom());
    await assertSucceeds(remove(ref(db(HOST), ROOM)));
    if ((await peek(ROOM)) !== null) throw new Error('sala ainda existe');
  });
  it('recusa: o convidado ou um terceiro apagam a sala', async () => {
    await seed(playingRoom());
    await assertFails(remove(ref(db(GUEST), ROOM)));
    await assertFails(remove(ref(db(OUTSIDER), ROOM)));
    await assertFails(remove(ref(db(null), ROOM)));
  });
  it('recusa: o anfitriao apagar so partes (meta, jogador do outro, turnos)', async () => {
    await seed(playingRoom({ turns: turn0() }));
    await assertFails(remove(ref(db(HOST), `${ROOM}/meta`)));
    await assertFails(remove(ref(db(HOST), `${ROOM}/players/guest`)));
    await assertFails(remove(ref(db(HOST), `${ROOM}/turns`)));
  });
});
