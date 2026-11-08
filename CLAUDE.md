# Walk2Win / QuizPenaltis

App Android (Java, minSdk 21): conta passos, converte em pontos, e os pontos compram um jogador para um jogo de penáltis com quiz.

## Build e testes
O `java` do PATH é o 8; usar o JDK do Android Studio:
```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
**Atenção:** o caminho do projeto tem `º` (`3ºano`), e o runner de testes não encontra as classes (ClassNotFoundException).
Para correr os testes, copiar o projeto para uma pasta com caminho ASCII e correr lá.

## Regras de segurança do Firebase
Fonte de verdade: `firebase/database.rules.json` (Realtime Database; os comentários no topo explicam as decisões e o que as regras não conseguem impor). Testes em `firebase/rules-test/rules.test.js`, no emulador (projeto `demo-…`, sem login). O Node não está no PATH e o emulador precisa de Java 21+:
```
export PATH="/c/Program Files/nodejs:$PATH"
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"
cd firebase/rules-test && npm install
npx firebase emulators:exec --only database --project demo-walk2win --config ../firebase.json "npm test"
```
(Aqui o caminho com `º` não dá problemas.) **Depois de qualquer mudança às regras: correr os testes e voltar a publicá-las na consola** (Realtime Database → Regras → colar → Publicar); a consola não lê o ficheiro.
O cliente não pode ler a sala inteira nem `turns/n` de uma vez: escuta cada caminho (`meta`, `players`, `toss`, `turns/n/shooter|q|answer|shot|defend`). O defensor só pode ler `shot` depois de escrever `defend` (e vice-versa).

## Estrutura (`pt.ubi.pdm.quizpenaltis`)
- `ui.main|game|player|progress|help` – Activities e views de cada ecrã
- `game` – regras puras sem Android (`PenaltyShootout`, `GameRules`, `GoalZone`, `DefenseZone`) → testadas em `src/test`
- `model` – `Player`, `PlayerCatalog` (fonte única dos jogadores), entidades Room
- `data.db|questions|progress` – Room, perguntas (`assets/questions.json`), histórico de passos
- `steps` – contagem de passos, serviços e notificação
- `util.Constants` – meta de passos, passos por ponto, chaves de Intent

## Pontos (regras)
- Pontos ganhos = passos de hoje ÷ 10. **Saldo = ganhos − gastos.** Só existe "hoje": à meia-noite passos e gastos voltam a 0 (pontos por gastar perdem-se; o ecrã principal avisa).
- Escolher um jogador custa o preço dele (`PlayerCatalog`). É cobrado ao carregar em CONFIRMAR e **não há reembolso** (sair a meio perde os pontos). Ganhar não dá prémio.
- Única fonte do saldo: `StepCounter.getTodayPoints()`; gastar com `StepCounter.spendPoints()` (verifica e desconta de uma vez, devolve false se não chegar). A conta pura está em `PointsBalance` (testada).
- Os ecrãs nunca copiam o saldo entre si: `PlayerSelectionActivity` relê-o ao abrir e no `onResume`.
- Mínimo para jogar = custo do jogador mais barato (`PlayerCatalog.cheapestCost()`).
- Para testar no emulador: TESTAR (+1000 passos = +100 pontos); "meia-noite" simula-se pondo `last_reset_date` de ontem em `shared_prefs/<pkg>_preferences.xml`.

## Design system (estilo Ultimate Team, verde relvado + dourado, só modo escuro)
Em curso: ecrã a ecrã (ver plano nas mensagens/commits). Feitos: etapa 0 (fundações), principal, seleção de jogador, cabeçalho+quiz, moeda/remate/defesa, transições e fim de jogo, Progresso e Ajuda, ícone e splash. Limpeza feita (drawables/cores antigos, emojis). Falta: sons (ficam para o fim).
- Tema: `Theme.Walk2Win` (Material 3 escuro, mas com `AppCompatViewInflater`: `<Button>` continua simples; usar `MaterialButton` só de propósito).
- Cores: tokens em `colors.xml` (`pitch_900..600` fundos, `grass_*`, `gold_300/500/700`, `text_primary/secondary/on_gold`, `feedback_*`). **Não escrever hex nos layouts.**
- Medidas: `dimens.xml` (`space_xs..xxl` = 4/8/12/16/24/32dp; `radius_sm/md/lg`; 6 tamanhos de texto `text_display..caption`).
- Fontes (OFL, em `res/font`): Barlow Condensed (títulos/botões) e Barlow (corpo). Usar os estilos `TextAppearance.W2W.Display/Title/Heading/Body/Label/Caption`.
- Componentes: `Widget.W2W.Button.Primary` (dourado) / `.Secondary`; `Widget.W2W.Card` / `.Card.Highlight`; `Widget.W2W.Chip`; fundo de ecrã `Widget.W2W.Screen` (`ds_screen_background`).
- Ícones: vetores Material `icon_*.xml` (brancos; colorir com `app:tint`/`android:tint`). Sem emojis como ícones.
- Android 15+ desenha por baixo das barras do sistema: aplicar `SystemBars.applyPadding(root)` nos ecrãs redesenhados.
- Remate/defesa: baliza em vetor (`ds_goal`), zonas `ds_zone_*` aplicadas como *background* de `View` (não `ImageView`, para preencherem a célula); a bola voa com `Animations.ballFlight` e o resultado vem num banner que substitui a instrução.
- Transições e morte súbita: `GameActivity.showOverlay` põe `layout_phase_transition` dentro de `mainContainer` (o cabeçalho com a pontuação fica visível); medalhas `ds_badge_*` (verde = bom para ti). Fim de jogo: `GameOverView` (medalha `ds_coin` dourada se ganhaste).
- Ícone: adaptativo em vetor (`ic_launcher_*`, com camada monocromática); `mipmap-anydpi/w2w.xml` cobre Android 5-7. Splash: `core-splashscreen` com o tema `Theme.Walk2Win.Splash` (só na MainActivity, que chama `installSplashScreen()` antes do `super.onCreate`).
- Imagens: WebP (jogadores, baliza). Evitar PNG grandes; sem fotos de marcas/troféus.

## Convenções
- Sem números mágicos: `Constants` (passos/pontos) e `GameRules` (jogo, tempos).
- Textos em `strings.xml`, cores em `colors.xml`. Placeholders de layout com `tools:text`.
- Regras do jogo em classes sem Android; as Activities só coordenam UI.
- Novos jogadores: uma linha em `PlayerCatalog`. Novas perguntas: `assets/questions.json` (só serve em instalações novas; a BD só é preenchida se estiver vazia).

## Pendente / conhecido
- (adicionar aqui a lista de bugs conhecidos do utilizador)
- `StepTrackingService` e `StepsNotificationService` são foreground (tipo `health`, mesma notificação, id 100) e partilham `StepsNotification.enterForeground()`. Quem os arranca TEM de usar `startForegroundService()`. Falta testar com sensor real (o emulador não tem passos).
- Room: esquema exportado em `app/schemas` (versão 3 = base). Só as versões 1-2 são destrutivas; **qualquer mudança de versão exige um `Migration`** (senão a app crasha em vez de apagar dados).
- `res/raw/*.mp3` não são usados.
- `QuestionManager` pré-carrega a pergunta seguinte; `getRandomQuestion()` só bloqueia (até 3 s) se ainda não houver nenhuma pronta.

## Firebase (multiplayer)
- `app/google-services.json` está commitado de propósito (não é segredo; a chave de API deve ser restringida ao pacote na Google Cloud Console). minSdk = 23.
