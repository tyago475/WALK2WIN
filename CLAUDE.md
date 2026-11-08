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
Em curso: ecrã a ecrã (ver plano nas mensagens/commits). Feitos: etapa 0 (fundações), principal, seleção de jogador, cabeçalho+quiz, moeda/remate/defesa, transições e fim de jogo, Progresso e Ajuda, ícone e splash. Falta: sons, limpeza (os sons ficam para o fim).
- Tema: `Theme.Walk2Win` (Material 3 escuro, mas com `AppCompatViewInflater`: `<Button>` continua simples; usar `MaterialButton` só de propósito).
- Cores: tokens em `colors.xml` (`pitch_900..600` fundos, `grass_*`, `gold_300/500/700`, `text_primary/secondary/on_gold`, `feedback_*`). **Não escrever hex nos layouts.**
- Medidas: `dimens.xml` (`space_xs..xxl` = 4/8/12/16/24/32dp; `radius_sm/md/lg`; 6 tamanhos de texto `text_display..caption`).
- Fontes (OFL, em `res/font`): Barlow Condensed (títulos/botões) e Barlow (corpo). Usar os estilos `TextAppearance.W2W.Display/Title/Heading/Body/Label/Caption`.
- Componentes: `Widget.W2W.Button.Primary` (dourado) / `.Secondary`; `Widget.W2W.Card` / `.Card.Highlight`; `Widget.W2W.Chip`; fundo de ecrã `Widget.W2W.Screen` (`ds_screen_background`).
- Ícones: vetores Material `icon_*.xml` (brancos; colorir com `app:tint`/`android:tint`). Sem emojis como ícones.
- Android 15+ desenha por baixo das barras do sistema: aplicar `SystemBars.applyPadding(root)` nos ecrãs redesenhados.
- Remate/defesa: baliza em vetor (`ds_goal`), zonas `ds_zone_*` aplicadas como *background* de `View` (não `ImageView`, para preencherem a célula); a bola voa com `Animations.ballFlight` e o resultado vem num banner que substitui a instrução.
- Transições e morte súbita: `GameActivity.showOverlay` põe `layout_phase_transition` dentro de `mainContainer` (o cabeçalho com a pontuação fica visível); medalhas `ds_badge_*` (verde = bom para ti). Fim de jogo: `GameOverView` (medalha `ds_coin` dourada se ganhaste).
- Ícone: adaptativo em vetor (`ic_launcher_*`, com camada monocromática); `mipmap-anydpi/w2w.xml` cobre Android 5-7. Splash: atributos `windowSplashScreen*` no tema (Android 12+), sem biblioteca.
- Imagens: WebP (jogadores, baliza). Evitar PNG grandes; sem fotos de marcas/troféus.
- TEMPORÁRIO: `cardViewStyle` no tema força CardViews brancos para os ecrãs antigos. Remover quando todos estiverem redesenhados.

## Convenções
- Sem números mágicos: `Constants` (passos/pontos) e `GameRules` (jogo, tempos).
- Textos em `strings.xml`, cores em `colors.xml`. Placeholders de layout com `tools:text`.
- Regras do jogo em classes sem Android; as Activities só coordenam UI.
- Novos jogadores: uma linha em `PlayerCatalog`. Novas perguntas: `assets/questions.json` (só serve em instalações novas; a BD só é preenchida se estiver vazia).

## Pendente / conhecido
- (adicionar aqui a lista de bugs conhecidos do utilizador)
- `StepTrackingService` é um serviço normal: o Android pode matá-lo em segundo plano. Tornar foreground (tipo `health`) e testar com sensor real.
- `StepsNotificationService` chama sempre `startForeground()` (tipo `health` no Android 14+). Quem usa `startForegroundService()` TEM de garantir isto, senão: `ForegroundServiceDidNotStartInTimeException` ~30 s depois. As atualizações da notificação não arrancam o serviço (`StepsNotificationManager.updateStepsNotification`).
- Room: `exportSchema=false` + `fallbackToDestructiveMigration()` apaga dados ao mudar a versão da BD. Passar a migrações.
- Tema do manifest é `Theme.AppCompat.Light.NoActionBar` direto; `themes.xml`/`values-night` não têm efeito.
- Cores fixas (~170) ainda nos layouts/drawables; ~16 botões/drawables quase duplicados.
- `res/raw/*.mp3` não são usados.
- Logs com emojis em `MainActivity`, `StepCounter`, etc.
- `GameActivity.onOptionClick` e `QuestionManager.getRandomQuestion()` esperam pela BD na thread principal (até 3 s de timeout).
