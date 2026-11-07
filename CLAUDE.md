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
