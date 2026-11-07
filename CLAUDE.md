# Walk2Win / QuizPenaltis

App Android (Java, minSdk 21): conta passos, converte em pontos, e os pontos compram um jogador para um jogo de penáltis com quiz.

## Build
O `java` do PATH é o 8; usar o JDK do Android Studio:
```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug --offline
./gradlew testDebugUnitTest --offline
```

## Convenções
- Constantes de jogo/passos em `Constants`/`GameConfig`; não espalhar números mágicos.
- Textos em `strings.xml`, cores em `colors.xml`.
- Regras do jogo em classes sem Android (testáveis), UI nas Activities.

## Bugs / pontos conhecidos
- (adicionar aqui a lista de bugs conhecidos)
- Passos do sensor ficam em SharedPreferences (`StepCounter`); o gráfico lê da Room.
