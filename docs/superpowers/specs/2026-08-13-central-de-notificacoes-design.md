Central de Notificações — planejamento

▎ Este arquivo é o entregável. Na aprovação ele vai para
▎ planix/docs/superpowers/specs/2026-08-13-central-de-notificacoes-design.md
▎ e é commitado. Nenhum código é escrito nesta etapa.

---
Contexto

Hoje, no Planix, o usuário só descobre o que aconteceu entrando em cada
quadro: um card que moveu, um prazo que venceu, um comentário novo. Não há
lugar nenhum que responda "o que mudou desde ontem?".

A Central de Notificações é um inbox por usuário — um sino no topo do app com
contador de não-lidas e um painel com o histórico. É a primeira feature do
projeto que não é uma tela sobre uma tabela: ela é derivada de coisas que
acontecem em outros domínios.

E há um segundo destino no horizonte: chat em tempo real. Isso muda o
recorte, e é a razão do desenho abaixo.

Decisões tomadas:

- Um serviço novo, planix-realtime, em Java 21 + Spring Boot 4, com
Kafka, Cassandra e WebSocket.
- Um serviço para os dois domínios (notificação agora, chat depois), não um
para cada.
- Fan-out no produtor: o planix resolve os destinatários e publica o evento
pronto.
- Sem biblioteca compartilhada entre os dois serviços.
- MVP notifica só o que o Planix já sabe hoje.

---
1. As três decisões de topologia, e por quê

1.1 Por que um serviço realtime, e não dois

Se notificações e chat forem serviços separados, o navegador precisa manter
duas conexões WebSocket abertas — ou você constrói um terceiro componente só
para multiplexar. Os dois domínios compartilham exatamente as mesmas peças: o
hub de conexões, o ticket de autenticação, o cluster Cassandra, o transporte.
O que os separa é só o modelo de dados, e isso é um pacote, não um serviço.

Um serviço, dois pacotes (notification/ e chat/), uma conexão WebSocket com
um campo type no frame. Partir em dois depois continua possível — os pacotes
já nascem separados e o corte é limpo.

1.2 Por que Java, e não Go

O argumento clássico a favor de Go aqui era goroutines para milhares de conexões
WebSocket. Virtual threads do Java 21 encerraram esse argumento: com
spring.threads.virtual.enabled=true, thread-por-conexão volta a ser viável em
Java na escala que este projeto jamais vai ver.

┌────────────────────────────┬─────────────────────────────────────────────────────────────────────┬──────────────────────┐
│                            │                        Java (Spring Boot 4)                         │          Go          │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ RAM do serviço             │ ~300 MB                                                             │ ~30 MB               │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ Subida                     │ ~4 s                                                                │ ~50 ms               │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ Consumidor Kafka           │ @KafkaListener + @RetryableTopic (retry e dead-letter declarativos) │ escrito à mão        │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ Cassandra                  │ Spring Data Cassandra                                               │ gocql, queries à mão │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ Reuso do que já existe     │ convenções, testes, Testcontainers, Dockerfile, scripts             │ reescrever           │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ Aprendizado de arquitetura │ integral                                                            │ integral             │
├────────────────────────────┼─────────────────────────────────────────────────────────────────────┼──────────────────────┤
│ Aprendizado de linguagem   │ nenhum                                                              │ Go                   │
└────────────────────────────┴─────────────────────────────────────────────────────────────────────┴──────────────────────┘

Com Cassandra (~1,5 GB) e Kafka (~1 GB) dominando o compose, a diferença de
memória entre Go e Java é ~300 MB de ~4 GB. Não é o fator decisivo; o tempo de
subida no loop de desenvolvimento incomoda mais, e é o preço aceito.

O ponto honesto: Java entrega 100% do aprendizado de arquitetura
distribuída — Kafka, Cassandra, outbox, consistência eventual, at-least-once,
WebSocket com estado. Go acrescentaria o aprendizado de linguagem, que é um
objetivo separado e legítimo — mas separado.

1.3 Por que NÃO um planix-common compartilhado

Biblioteca de código compartilhada entre microserviços recria o acoplamento que
a separação existe para evitar: mudou o common, redeploya os dois. A
disciplina correta é compartilhar o contrato, não o código — o contrato é o
JSON do §5, versionado no nome do tópico.

Na prática também: transformar o planix num parent pom multi-módulo moveria
~100 arquivos Java para planix-api/src/…, quebrando Dockerfile, compose.yaml,
scripts PowerShell e a linha de base de testes. Muito trabalho que não tem
relação com notificação.

O que de fato se duplica são ~50 linhas: validação de JWT (jjwt, só verify —
o planix-realtime nunca emite token) e o record ApiError. Se incomodar
depois, o meio-termo é um módulo minúsculo só com o record do evento.

---
2. O que o MVP notifica

Foi levantado na exploração que menção em comentário e convite dirigido não
existem no Planix: Comment guarda só texto livre e TeamInvite é link com
token (tokenHash, maxUses), não é dirigido a ninguém. Notificar essas duas
coisas exige implementá-las antes — no monolito, não no serviço novo. Ficam
fora do MVP.

Dentro do MVP (tudo já existe hoje):

┌────────────────────┬──────────────────────────────────────────────────────┬────────────────────────────────────┐
│        Tipo        │                  Gatilho no Planix                   │            Quem recebe             │
├────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────┤
│ CARD_MOVED         │ CardService.move muda list_id                        │ membros do quadro, menos o ator    │
├────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────┤
│ CARD_ASSIGNED      │ CardAssigneeService adiciona responsável             │ o responsável adicionado           │
├────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────┤
│ CARD_COMMENTED     │ CommentService cria comentário                       │ responsáveis do card, menos o ator │
├────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────┤
│ CARD_DUE_SOON      │ varredura horária: prazo em < 24 h                   │ responsáveis do card               │
├────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────┤
│ CARD_DUE_OVERDUE   │ varredura horária: prazo vencido, card não concluído │ responsáveis do card               │
├────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────┤
│ TEAM_MEMBER_JOINED │ alguém entra na equipe via convite                   │ donos e admins da equipe           │
└────────────────────┴──────────────────────────────────────────────────────┴────────────────────────────────────┘

Fora do MVP, registrado como evolução: menções @fulano, convite dirigido,
preferências por usuário (silenciar quadro / desligar tipo), agrupamento
("Fulano moveu 3 cards"), e-mail/push — e o chat.

---
3. Arquitetura

              ┌──────────────────────────────────────────┐
              │  planix (Spring Boot · Postgres)  :8080   │
              │                                          │
   ação do    │  CardService ──▶ NotificationPublisher    │
   usuário ──▶│       │                  │                │
              │       └── MESMA TRANSAÇÃO ┘                │
              │                  ▼                        │
              │        notification_outbox (Postgres)     │
              │                  │                        │
              │        OutboxRelay  @Scheduled 2s         │
              └──────────────────┼───────────────────────┘
                                 │ produce
                                 ▼
                 ┌───────────────────────────────┐
                 │ Kafka  planix.notifications.v1 │
                 └───────────────┬───────────────┘
                                 │ @KafkaListener
                                 ▼
              ┌──────────────────────────────────────────┐
              │  planix-realtime (Spring Boot)   :8090    │
              │                                          │
              │  notification/  consumer ──▶ Cassandra    │
              │        │                                  │
              │        └──▶ socket/ConnectionHub ──┐      │
              │  chat/  (fase futura, mesmo hub)   │      │
              │                                    │      │
              │  REST: GET /api/notifications      │      │
              └────────────────────────────────────┼─────┘
                                                   ▼
                    ┌──────────────────────────────────┐
                    │ planix-frontend-2 (React)        │
                    │  sino + painel + 1 socket        │
                    └──────────────────────────────────┘

Três repositórios: planix (produtor), planix-realtime (novo),
planix-frontend-2 (consumidor). O nginx do frontend já é o gateway — vai
rotear /api/notifications e /ws/ para o serviço novo (§9).

Autenticação: o planix-realtime valida o mesmo JWT HS256, com o mesmo
PLANIX_JWT_SECRET. O token traz sub = id do usuário
(planix/…/auth/JwtService.java:32). Nenhuma chamada de volta ao Planix. Custo:
o segredo vive em dois lugares — dívida anotada (a saída seria RS256 + JWKS).

---
4. Lado planix (produtor)

4.1 Por que outbox, e não publicar direto no Kafka

Publicar no KafkaTemplate de dentro do service cria dual-write: a
transação do Postgres commita e o publish falha (ou o contrário), e a
notificação some ou nasce de um fato que não aconteceu. A outbox elimina isso —
a linha do evento é gravada na mesma transação da regra de negócio, sem
nenhum I/O externo.

Isso paga um segundo benefício, prático e grande: os testes continuam rodando
sem Kafka. ./mvnw clean verify sobe só o Postgres do Testcontainers e
verifica que a linha foi para a outbox. A linha de base atual (38 Surefire /
135 Failsafe) não ganha dependência nova de infra.

4.2 Pacote novo com.sergio.planix.notification/

Segue a anatomia do projeto (package-by-feature, docs/NOVA-FEATURE.md):

notification/
  NotificationType.java            enum dos 6 tipos
  NotificationEvent.java           record — O CONTRATO (§5)
  NotificationOutbox.java          entidade da tabela outbox
  NotificationOutboxRepository.java
  NotificationPublisher.java       grava na outbox — é o que os services chamam
  RecipientResolver.java           quem recebe cada tipo, já sem o ator
  OutboxRelay.java                 @Scheduled — lê pendentes, publica, marca enviada
  DueDateScanner.java              @Scheduled — prazos próximos e vencidos

- NotificationPublisher é a única porta. Recebe o tipo, o ator, o contexto
denormalizado e os destinatários; grava uma linha na outbox. Nunca faz I/O
de rede — é o que permite chamá-lo de dentro de uma transação.
- RecipientResolver reusa BoardRepository.hasAccess e o
BoardMemberRepository. Não duplicar regra de acesso — se faltar uma query
de "listar membros do quadro", ela nasce no BoardMemberRepository, não aqui.
- OutboxRelay roda a cada 2 s, pega as pendentes por id crescente,
publica com KafkaTemplate, marca sent_at. Kafka fora do ar? A outbox
acumula e drena sozinha. Exige @EnableScheduling — o projeto ainda não
tem (nenhum @Scheduled hoje), então entra em config/.
- DueDateScanner roda de hora em hora. O event_id é determinístico —
UUID.nameUUIDFromBytes("due-soon:{cardId}:{dueDateEpoch}"), que é UUIDv3
(MD5). O JDK não gera v5, e a diferença é irrelevante aqui porque o uso não é
criptográfico. Reexecutar não duplica, e isso evita uma coluna nova em cards
só para marcar "já avisei".
  O insert precisa ser insert … on conflict (event_id) do nothing, num
método com @Modifying @Query(nativeQuery = true). O save() do Spring Data
não ignora a violação de unique: estoura DataIntegrityViolationException e
derruba a transação do scanner inteiro, levando junto os eventos que já tinham
sido montados na mesma passada.

4.3 Migration V11__notification_outbox.sql

create table notification_outbox (
    id          bigserial primary key,
    event_id    uuid        not null unique,
    type        varchar(40) not null,
    payload     jsonb       not null,
    created_at  timestamptz not null default now(),
    sent_at     timestamptz
);
create index idx_outbox_pending on notification_outbox (id) where sent_at is null;

Índice parcial: a fila de pendentes fica barata mesmo com a tabela crescendo.
Uma limpeza das enviadas com mais de 7 dias entra no mesmo @Scheduled.

4.4 Pontos de chamada

Uma linha em cada, no fim do método, dentro da transação existente:

┌───────────────────────────────┬────────────────────────────┬────────────────────┐
│            Arquivo            │           Método           │       Evento       │
├───────────────────────────────┼────────────────────────────┼────────────────────┤
│ card/CardService.java         │ move (quando list_id muda) │ CARD_MOVED         │
├───────────────────────────────┼────────────────────────────┼────────────────────┤
│ card/CardAssigneeService.java │ adicionar responsável      │ CARD_ASSIGNED      │
├───────────────────────────────┼────────────────────────────┼────────────────────┤
│ comment/CommentService.java   │ criar comentário           │ CARD_COMMENTED     │
├───────────────────────────────┼────────────────────────────┼────────────────────┤
│ team/TeamMemberService.java   │ entrada via convite        │ TEAM_MEMBER_JOINED │
└───────────────────────────────┴────────────────────────────┴────────────────────┘

CardService.move já grava um CardChange de list_id
(CardService.java:106) — é exatamente ali. Regra do projeto mantida:
service chama service, controller não sabe que notificação existe.

---
5. O contrato do evento

Tópico planix.notifications.v1 (versão no nome — quebrar contrato vira
.v2, não uma migração dolorosa). Uma mensagem por evento, com a lista de
destinatários já resolvida.

Chave da mensagem: "board:{id}" quando o evento tem quadro, "team:{id}"
quando não tem. Agrupa eventos do mesmo quadro na mesma partição, preservando
a ordem entre eles. O fallback não é detalhe: TEAM_MEMBER_JOINED tem
board: null, e chave nula no Kafka cai em round-robin — os eventos da mesma
equipe se espalhariam pelas partições e perderiam exatamente a ordenação que a
chave existe para dar.

{
  "eventId":    "9f1c...-uuid",
  "type":       "CARD_MOVED",
  "occurredAt": "2026-08-13T14:22:05Z",
  "recipients": [12, 15, 31],
  "actor":      { "id": 7, "name": "Sérgio", "avatarUrl": "/uploads/..." },
  "board":      { "id": 4, "name": "Sprint 12" },
  "card":       { "id": 88, "title": "Ajustar filtro de cards" },
  "team":       null,
  "data":       { "fromList": "Fazendo", "toList": "Revisão" }
}

O record NotificationEvent existe nos dois projetos, duplicado de
propósito (§1.3). É o contrato, e a duplicação é a fronteira.

Três decisões embutidas, cada uma com um motivo:

1. recipients já resolvido. O planix-realtime nunca precisa saber o que
é um membro de quadro. Sem réplica de dados, sem tópico de membros, sem
backfill. O preço: o acoplamento migra para o payload — se a regra de "quem
recebe" mudar, muda no planix, e eventos já gravados no Cassandra não são
recalculados. Aceitável, e é a razão de o desenho ser este.
2. Contexto denormalizado (actor.name, board.name, card.title). O
serviço renderiza a notificação sem nunca chamar o Planix. Consequência
assumida: se o card for renomeado depois, a notificação antiga mostra o
título antigo — o que é correto, porque ela é registro histórico, não view.
3. occurredAt e eventId vêm do produtor, nunca do consumidor. É o que
torna a escrita idempotente (§6.2). Nada de now() no consumidor.

5.1 Como o contrato não quebra em silêncio

A duplicação do record é a fronteira, e é deliberada — mas ela troca um erro
de compilação por um erro de runtime. Renomear um campo no planix não quebra
nada no build do planix-realtime: quebra a desserialização de uma mensagem,
depois, em execução. Dois repositórios não têm compilador em comum, então a
proteção precisa ser escrita à mão.

A rede é um golden fixture — o mesmo notification-event.sample.json
commitado nos dois repositórios:

- no planix, um teste serializa um NotificationEvent montado à mão e compara
com o fixture;
- no planix-realtime, um teste desserializa o mesmo fixture e afirma que todo
campo chegou preenchido.

Qualquer drift quebra o build de um dos dois lados, e quebra na hora. É a única
peça que devolve verificação em tempo de build a uma fronteira que a topologia
tornou dinâmica — e é, ela própria, a lição de contract testing que dois
repositórios cobram.

---
6. planix-realtime — repositório novo

Spring Boot 4 · Java 21 · Spring Kafka · Spring Data Cassandra · Spring WebSocket
· jjwt · springdoc + Scalar · Actuator · Testcontainers. Mesmas convenções do
planix (docs/CONVENCOES.md): package-by-feature, controller → service →
repository, DTO na fronteira HTTP, erro nascendo como exceção, teste em
português.

Atenção ao Actuator: o /actuator/health do §6.3 e o healthcheck do compose
dependem de spring-boot-starter-actuator, que não está no pom.xml do planix.
Não vem por herança de convenção — é dependência nova, declarada aqui.

planix-realtime/
  src/main/java/com/sergio/planix/realtime/
    config/       Security, Cassandra, Kafka, WebSocket, OpenApi
    auth/         JwtService (só verify) · JwtAuthFilter · CurrentUser
                  WsTicketService (ticket de uso único, 30 s)
    notification/ Notification (@Table) · NotificationRepository
                  NotificationService · NotificationController
                  NotificationConsumer (@KafkaListener) · dto/
    socket/       RealtimeWebSocketHandler · ConnectionHub · dto/
    common/       dto/ApiError · exception/GlobalExceptionHandler
    chat/         (fase futura — mesmo hub, mesmo Cassandra)
  src/main/resources/cql/  V1__notifications.cql, …
  Dockerfile
  compose.yaml

spring.threads.virtual.enabled=true — cada conexão WebSocket segura uma
virtual thread, sem custo de plataforma.

6.1 O que o Spring dá de graça aqui

- @KafkaListener(topics = "planix.notifications.v1", groupId = "realtime-writer")
— grupo de consumidores, commit de offset e desserialização configurados.
- @RetryableTopic — retry com backoff e dead-letter topic
declarativos. Um evento que falha três vezes vai para
planix.notifications.v1.DLT em vez de travar a partição. Escrever isso à mão
é dia de trabalho.
- @ServiceConnection nos Testcontainers de Cassandra e Kafka — o Spring
Boot injeta os endereços nos testes sem uma linha de @DynamicPropertySource.

6.2 Modelagem Cassandra

Schema em .cql versionados, aplicados na subida — mesma disciplina do
Flyway: arquivo aplicado nunca se edita, corrige-se com o próximo.

CREATE KEYSPACE planix
  WITH replication = {'class':'SimpleStrategy','replication_factor':1};

CREATE TABLE notifications_by_user (
    user_id      bigint,
    occurred_at  timestamp,
    event_id     uuid,
    type         text,
    actor_id     bigint,  actor_name  text,  actor_avatar text,
    board_id     bigint,  board_name  text,
    card_id      bigint,  card_title  text,
    team_id      bigint,  team_name   text,
    data         map<text, text>,
    read         boolean,
    PRIMARY KEY ((user_id), occurred_at, event_id)
) WITH CLUSTERING ORDER BY (occurred_at DESC, event_id ASC)
  AND default_time_to_live = 7776000;   -- 90 dias

CREATE TABLE unread_by_user (
    user_id      bigint,
    occurred_at  timestamp,
    event_id     uuid,
    PRIMARY KEY ((user_id), occurred_at, event_id)
) WITH CLUSTERING ORDER BY (occurred_at DESC, event_id ASC)
  AND default_time_to_live = 7776000;

Em Spring Data Cassandra isso vira @Table + @PrimaryKeyClass com
@PrimaryKeyColumn(type = CLUSTERED, ordering = DESCENDING).

As quatro decisões que importam:

- Partição por user_id. Toda leitura do inbox é uma partição só. É o
desenho que Cassandra recompensa — e é por isso que Cassandra é a escolha
certa aqui, não um Postgres esquisito.
- Chave primária determinística (occurred_at + event_id, ambos do
evento). Kafka é at-least-once: reprocessar acontece. Como o INSERT do
Cassandra é upsert e a chave não depende do relógio do consumidor,
reprocessar é inofensivo. Se occurred_at viesse de now(), cada
reprocessamento criaria uma linha duplicada — esta é a armadilha do desenho.
- Tabela separada para não-lidas. Cassandra não faz WHERE read = false sem
índice secundário (antipadrão aqui). Contador =
SELECT event_id FROM unread_by_user WHERE user_id = ? LIMIT 100 → tamanho da
lista, exibindo "99+" acima do teto. Marcar como lida = DELETE daqui +
UPDATE read = true na principal. Idempotente e sem counter drift — o que
uma counter column não daria, porque counter não é idempotente sob retry. O
LIMIT 100 é do contador e só dele: o read-all do §6.3 varre a partição
inteira.
- TTL nativo, 90 dias. Retenção sem job de limpeza. Pegadinha: o UPDATE
de read grava a célula com TTL novo e ela sobreviveria ao resto da linha.
Marcar como lida usa USING TTL <restante> = 7776000 - idade. É uma lição
legítima de Cassandra.

Limitação assumida: se o consumidor reprocessar um evento que o usuário já
leu, a linha volta para unread_by_user. A janela é o intervalo entre gravar e
commitar o offset. Mitigação barata quando incomodar: ler read da tabela
principal antes de inserir na de não-lidas.

6.3 API HTTP

Todos exigem Authorization: Bearer <jwt>. O id exposto é o composto
"{occurredAtMillis}-{eventId}" — um único valor que serve de identidade, de
cursor e de coordenada para o UPDATE.

┌────────┬─────────────────────────────────────────────┬──────────────────────────────┐
│ Método │                    Rota                     │           Resposta           │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ GET    │ /api/notifications?limit=20&before=<cursor> │ { items: [...], nextCursor } │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ GET    │ /api/notifications/unread-count             │ { count: 12, capped: false } │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ POST   │ /api/notifications/{id}/read                │ 204                          │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ POST   │ /api/notifications/read-all                 │ 204                          │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ POST   │ /api/notifications/ws-ticket                │ { ticket, expiresIn: 30 }    │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ GET    │ /ws/realtime?ticket=<ticket>                │ upgrade para WebSocket       │
├────────┼─────────────────────────────────────────────┼──────────────────────────────┤
│ GET    │ /actuator/health                            │ 200                          │
└────────┴─────────────────────────────────────────────┴──────────────────────────────┘

read-all não reusa a leitura do contador. O contador para no LIMIT 100 e
exibe "99+"; se o read-all usasse a mesma query para saber o que apagar, quem
tem 300 não-lidas ficaria com 200 depois de clicar em "marcar todas" — e o
botão pareceria quebrado. O read-all pagina a partição até o fim, em lotes.

Formato de erro igual ao do planix (common/dto/ApiError) — o
normalizeApiError do frontend funciona sem caso especial. Todo endpoint com
@Operation + @ApiResponses; Scalar em /scalar, como no planix.
Notificação de outro usuário responde 404, não 403 — regra 3 do CLAUDE.md
vale aqui também.

6.4 WebSocket — o ticket, e por quê

O WebSocket nativo do browser não permite enviar header Authorization.
As saídas ruins são token na query string (vaza em log de proxy e no histórico
do navegador) e abuso do Sec-WebSocket-Protocol. A saída correta:

1. Frontend chama POST /api/notifications/ws-ticket com o Bearer normal.
2. O WsTicketService gera um ticket aleatório e guarda em memória com o
userId, uso único e 30 s de validade.
3. O browser conecta em /ws/realtime?ticket=…. O handler resgata, apaga o
ticket e registra a sessão no ConnectionHub sob aquele userId.

ConnectionHub é um ConcurrentHashMap<Long, Set<WebSocketSession>> — várias
abas do mesmo usuário recebem todas. Ping a cada 30 s para o nginx não matar a
conexão ociosa. Ao consumir um evento, o NotificationConsumer grava no
Cassandra e depois chama hub.push(userId, frame); quem não estiver
conectado vê na próxima leitura do inbox. O WebSocket é otimização de
latência, nunca a fonte da verdade.

O frame já nasce com envelope, para o chat entrar sem quebrar nada:

{ "type": "NOTIFICATION_CREATED", "payload": { … } }

Escalar para N instâncias (fora do MVP, mas o desenho comporta): dois
@KafkaListener — um com groupId fixo (realtime-writer) que grava no
Cassandra uma única vez, e outro com groupId único por instância
(realtime-push-${random}), fazendo toda instância ver todo evento e empurrar
só para suas sessões locais. Sem Redis pub/sub, sem sticky session. É o próximo
exercício natural — e é exatamente o mecanismo que o chat vai precisar.

---
7. Lado frontend (planix-frontend-2)

Respeitando a regra de dependência api/ → services/ → hooks/ → components/:

┌───────────────────────────┬──────────────────────────┬──────────────────────────────────────────────────────────────┐
│          Camada           │         Arquivo          │                            O quê                             │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ types/                    │ notification.types.ts    │ contrato + labels e ícone por tipo                           │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ api/                      │ endpoints.ts (editar)    │ bloco notifications: { … } — nenhuma rota literal fora daqui │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ services/                 │ notification-service.ts  │ chamadas tipadas, sem React                                  │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ lib/                      │ query-client.ts (editar) │ queryKeys.notifications — list, unreadCount                  │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ lib/                      │ realtime-socket.ts       │ conexão, ticket, backoff exponencial, reconexão — sem React  │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ hooks/                    │ use-notifications.ts     │ useInfiniteQuery com cursor + mutations de leitura           │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ hooks/                    │ use-realtime.ts          │ monta o socket, roteia frames por type, injeta no cache      │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ components/notifications/ │ notification-bell.tsx    │ sino + badge de não-lidas                                    │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│                           │ notification-panel.tsx   │ popover com a lista, "marcar todas" e scroll infinito        │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│                           │ notification-item.tsx    │ uma linha, clicável → navega para o card/quadro              │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────┤
│ layout/                   │ page-topbar.tsx (editar) │ onde o sino entra                                            │
└───────────────────────────┴──────────────────────────┴──────────────────────────────────────────────────────────────┘

Detalhes que decidem a qualidade:

- Ao chegar frame pelo socket: queryClient.setQueryData faz prepend na
primeira página e incrementa o contador — sem refetch. Refetch só como
rede de segurança no onReconnect.
- use-realtime monta uma vez no app-layout, não por componente. Desmonta
no logout — o evento planix:auth-logout já existe em lib/token-storage.ts.
- O roteamento por type do frame é o que deixa o chat entrar depois sem tocar
em nada disto.
- Estados de carregando / erro / vazio com Skeleton, ErrorState e
Empty — é regra do CLAUDE.md, e o vazio ("nada por aqui") é o estado mais
visto de uma central de notificações nova.
- Toast de erro sai do hook via toastApiError, nunca do componente.
- VITE_REALTIME_URL só existe em dev (npm run dev fala direto com a 8090).
Em produção é vazio: o nginx roteia por caminho.

---
8. Fallback se o serviço cair

O sino desaparece, não quebra a tela: erro na query de contador não mostra
badge nem toast. O Planix segue funcionando por completo e a outbox acumula os
eventos, que drenam sozinhos quando o Kafka voltar. Nenhum caminho de escrita
do Planix depende do planix-realtime — é a propriedade que justifica a
separação, e a que o chat vai herdar.

---
9. Infraestrutura

planix-realtime/compose.yaml — Kafka em modo KRaft (sem Zookeeper) e
Cassandra 5, na rede planix-net que o planix/compose.yaml já cria:

services:
  kafka:       # apache/kafka, KRaft, single node
  cassandra:   # cassandra:5, MAX_HEAP_SIZE=1G, healthcheck com cqlsh
  realtime:
    environment:
      PLANIX_JWT_SECRET: ${PLANIX_JWT_SECRET:?}   # mesmo segredo do planix
      JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=70
    ports: ["127.0.0.1:8090:8090"]
networks:
  default: { name: planix-net, external: true }

Custo real: Cassandra ~1,5 GB, Kafka ~1 GB, a JVM do realtime ~350 MB. Some
~3 GB ao Docker Desktop. Medido nesta máquina: 31,4 GB de RAM e nenhum
.wslconfig, então o WSL usa o default de 50% — ~15,7 GB. Cabe folgado. Memória
não é um risco deste projeto, e não vale gastar atenção com ela.

Ordem de subida, essa sim obrigatória. O planix/compose.yaml é o dono da
planix-net; o frontend e o realtime a consomem como external. A ordem é sempre
planix → planix-realtime → frontend, e o realtime não sobe sozinho num
ambiente limpo. Se um dia existir um compose.dev.yaml do realtime, ele precisa
de rede própria — pelo mesmo motivo que o do planix precisou: dois containers
com o mesmo alias na planix-net fazem o nginx sortear entre eles.

planix-frontend-2/nginx.conf — dois blocos novos, antes do location /.
/api/notifications é prefixo mais específico que /api/, então o nginx o
escolhe sem ambiguidade:

location /api/notifications {
    set $planix_rt http://realtime:8090;
    proxy_pass $planix_rt$request_uri;
    # mesmos proxy_set_header do bloco /api/
}

location /ws/ {
    set $planix_rt http://realtime:8090;
    proxy_pass $planix_rt$request_uri;
    proxy_http_version 1.1;
    proxy_set_header Upgrade    $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 3600s;      # o default de 60s mataria a conexão ociosa
}

---
10. Fases

Cada fase termina em algo verificável. A ordem foi escolhida para que a Fase 3
já entregue valor na tela, antes de qualquer WebSocket.

┌──────┬──────────────────────────────────────────────────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────────────────────────┐
│ Fase │                                               Entrega                                                │                                       Termina quando                                       │
├──────┼──────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
│ 0    │ Contrato + outbox no planix: pacote notification/, migration V11, publisher, resolver, pontos de     │ ./mvnw clean verify verde e os *IT provam que mover um card grava a linha certa na outbox. │
│      │ chamada, DueDateScanner. Relay só loga.                                                              │  Sem Kafka, sem Docker novo.                                                               │
├──────┼──────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
│ 1    │ Kafka e Cassandra no compose; OutboxRelay publicando de verdade.                                     │ kafka-console-consumer mostra o JSON do §5 ao mover um card.                               │
├──────┼──────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
│ 2    │ planix-realtime esqueleto: pom, security, consumidor, Cassandra, API REST de leitura.                │ curl com um JWT real devolve o inbox e o contador; testes com Testcontainers passam.       │
├──────┼──────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
│ 3    │ Frontend: sino, painel, scroll infinito, marcar como lida. Contador em polling de 30 s.              │ A feature está usável. npm run typecheck e lint limpos.                                    │
├──────┼──────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
│ 4    │ WebSocket: ticket, hub, push, envelope tipado; polling vira fallback de reconexão.                   │ Dois navegadores: um move o card, o outro vê o sino acender na hora.                       │
├──────┼──────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
│ 5    │ (fora do MVP) chat no mesmo serviço; menções; convite dirigido; preferências; N instâncias.          │ —                                                                                          │
└──────┴──────────────────────────────────────────────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────┘

---
11. Armadilhas conhecidas

Coletadas na exploração — cada uma já custou tempo de alguém:

1. occurred_at do consumidor. Se o consumidor usar now() na chave,
reprocessar duplica notificação. Vem do evento, sempre.
2. proxy_read_timeout do nginx. O default de 60 s derruba o WebSocket
ocioso, e o sintoma parece bug de reconexão do frontend.
3. @EnableScheduling não existe hoje no planix. Sem ele, o relay e o
scanner ficam mudos e nada acusa.
4. Migration aplicada nunca se edita (regra 5 do CLAUDE.md). Vale para os
.cql do Cassandra também.
5. Recurso invisível responde 404, não 403 (regra 3) — no planix-realtime
também.
6. Mover classe de pacote exige clean — o .class velho em target/
quebra o contexto do Spring com ConflictingBeanDefinitionException, um erro
que parece de código e não é.
7. Cassandra demora ~60 s para ficar pronta. O depends_on precisa de
condition: service_healthy com healthcheck de cqlsh, ou o serviço morre
na subida.
8. Fundir os dois serviços custaria a suíte de testes do planix. Os 135
Failsafe de hoje sobem só o Postgres do Testcontainers; com Cassandra no
mesmo app, todo @SpringBootTest passaria a esperar um container que leva
~60 s para ficar pronto — encarecendo os testes que já existem, não só os
novos. É o motivo real de separar os processos. O conflito de scan entre
Spring Data JPA e Spring Data Cassandra é secundário, e resolvível com
@EnableJpaRepositories(basePackages=…) e
@EnableCassandraRepositories(basePackages=…) escopados: não sustenta a
decisão sozinho.
9. save() na outbox estoura no unique de event_id. O insert idempotente do
DueDateScanner precisa de on conflict do nothing (§4.2). Sem isso, a
segunda passada derruba a transação inteira, levando junto os eventos que já
tinham sido montados nela.
10. Chave nula no Kafka. TEAM_MEMBER_JOINED não tem quadro; sem o fallback
team:{id} do §5, a mensagem vai para uma partição sorteada e a ordem se
perde — em silêncio.

---
12. Verificação

Por fase, e nesta ordem:

- planix: ./mvnw clean verify — linha de base 38 Surefire / 135
Failsafe, 0 falhas, mais os *IT novos de outbox e o teste de serialização
contra o golden fixture do §5.1.
- planix-realtime: ./mvnw clean verify com Testcontainers de Cassandra e
Kafka via @ServiceConnection. Os testes que importam: o golden fixture do
§5.1 desserializa com todo campo preenchido; reprocessar o mesmo evento não
duplica; marcar como lida some do contador; read-all zera mesmo acima de 100;
o cursor pagina sem repetir nem pular; JWT de outro usuário não lê o inbox
alheio.
- Frontend: npm run typecheck e npm run lint — base 0 erros, 3 avisos
conhecidos de react-hooks/exhaustive-deps. Erro ou aviso novo é regressão.
npm run format no que foi tocado.
- Ponta a ponta, manual: dois usuários do seed de dev (senha senha123),
dois navegadores. Um move um card do quadro compartilhado; o outro vê o
contador subir sem recarregar, abre o painel, clica e cai no card certo.
- Resiliência: docker stop no realtime — o Planix continua respondendo
tudo e o sino simplesmente some. Subir de volta: a outbox drena e as
notificações aparecem.
