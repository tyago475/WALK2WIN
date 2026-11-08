# Walk2Win: modo multiplayer (design)

Data: 2026-11-08. Estado: rascunho para aprovação do autor (Tiago).

## 1. Intenção

O Walk2Win é uma app Android (Java, minSdk 21) onde os passos dados no dia viram pontos, os pontos
compram um jogador, e esse jogador joga um desempate por penáltis com perguntas de quiz contra um bot
(ver `CLAUDE.md`).

O autor quer poder jogar **contra um amigo, cada um no seu telemóvel**: o jogo é o mesmo, mas o
adversário deixa de ser o bot e passa a ser outra pessoa. O singleplayer **mantém-se e não é alterado**:
o menu passa a ter os dois modos.

Porquê: tornar o jogo social e mais interessante de demonstrar, sem mudar as regras que já estão afinadas.

## 2. Objetivos

- Dois jogadores, em telemóveis diferentes, jogam uma partida completa (moeda, 5 penáltis cada, morte
  súbita, fim de jogo) com as mesmas regras e o mesmo aspeto do singleplayer.
- Funciona **pela internet**, por isso serve tanto para dois amigos na mesma rede como em sítios diferentes.
- Entrar numa partida é simples: um cria, recebe um código curto, o outro escreve-o.
- O singleplayer continua exatamente como está (comportamento, ecrãs, testes).
- Segurança razoável para um jogo entre amigos (ver secção 7).

## 3. Não-objetivos

- Mais de 2 jogadores por partida, torneios, rankings globais, chat, lista de amigos.
- Contas de utilizador, emails, palavras-passe, perfis guardados.
- Impedir batota a 100%: isso exigiria um servidor próprio com lógica de jogo (pago). Aceita-se um
  nível "entre amigos" (secção 7).
- Notificações push para convidar amigos; reencontrar uma partida depois de fechada.

## 4. Anti-objetivos (seria um fracasso mesmo que "funcione")

- Estragar ou mudar o singleplayer, ou os testes unitários que já existem.
- Pedir ao utilizador um registo ou dados pessoais.
- Deixar um estranho entrar numa sala que não é dele, ou ler/alterar salas alheias.
- Permitir que quem defende veja para onde o outro rematou **antes** de escolher onde defende.
- Quebrar a regra dos pontos do `CLAUDE.md` (o saldo tem uma única fonte: `StepCounter`).

## 5. Decisões do autor (palavras dele, resumidas)

1. Cada jogador joga no seu telemóvel; o formato é "praticamente o mesmo", com o amigo no lugar do bot.
2. A forma de entrar fica ao critério de quem constrói (a proposta é a da secção 6).
3. Tem de funcionar na mesma rede **e** à distância (logo, pela internet).
4. Os dois modos (singleplayer e multiplayer) existem no jogo; o singleplayer não se apaga.
5. Tecnologia: o autor deixou a escolha a quem constrói, mas pediu atenção à segurança.
6. **Pontos iguais ao singleplayer:** cada um paga o preço do seu jogador (sem reembolso depois de a
   partida começar), e quem ganha não leva nada. O momento exato da cobrança está na secção 6.
7. **Desistência/queda:** se um jogador deixar de responder durante cerca de 30 segundos, o outro ganha
   automaticamente. O que saiu perde os pontos que já gastou (como hoje ao sair a meio).
8. Confirmou o desenho geral descrito na secção 6.

## 6. Como funciona (desenho geral)

**Menu:** o ecrã principal passa a ter "Jogar contra o bot" (o fluxo atual) e "Jogar com um amigo".

**Entrar:**
- *Criar partida:* o jogador escolhe o jogador (carta) e o nome; aparece um **código de 4 letras** e o
  ecrã "À espera do amigo". Ainda **não é cobrado nada**: sair da sala de espera não custa pontos.
- *Entrar com código:* o outro escreve o código, escolhe o jogador e o nome, e confirma.
- **Cobrança (escolha minha):** quando os dois estão dentro, o preço de cada jogador é cobrado a cada um,
  ao mesmo tempo, e a partida começa. Se um dos dois já não tiver pontos suficientes nesse momento
  (`StepCounter.spendPoints()` devolve false), a partida não começa e nenhum paga. Daí em diante não há
  reembolso, como no singleplayer. Os dois podem escolher o mesmo jogador.
- Cada um vê o jogador (carta/foto) e o nome do outro, e o marcador com os dois nomes.

**Jogo:** igual ao atual. Quem remata responde à pergunta (10 s); se acertar, remata; quem não remata
defende, escolhendo a zona onde acha que a bola vai. Tudo o resto (5 penáltis cada, morte súbita, banners,
transições, fim de jogo) mantém-se, com "o adversário" a ser o amigo.

**Quem remata primeiro:** lançamento da moeda (o telemóvel de quem criou a partida lança e decide; o outro
vê o resultado).

**Perguntas:** a pergunta de cada ronda é a mesma nos dois telemóveis, sorteada pelo telemóvel de quem criou a
partida. As perguntas já existem nos dois (vêm da app), por isso só circula a identificação da pergunta.

**Resultado calculado nos dois lados:** cada telemóvel envia só a sua escolha (a opção respondida, a
zona do remate, a zona da defesa). O golo, a defesa e o marcador são calculados por cada telemóvel a partir
dessas escolhas, nunca "eu digo que marquei".

**Consequência importante nas zonas verdes:** hoje as zonas verdes da baliza são sorteadas ao acaso no
telemóvel que remata, na hora do remate (`GameActivity.startShootPhase`). No multiplayer, o sorteio tem de
ser reproduzível pelos dois lados (por exemplo, a partir de uma semente por remate, guardada na sala) e
depender do custo do jogador de quem remata.

**Regra de defesa:** quem defende não pode ver a zona do remate antes de guardar a sua escolha
(escolha "trancada" primeiro, revelação depois).

**Fim da partida:** vence quem tiver mais golos (regras de `PenaltyShootout`); a sala é apagada pouco depois
de acabar. Desistência/queda: ver decisão 7.

## 7. Segurança

Propostas do construtor (escolha minha, para o autor rever):

- **Identificação anónima:** cada telemóvel entra com um id anónimo gerado pelo serviço; não há registo.
- **Salas fechadas:** só os dois jogadores de uma sala a podem ler e escrever; as regras do serviço
  recusam tudo o resto. Entrar exige o código, só funciona se a sala estiver "à espera" e com um lugar livre,
  e cada telemóvel só pode ocupar um lugar.
- **Dados validados pelas regras do serviço:** forma e valores permitidos de cada escrita (uma zona tem de ser
  uma das zonas, uma opção uma das três, etc.), e cada jogador só escreve a sua parte.
- **Revelação depois do compromisso** na defesa (secção 6), imposta pelas regras e não só pela app.
- **Limites realistas:** sem servidor próprio, um jogador que altere a app ainda poderia tentar batota
  (por exemplo, ignorar os 10 s da pergunta). Aceita-se para jogo entre amigos; as regras limitam o resto.
- **Ficheiro de configuração do serviço** (`google-services.json`) não é um segredo, mas a chave deve ser
  restringida ao pacote da app; opcionalmente, ligar a verificação da app (App Check).
- **Dados mínimos:** a sala só tem nomes escolhidos, ids anónimos, escolhas do jogo; é apagada no fim e
  expira sozinha se ficar abandonada.
- **Código da sala:** 4 letras (cerca de 450 mil combinações), sem letras ambíguas; quem construir deve
  ponderar tentativas repetidas de adivinhar códigos.

## 8. Como é construído (escolhas do construtor, para o autor rever)

- **Serviço online: Firebase** (plano gratuito), com **Realtime Database** (ou Firestore, à escolha de quem
  constrói) e **autenticação anónima**. Escolha minha: dispensa servidor próprio e traz regras de segurança.
- **Região dos dados na Europa.**
- **Isto obriga o autor a criar uma conta Google e um projeto Firebase** e a fornecer o ficheiro
  `google-services.json`; a app não consegue ligar-se sem isso. O construtor guia os passos.
- **Mudança de versão mínima do Android (a confirmar pelo autor):** as versões atuais do Firebase exigem
  Android 6 (API 23). O `minSdk` da app sobe de 21 para 23 (deixa de instalar em Android 5.x), salvo se
  o construtor encontrar versões compatíveis com 21.
- **Arquitetura:** o `GameActivity` (cerca de 630 linhas) tem hoje a lógica do bot misturada com a UI. A
  intenção é isolar "o adversário" (bot ou amigo) atrás de uma abstração, para o resto do jogo não saber
  de qual se trata, sem alterar o comportamento do singleplayer. Como fazer isto fica ao construtor.
- **Regras puras testadas:** o que for lógica sem Android (código da sala, sorteio de zonas por semente,
  estados da partida) vive em classes sem Android, com testes unitários (como `PenaltyShootout`).
- **Pontos:** cobrar com `StepCounter.spendPoints()` quando a partida começa (única fonte do saldo).

## 9. Fica ao critério do construtor

Escolha entre Realtime Database e Firestore; estrutura exata dos dados da sala; como representar a
semente das zonas e a identificação da pergunta; abstração do adversário; ecrãs novos (criar/entrar,
à espera) no mesmo design system (`Widget.W2W.*`, tokens, `SystemBars.applyPadding`, textos em
`strings.xml`); como mostrar ligação perdida; ordem de entrega por fases.

## 10. Como se verifica

- Testes unitários das regras novas e dos testes atuais (`./gradlew testDebugUnitTest` numa cópia com
  caminho ASCII, ver `CLAUDE.md`).
- Dois emuladores (ou um emulador e um telemóvel) a jogar uma partida completa, incluindo morte súbita,
  e o caso de um sair a meio.
- Regras de segurança testadas: um terceiro cliente não consegue ler nem escrever numa sala; quem defende
  não lê o remate antes de se comprometer.
