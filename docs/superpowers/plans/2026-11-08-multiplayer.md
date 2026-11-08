# Multiplayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar o modo "Jogar com um amigo" (2 telemóveis, pela internet) ao Walk2Win, sem alterar o singleplayer.

**Architecture:** O `GameActivity` deixa de falar diretamente com o bot e passa a falar com uma interface `Opponent`, com duas implementações: `BotOpponent` (o comportamento atual, extraído sem mudanças) e `RemoteOpponent` (Firebase Realtime Database). A lógica de regras do multiplayer (código da sala, sorteio de zonas, resolução do remate, desistência) fica em classes sem Android, testadas. As jogadas são "trancadas" às cegas na base de dados e o resultado é calculado nos dois telemóveis.

**Tech Stack:** Java, Android (minSdk 23), Firebase BoM (Auth anónima + Realtime Database, região `europe-west1`), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-08-multiplayer-design.md`

## Global Constraints

- `minSdk` sobe de 21 para **23** (aprovado pelo autor). `targetSdk`/`compileSdk` ficam em 36.
- Firebase plano gratuito; **Realtime Database** + **autenticação anónima**; dados na **Europa**. URL da base: `https://walk2win-c8f21-default-rtdb.europe-west1.firebasedatabase.app` (não é o URL por omissão do SDK: tem de ser passado explicitamente). Projeto `walk2win-c8f21`, pacote `pt.ubi.pdm.quizpenaltis`.
- O singleplayer **não muda** (comportamento, ecrãs, testes). Os testes unitários atuais (22) continuam a passar.
- Código da sala: **4 letras**. Pergunta: **10 s**. Desistência: **30 s** sem sinal de vida do adversário ⇒ o outro ganha. Quem sai perde os pontos gastos.
- Pontos: igual ao singleplayer (preço do jogador, sem reembolso depois de a partida começar, vencedor sem prémio). Cobrança **quando a partida começa**, só com `StepCounter.spendPoints()` (única fonte do saldo). Se um não tiver pontos nesse momento, a partida não começa e ninguém paga.
- Regras sem Android em classes puras com testes (como `PenaltyShootout`). Textos em `strings.xml`, cores só por tokens, estilos `Widget.W2W.*`/`TextAppearance.W2W.*`, `SystemBars.applyPadding(root)` nos ecrãs novos, sem emojis como ícones, sem números mágicos (constantes em `MultiplayerRules`).
- Testes só correm numa cópia do projeto com caminho ASCII (o `º` de `3ºano` parte o runner). Comando-padrão ("TESTES"): copiar o projeto para `C:/w2w_test` (sem `.git`, `build`, `.gradle`) e correr aí `./gradlew testDebugUnitTest -q` com `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. Esperado: todos os testes a passar.
- Build: `./gradlew assembleDebug -q` com o mesmo `JAVA_HOME`.
- Emulador: `emulator -avd Medium_Phone_API_36.1 -no-snapshot -no-snapshot-save -no-audio -no-boot-anim -gpu swiftshader_indirect -memory 2048`.
- Cada tarefa termina com commit (mensagem em português, terminada com a linha `Co-Authored-By` do harness). Não fazer push sem o autor pedir.

## Decisões deste plano (o spec deixava em aberto)

**Modelo de dados** (`rooms/{CODE}`; `CODE` = 4 letras):
- `meta`: `{host: uid, guest: uid|null, status: "waiting"|"playing"|"finished", v: 1, created: TIMESTAMP, winner: "host"|"guest"|null, reason: "score"|"forfeit"|null}`
- `players/{host|guest}`: `{uid, name, player (nome no PlayerCatalog), cost, ready: bool, seen: TIMESTAMP}`
- `toss`: `{guestChoice: "heads"|"tails", first: "host"|"guest"}` (o convidado escolhe cara/coroa; o telemóvel do anfitrião lança e escreve `first`)
- `turns/{n}`: `{shooter: "host"|"guest", q: int, answer: 0|1|2|-1, shot: {zone: 0..14, nonce: long}, defend: {zone: 0..4, nonce: long}}`. `q` e `shooter` são escritos pelo anfitrião ao abrir o turno; `answer` e `shot` pelo atirador (`-1` = tempo esgotado); `defend` por quem defende.

**Fluxo de um turno `n`:** o anfitrião abre `turns/n` (`shooter = TurnOrder.shooterOf(n, first)`, `q` novo sem repetir). O atirador responde (`answer`). Se a resposta for errada (calculada localmente por ambos) o turno acaba sem golo. Se certa: o atirador escreve `shot`, quem defende escreve `defend`, cada um **às cegas**; quando os dois existem ambos calculam o resultado e aplicam `PenaltyShootout.recordPlayerShot/recordBotShot` (o "bot" do `PenaltyShootout` é o adversário) + `evaluate()`. O anfitrião abre o turno seguinte depois da transição.

**Resolução do remate:** golo ⇔ `zone ∈ GreenZones.pick(seed, GameRules.greenZonesForCost(custoDoAtirador))` **e** `DefenseZone.covering(zone) != zonaDeDefesa`. `seed = shooterNonce XOR defenderNonce`: nenhum dos dois o conhece antes de se comprometer, e nenhum o controla sozinho (as zonas verdes continuam a ser "sorte", como no singleplayer).
`DefenseZone.covering(GoalZone)`: coluna 3 (índices 2 de cada fila) → `CENTER`; linhas 1-2 colunas 1-2 → `TOP_LEFT`; linhas 1-2 colunas 4-5 → `TOP_RIGHT`; linha 3 colunas 1-2 → `BOTTOM_LEFT`; linha 3 colunas 4-5 → `BOTTOM_RIGHT` (índice da zona: `row = ordinal / 5`, `col = ordinal % 5`).

**Perguntas:** `q` é o índice na lista de `QuestionLoader.loadFromAssets(context)` (ordem estável entre telemóveis com a mesma versão da app); `meta.v = 1` é a versão do protocolo e a sala recusa entrar se a versão for diferente. `answer` é o índice da opção na ordem original A/B/C.

**Sinal de vida e desistência:** cada telemóvel escreve `players/{eu}/seen = ServerValue.TIMESTAMP` a cada `HEARTBEAT_MS` durante a partida. Quem vê o adversário sem sinal há mais de `FORFEIT_MS` (usando `.info/serverTimeOffset`) escreve `meta.winner = eu, reason = "forfeit"`; as regras só deixam escrever isto se `now - seen_do_adversário > 30000`.

**Limpeza (desvio do spec: "expira sozinha"):** o plano gratuito do Firebase não tem temporizadores. Em vez disso: o anfitrião apaga a sala no fim da partida e, enquanto está à espera, `onDisconnect().removeValue()`; as regras permitem **recriar** uma sala cujo `created` tenha mais de 2 h. Uma sala abandonada fica no máximo a ocupar uns bytes até alguém reusar o código.

**Limitação conhecida:** entre `ready` e `playing` o saldo de um jogador pode mudar (meia-noite). Cada telemóvel verifica o saldo ao marcar `ready` e cobra ao ver `playing`; se a cobrança falhar nessa janela rara, esse jogador desiste (perde) e o outro ganha.

**Ordem de execução:** T1, T2, T5, T3, T4, T6, T7, T8, T9 (T5 não depende de Firebase; T3 precisa do Node e T4 das regras publicadas).

## Review Focus

1. Código de sala inexistente, cheio, de versão diferente ou mal escrito ("ab1", espaços, minúsculas) → mensagem clara, sem crash, sem cobrança. Testado em `RoomCodeTest` e `RoomSnapshotTest`.
2. Adversário desliga a meio da pergunta/remate → vitória por desistência aos 30 s (não antes, não depois). Testado em `ForfeitRuleTest`.
3. Toque duplo / escrita duplicada (dois `shot`, dois `answer`) → a segunda é recusada pelas regras e ignorada pela app. Testado nas regras (Task 3) e com guarda `isProcessingClick` (Task 7).
4. Dados hostis na sala (zona 99, `answer` 7, nome gigante, campos em falta) → ignorados, sem crash. Testado em `RoomSnapshotTest` e `PlayerNameTest`.
5. App fica em segundo plano < 30 s a meio da partida → ao voltar retoma a mesma partida (listeners re-ligados); > 30 s → o adversário já pode ter ganho. Verificado na Task 8.

---

### Task 1: Firebase ligado (dependências, minSdk 23, login anónimo)

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts` (raiz), `app/build.gradle.kts` (minSdk 23, plugin, dependências)
- Create: `app/google-services.json` (copiado de `C:\Users\tiago\Downloads\google-services.json`; é commitado, não é segredo)
- Create: `app/src/main/java/pt/ubi/pdm/quizpenaltis/multiplayer/FirebaseConfig.java`, `.../multiplayer/AuthManager.java`
- Modify: `AndroidManifest.xml` (`<uses-permission android:name="android.permission.INTERNET" />`)

**Interfaces:**
- Produces: `FirebaseConfig.DATABASE_URL` (String); `FirebaseConfig.database()` → `com.google.firebase.database.FirebaseDatabase` já com o URL; `AuthManager.signIn(AuthManager.Callback cb)` onde `interface Callback { void onSuccess(String uid); void onError(Exception e); }` (reutiliza a sessão se já existir).

- [ ] **Step 1:** Em `libs.versions.toml` acrescentar o plugin `com.google.gms.google-services` (versão estável atual) e as bibliotecas `firebase-bom`, `firebase-auth`, `firebase-database` (sem versão individual; o BoM manda). Aplicar o plugin na raiz (`apply false`) e no módulo `app`. `minSdk = 23`.
- [ ] **Step 2:** Copiar o `google-services.json` para `app/`. Confirmar que `project_id` é `walk2win-c8f21` e o pacote `pt.ubi.pdm.quizpenaltis`.
- [ ] **Step 3:** Implementar `FirebaseConfig` (URL fixo; `database()` devolve `FirebaseDatabase.getInstance(DATABASE_URL)`) e `AuthManager.signIn` com `FirebaseAuth.signInAnonymously()`.
- [ ] **Step 4:** Verificação temporária (não commitar): na `MainActivity.onCreate` chamar `AuthManager.signIn` e fazer `Log.d("AuthSmoke", uid)`. Build + instalar + abrir. Esperado: `adb logcat -d | grep AuthSmoke` mostra um uid. Remover a chamada.
- [ ] **Step 5:** Pedir ao autor (passo dele, na Google Cloud Console → APIs e serviços → Credenciais): restringir a chave de API do projeto ao pacote `pt.ubi.pdm.quizpenaltis` (e ao SHA-1 de debug). Registar no `CLAUDE.md` que `google-services.json` está commitado de propósito.
- [ ] **Step 6:** TESTES (os 22 atuais passam) e commit.

---

### Task 2: Regras puras do multiplayer (TDD)

**Files:**
- Create (tudo em `app/src/main/java/pt/ubi/pdm/quizpenaltis/multiplayer/` salvo indicação): `Role.java`, `RoomCode.java`, `TurnOrder.java`, `GreenZones.java`, `ShotResolution.java`, `ForfeitRule.java`, `PlayerName.java`, `MultiplayerRules.java`
- Modify: `.../game/DefenseZone.java` (acrescentar `covering`)
- Test: `app/src/test/java/pt/ubi/pdm/quizpenaltis/multiplayer/` (`RoomCodeTest`, `TurnOrderTest`, `GreenZonesTest`, `ShotResolutionTest`, `ForfeitRuleTest`, `PlayerNameTest`) e `.../game/DefenseZoneTest.java`

**Interfaces (Produces):**
- `enum Role { HOST, GUEST; Role other(); String key(); static Role fromKey(String) }` (`key()` = `"host"`/`"guest"`; `fromKey` devolve `null` se inválido)
- `RoomCode`: `static final int LENGTH = 4`; `static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ"` (sem `I` nem `O`); `static String generate(java.util.Random r)`; `static String normalize(String input)` (trim + maiúsculas; `""` se `null`); `static boolean isValid(String code)` (exatamente 4 letras do alfabeto)
- `TurnOrder.shooterOf(int turn, Role first)` → `first` nos turnos pares, `first.other()` nos ímpares
- `GreenZones.pick(long seed, int count)` → `Set<GoalZone>` (`Collections.shuffle(todas, new Random(seed))`, primeiras `count`); `GreenZones.combine(long shooterNonce, long defenderNonce)` → XOR
- `DefenseZone.covering(GoalZone)` conforme a tabela do plano
- `ShotResolution.isGoal(GoalZone shot, DefenseZone defense, Set<GoalZone> greens)`
- `ForfeitRule.hasForfeited(long serverNowMs, long opponentSeenMs)` → `serverNowMs - opponentSeenMs > MultiplayerRules.FORFEIT_MS`
- `PlayerName.sanitize(String raw)` → trim, só letras/dígitos/espaço, no máximo `MultiplayerRules.NAME_MAX_LENGTH` caracteres, `MultiplayerRules.DEFAULT_NAME` ("Jogador") se ficar vazio
- `MultiplayerRules`: `PROTOCOL_VERSION = 1`, `FORFEIT_MS = 30_000`, `HEARTBEAT_MS = 5_000`, `ROOM_REUSE_AFTER_MS = 2 * 60 * 60 * 1000`, `NAME_MAX_LENGTH = 12`, `DEFAULT_NAME = "Jogador"`

- [ ] **Step 1:** Escrever os testes, com estes casos mínimos: `RoomCodeTest` — `generate` com `Random(1)` produz 4 letras válidas e nunca `I`/`O` em 1000 códigos; `normalize(" ab cd ")` não é válido, `normalize("abcd")` → `"ABCD"`; `isValid("AB1D")`, `isValid("ABC")`, `isValid("ABCDE")`, `isValid(null)` são `false`. `TurnOrderTest` — turnos 0..5 com `first=GUEST` dão G,H,G,H,G,H. `GreenZonesTest` — mesma `seed` e `count` dão o mesmo conjunto em duas chamadas; `count` 0 → vazio, 14 → 14 zonas; `combine(a,b) == combine(b,a)`. `DefenseZoneTest` — `covering(TOP_CENTER)==CENTER`, `covering(TOP_LEFT_CENTER)==TOP_LEFT`, `covering(MIDDLE_RIGHT)==TOP_RIGHT`, `covering(BOTTOM_LEFT)==BOTTOM_LEFT`, `covering(BOTTOM_RIGHT_CENTER)==BOTTOM_RIGHT`, e todas as 15 zonas têm cobertura. `ShotResolutionTest` — zona verde sem defesa que a cubra = golo; zona verde coberta = defendido; zona não verde = falha mesmo sem defesa. `ForfeitRuleTest` — 30 000 ms exatos **não** desiste; 30 001 desiste. `PlayerNameTest` — `"  Ana  "`→`"Ana"`, `""`/`null`/`"<>!"`→`"Jogador"`, 40 letras → 12.
- [ ] **Step 2:** TESTES. Esperado: **falham** (classes inexistentes).
- [ ] **Step 3:** Implementar as classes com as assinaturas acima (as classes `multiplayer` sem importar `android.*`; `DefenseZone` e `GoalZone` já existem).
- [ ] **Step 4:** TESTES. Esperado: todos passam (22 antigos + novos).
- [ ] **Step 5:** Commit.

---

### Task 3: Regras de segurança do Firebase (+ testes)

**Files:**
- Create: `firebase/database.rules.json`, `firebase/firebase.json`, `firebase/rules-test/package.json`, `firebase/rules-test/rules.test.js`
- Modify: `CLAUDE.md` (como publicar e testar as regras)

**Interfaces:** consome o modelo de dados do plano. Produz um ficheiro de regras que é a fonte de verdade (publicado na consola pelo autor).

Regras a impor (cada uma com um teste "permite" e outro "recusa", com `auth.uid` de `hostUid`, `guestUid` e `outsiderUid`):
- Tudo exige `auth != null`; a raiz e a lista `rooms` **não** se podem ler (nada de listar salas).
- `rooms/$code/meta`: criar só se não existir **ou** `now - created > 7200000`; o criador fica `host`. Entrar: só `guest == null`, `status == "waiting"`, e escreve-se `guest = auth.uid`; `v` tem de ser 1.
- `players/$role`: só o dono do `uid` (host ou guest conforme `meta`) escreve; `name` string ≤ 12; `cost` inteiro 0..1000; `ready` booleano; `seen` só `now`.
- Leitura de `rooms/$code` só para `meta.host` e `meta.guest`.
- `turns/$n`: `q`/`shooter` só o anfitrião, uma vez; `answer` só o atirador, uma vez, valor -1..2; `shot.zone` 0..14 e `defend.zone` 0..4, só o atirador/defensor respetivo, **escrita única** (não se pode reescrever) e só depois de `answer` existir.
- **Revelação depois do compromisso:** `shot` só é legível pelo defensor se `defend` já existir; `defend` só é legível pelo atirador se `shot` já existir.
- `meta.winner` (com `status = "finished"`): só pelo próprio jogador e só se `now - players/<adversário>/seen > 30000` (desistência) ou `status == "finished"` com resultado coerente; `reason` ∈ {`score`,`forfeit`}.
- `meta.status`: só o anfitrião muda de `waiting` para `playing`, e só se `players/host/ready` e `players/guest/ready` forem ambos true; passa a `finished` apenas junto com `winner`.
- `meta.guest`: preenchido uma só vez por quem entra (ver acima).
- `toss/guestChoice`: só o convidado, uma vez (`heads`|`tails`); `toss/first`: só o anfitrião, uma vez e só depois de `guestChoice` existir (`host`|`guest`).
- Cada jogador só pode escrever `meta.winner` com o seu próprio papel.
- Apagar a sala: só o anfitrião.

- [ ] **Step 1:** Escrever `database.rules.json` com as regras acima.
- [ ] **Step 2:** Escrever `rules.test.js` com `@firebase/rules-unit-testing` (casos "permite"/"recusa" de cada ponto, incluindo: terceiro não lê nem escreve; segundo `shot` recusado; defensor não lê `shot` antes de escrever `defend`; `answer` 7 recusado; zona 99 recusada; nome com 13 letras recusado; `winner` antes dos 30 s recusado e depois permitido).
- [ ] **Step 3:** Pré-requisito: `node --version` (≥ 18). **O Node não está instalado nesta máquina:** pedir ao autor para instalar o Node LTS (nodejs.org) e depois `npm install -g firebase-tools`. Correr `cd firebase/rules-test && npm install && npx firebase emulators:exec --only database --project walk2win-c8f21 "npm test"`. Esperado: todos os testes passam.
- [ ] **Step 4:** Alternativa se o autor não quiser instalar o Node: colar `database.rules.json` na consola (Realtime Database → Regras) e verificar cada caso no **Simulador de regras** da consola, registando o resultado de cada um no `CLAUDE.md`. Dizer ao autor, sem rodeios, que esta via é manual e menos fiável.
- [ ] **Step 5:** Pedir ao autor que **publique as regras na consola** (Realtime Database → Regras → colar o conteúdo → Publicar). Confirmar com ele.
- [ ] **Step 6:** Commit.

---

### Task 4: Repositório da sala (Firebase) e leitura tolerante

**Files:**
- Create (em `multiplayer/`): `RoomSnapshot.java`, `PlayerInfo.java`, `RoomRepository.java`, `FirebaseRoomRepository.java`
- Test: `app/src/test/java/pt/ubi/pdm/quizpenaltis/multiplayer/RoomSnapshotTest.java`

**Interfaces:**
- Produces: `PlayerInfo { String uid, name, player; int cost; boolean ready; long seen; }`.
- `RoomSnapshot.parse(Object firebaseValue)` → `RoomSnapshot` (aceita `Map<String,Object>` genérico; **nunca lança**: campos em falta/inválidos ficam `null`/ignorados). Getters: `status()`, `host()`, `guest()` (`PlayerInfo` ou `null`), `first()` (`Role` ou `null`), `guestChoice()`, `turn(int n)` → `TurnSnapshot` (`q`, `shooter`, `answer`, `shotZone`, `shotNonce`, `defendZone`, `defendNonce`, cada um nulo se não existir ou fora do intervalo), `winner()`, `reason()`, `version()`.
- `RoomRepository` (interface, para o ecrã e o `RemoteOpponent` não dependerem do Firebase): `createRoom(PlayerInfo me, Callback<String> code)`; `joinRoom(String code, PlayerInfo me, Callback<JoinResult> result)` com `enum JoinResult { OK, NOT_FOUND, FULL, WRONG_VERSION, ERROR }`; `setReady(String code, Role me)`; `startMatch(String code)` (anfitrião: `status = "playing"`); `writeToss(String code, Role writer, String value)`; `openTurn(String code, int n, Role shooter, int q)`; `writeAnswer(String code, int turn, int optionIndex)`, `writeShot(String code, int n, int zone, long nonce)`, `writeDefend(String code, int n, int zone, long nonce)`; `claimWin(String code, Role me, String reason)`; `heartbeat(String code, Role me)`; `listen(String code, Listener l)` com `interface Listener { void onRoom(RoomSnapshot s); void onError(String message); }`; `stopListening()`; `leave(String code, Role me)` (anfitrião à espera: apaga a sala); `deleteRoom(String code)`; `long serverNow()` (hora do servidor em ms, `System.currentTimeMillis()` + `.info/serverTimeOffset`). Os callbacks usam `pt.ubi.pdm.quizpenaltis.util.Callback<T>` (criada na Task 5).

- [ ] **Step 1:** Escrever `RoomSnapshotTest`: mapa completo válido → campos certos; `shot.zone = 99` → `shotZone == null`; `answer = 7` → `null`; `players.host.name` de 40 letras → devolvido já passado por `PlayerName.sanitize`; mapa vazio / `null` / tipo errado → `RoomSnapshot` sem lançar, `status()` nulo.
- [ ] **Step 2:** TESTES. Esperado: falha.
- [ ] **Step 3:** Implementar `RoomSnapshot.parse` e `PlayerInfo` (sem `android.*`).
- [ ] **Step 4:** TESTES. Esperado: passam.
- [ ] **Step 5:** Implementar `RoomRepository` e `FirebaseRoomRepository` com `FirebaseConfig.database()`. `createRoom` gera códigos com `RoomCode.generate` e repete se a escrita for recusada por a sala existir (máx. 5 tentativas); o anfitrião à espera regista `onDisconnect().removeValue()`. `joinRoom` lê `meta` e devolve `NOT_FOUND`/`FULL`/`WRONG_VERSION` antes de escrever.
- [ ] **Step 6:** Verificação manual com a consola do Firebase aberta: criar uma sala a partir de uma activity temporária (não commitar), ver `rooms/{CODE}` aparecer e desaparecer quando a app fecha. Esperado: aparece; desaparece.
- [ ] **Step 7:** Commit.

---

### Task 5: `Opponent` e `BotOpponent` (singleplayer intacto)

**Files:**
- Create: `app/src/main/java/pt/ubi/pdm/quizpenaltis/ui/game/opponent/Opponent.java`, `BotOpponent.java`, `app/src/main/java/pt/ubi/pdm/quizpenaltis/util/Callback.java`
- Modify: `ui/game/GameActivity.java`

**Interfaces:**
- Produces: `interface Opponent` com `Player player()` (carta do adversário), `String displayName()`, `void questionFor(int turn, Callback<Question> cb)` (a pergunta do turno, assíncrona porque no amigo vem da rede; no bot chama `cb` logo com `QuestionManager.getRandomQuestion()`), `void onUserAnswered(int turn, int optionIndex)` (no bot: nada), `void opponentAnswers(int turn, Callback<Boolean> correct)` (no bot: sorteio `BOT_CORRECT_PROBABILITY`), `void userShoots(int turn, GoalZone zone, Callback<Boolean> scored)` (no bot: `greenZones.contains(zone)` com as zonas do utilizador sorteadas como hoje), `void userDefends(int turn, DefenseZone chosen, Callback<DefenseOutcome> outcome)` com `DefenseOutcome { DefenseZone opponentShot; boolean defended; }` (no bot: `DefenseZone.random()`), `void close()`. `Callback<T> { void on(T value); }` definida em `app/src/main/java/pt/ubi/pdm/quizpenaltis/util/Callback.java` (única; a Task 4 reutiliza-a).
- Consome: o comportamento atual de `GameActivity.startBotTurn`, `startShootPhase`, `onGoalZoneClick`, `onDefenseZoneClick`.

- [ ] **Step 1:** TESTES base (22 passam) e jogar uma partida no emulador até ao fim, anotando comportamento de referência (pergunta → remate → bot responde → defesa). Isto é a referência de regressão.
- [ ] **Step 2:** Extrair a lógica do bot para `BotOpponent` mantendo **exatamente** as probabilidades, as zonas verdes locais e os tempos; o `GameActivity` passa a usar `Opponent` (instância `BotOpponent` por omissão, escolhida na `onCreate`). Sem mudar layouts nem textos.
- [ ] **Step 3:** Build, TESTES e repetir a partida do Step 1: mesmo comportamento, sem crashes (`adb logcat | grep FATAL` vazio).
- [ ] **Step 4:** Commit.

---

### Task 6: Menu, seleção sem cobrança e sala de espera

**Files:**
- Modify: `res/layout/activity_main.xml` (botão "Jogar com um amigo", `Widget.W2W.Button.Secondary`, ícone `icon_sports`), `MainActivity.java`, `ui/player/PlayerSelectionActivity.java`, `Constants.java` (`EXTRA_MODE`, `EXTRA_ROOM_CODE`, `EXTRA_ROLE`, `MODE_SINGLE`, `MODE_MULTI`), `AndroidManifest.xml`, `res/values/strings.xml`
- Create: `ui/multiplayer/MultiplayerLobbyActivity.java`, `res/layout/activity_multiplayer_lobby.xml`

**Interfaces:**
- `PlayerSelectionActivity`: se `EXTRA_MODE == MODE_MULTI`, **não cobra** ao confirmar e devolve o resultado com `setResult(RESULT_OK, intent com EXTRA_SELECTED_PLAYER e EXTRA_PLAYER_COST)` + `finish()`; usado com `ActivityResultLauncher`. Em modo singleplayer o comportamento atual fica intacto.
- `MultiplayerLobbyActivity`: ecrã com campo do nome (`PlayerName.sanitize`), botões "Criar partida" e "Entrar com código", campo do código (`RoomCode.normalize`), estado "À espera do amigo" com o código grande. Fluxo: escolher jogador (via `PlayerSelectionActivity`) → `AuthManager.signIn` → `createRoom`/`joinRoom` → ao haver os dois jogadores, cada um verifica o saldo (`StepCounter.getTodayPoints() >= cost`) e chama `setReady`; quando ambos `ready` o anfitrião chama `startMatch`; ao ver `status == "playing"` cada telemóvel cobra com `StepCounter.spendPoints()` e abre o `GameActivity` com `EXTRA_MODE=MODE_MULTI`, `EXTRA_ROOM_CODE`, `EXTRA_ROLE` (`Role.key()`), jogador e custo. Se `spendPoints` falhar nessa janela: `claimWin` para o adversário e mensagem (ver "Limitação conhecida").
- Mensagens de erro (strings): código inválido, sala não encontrada, sala cheia, versão diferente, sem ligação.

- [ ] **Step 1:** Strings e layout do menu e do lobby no design system (tokens, `SystemBars.applyPadding`); sem hex nem emojis.
- [ ] **Step 2:** Modo múltiplo em `PlayerSelectionActivity` (sem cobrança) e teste manual: singleplayer continua a cobrar e a abrir o jogo.
- [ ] **Step 3:** Implementar o lobby e o fluxo `createRoom` → espera → `ready` → `playing` → cobrança → `GameActivity`.
- [ ] **Step 4:** Verificação em **dois emuladores** (ou emulador + telemóvel): A cria e vê o código; B escreve o código; ambos veem o nome e a carta do outro; nada é cobrado antes de `playing`; sair da sala de espera não custa pontos. Códigos errados mostram a mensagem certa (Review Focus 1).
- [ ] **Step 5:** TESTES e commit.

---

### Task 7: `RemoteOpponent` e o jogo em modo amigo

**Files:**
- Create: `ui/game/opponent/RemoteOpponent.java`
- Modify: `ui/game/GameActivity.java`, `ui/game/CoinTossPhase.java` (só se necessário para o convidado escolher e o anfitrião lançar), `res/values/strings.xml`

**Interfaces:**
- `RemoteOpponent(Context, RoomRepository, String code, Role me, PlayerInfo opponentInfo, List<Question> questions)` implementa `Opponent`:
  - `questionFor(turn, cb)`: quando `turns/n.q` aparecer (o anfitrião chama `openTurn` com um `q` novo sem repetir) chama `cb` com `questions.get(q)`; nunca bloqueia a thread principal.
  - `onUserAnswered(turn, optionIndex)` → `writeAnswer`.
  - `opponentAnswers(turn, cb)` → espera `answer` do adversário e devolve se é a certa.
  - `userShoots(turn, zone, cb)` → `writeShot` com um `nonce` aleatório (`SecureRandom`), espera `defend` e devolve `ShotResolution.isGoal(zone, defendZone, GreenZones.pick(GreenZones.combine(shotNonce, defendNonce), GameRules.greenZonesForCost(meuCusto)))`.
  - `userDefends(turn, chosen, cb)` → `writeDefend`, espera `shot`, devolve `DefenseOutcome` com a zona do remate (**uma `DefenseZone` = `covering(shotZone)`** para a animação) e `defended = !isGoal(...)` com as zonas do custo do adversário.
- `GameActivity` em modo múltiplo: nomes e cartas vêm de `RoomSnapshot`; a moeda: o convidado escolhe (`writeToss(code, GUEST, "heads"|"tails")`), o anfitrião lança e escreve `first`; `isPlayerTurn = (TurnOrder.shooterOf(turn, first) == me)`; o anfitrião abre cada turno com `openTurn`; todos aplicam `recordPlayerShot/recordBotShot` e `evaluate()` iguais (a morte súbita mantém a alternância por turnos). Fim: `GameOverView` com os dois nomes; o jogador que ganhou chama `claimWin(code, me, "score")` (que também põe `status = "finished"`) e o anfitrião apaga a sala (`deleteRoom`) depois, mesmo que tenha perdido. Os clientes só agem sobre `meta.winner` quando `reason == "forfeit"`; para `"score"` confiam no cálculo local.
- Guardas: `isProcessingClick` (toque duplo, Review Focus 3); ignorar `turns/n` cujo `shooter` não seja `TurnOrder.shooterOf(n, first)` (Review Focus 4).

- [ ] **Step 1:** Implementar `RemoteOpponent` contra `RoomRepository` (interface, não contra o Firebase diretamente).
- [ ] **Step 2:** Ligar o `GameActivity` ao modo múltiplo (moeda, perguntas partilhadas, resultado, marcador, morte súbita, fim).
- [ ] **Step 3:** Verificação em dois emuladores: uma partida completa sem empate; outra com empate até à **morte súbita**; confirmar que os dois marcadores coincidem em todos os turnos e que o defensor não vê o remate antes de escolher.
- [ ] **Step 4:** TESTES, regressão do singleplayer (Task 5 Step 1) e commit.

---

### Task 8: Desistência, sinal de vida e saída

**Files:**
- Modify: `ui/game/GameActivity.java`, `ui/game/opponent/RemoteOpponent.java`, `ui/multiplayer/MultiplayerLobbyActivity.java`, `res/values/strings.xml`

**Interfaces:** consome `ForfeitRule.hasForfeited`, `MultiplayerRules.HEARTBEAT_MS`, `RoomRepository.heartbeat/claimWin/leave`, `RoomRepository.serverNow()`.

- [ ] **Step 1:** Enviar `heartbeat` a cada `HEARTBEAT_MS` enquanto a activity está visível; verificar a cada heartbeat se o adversário desistiu (`ForfeitRule.hasForfeited(serverNow(), opponent.seen)`) e, se sim, `claimWin(…, "forfeit")` e mostrar o fim com mensagem de desistência.
- [ ] **Step 2:** Se o utilizador sai a meio (botão voltar com confirmação), `leave` e fica sem pontos de volta; o adversário vê a desistência aos 30 s. Se `meta.winner` for escrito pelo outro, mostrar o fim de jogo correspondente.
- [ ] **Step 3:** Verificação em dois emuladores: fechar a app de A a meio de uma pergunta → B ganha entre 30 e ~35 s e o resultado é "desistência"; pôr A em segundo plano ~10 s e voltar → a partida continua (Review Focus 5); mensagem clara se a rede cair.
- [ ] **Step 4:** TESTES e commit.

---

### Task 9: Fecho (fim a fim, documentação)

**Files:**
- Modify: `CLAUDE.md` (secção multiplayer: pacote `multiplayer`, `Opponent`, modelo de dados, como publicar e testar regras, `google-services.json` commitado de propósito, limitações), `res/values/strings.xml` + `activity_help.xml` (uma linha na Ajuda sobre jogar com um amigo, sem emojis)

- [ ] **Step 1:** Rever o checklist do spec (secções 2, 4 e 10) item a item, em dois emuladores, e anotar o resultado de cada um. Incluir: um terceiro cliente não consegue ler a sala (regras da Task 3); o singleplayer tem o mesmo aspeto e comportamento de antes.
- [ ] **Step 2:** TESTES completos e build final.
- [ ] **Step 3:** Atualizar `CLAUDE.md` e a Ajuda. Commit.
- [ ] **Step 4:** Mostrar ao autor o resultado e perguntar se faz push.
