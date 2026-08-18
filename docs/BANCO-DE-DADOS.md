# Banco de dados

PostgreSQL 18, schema versionado com Flyway, acesso por Spring Data JPA.

## O Flyway é o dono do schema

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

O Hibernate **nunca** cria nem altera tabela. Ele só confere, no boot, se as
entidades batem com o que está lá — e **recusa subir** se não baterem. É de
propósito: o erro aparece no start, não no primeiro request que tocar a coluna
que ninguém criou.

Consequência prática: **toda mudança de entidade precisa de uma migration no
mesmo commit.**

### Escrevendo uma migration

`src/main/resources/db/migration/V{n}__descricao_curta.sql`, com `n` sendo o
próximo número livre. Duas barras baixas entre o número e a descrição.

**Migration já aplicada nunca é editada.** O Flyway guarda o checksum de cada
uma; mudar um arquivo aplicado faz o boot falhar com "migration checksum
mismatch". Corrija sempre com uma migration nova — inclusive em dev, onde é
tentador editar e recriar o banco.

Padrões que o projeto segue:

```sql
id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
board_id   BIGINT      NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
created_at TIMESTAMPTZ NOT NULL,
updated_at TIMESTAMPTZ NOT NULL
```

- `IDENTITY`, casando com `GenerationType.IDENTITY` da `BaseEntity`;
- `TIMESTAMPTZ` para data — nunca `TIMESTAMP` sem fuso, porque o Java grava
  `OffsetDateTime` em UTC;
- `created_at`/`updated_at` em toda tabela de entidade (tabela de junção pura,
  como `card_labels`, não tem);
- **`ON DELETE CASCADE` para composição** (a lista morre com o quadro),
  **`ON DELETE SET NULL` para referência** (apagar o autor não apaga o
  histórico — ver `card_changes.changed_by`);
- índice em toda FK usada para buscar, e índice composto `(pai_id, position)`
  onde há ordenação.

### O histórico até aqui

| Migration | O que trouxe |
|---|---|
| `V1__init_schema` | o núcleo do Trello: `boards`, `board_lists`, `cards`, `labels`, `card_labels`, `checklist_items`, `comments`, `card_links`, `attachments`, `card_changes` |
| `V2__auth` | `users`, `refresh_tokens`, e a autoria retroativa: `owner_id` em quadro, `user_id` em comentário e anexo, `changed_by` no histórico |
| `V3__board_members` | `board_members` e `board_invites` — convite ainda por quadro |
| `V4__card_assignees` | responsáveis pelo cartão |
| `V5__user_profile` | perfil no próprio `users` (endereço vira `@Embeddable`, mas em SQL são colunas soltas) + `user_social_links` |
| `V6__board_icon` | `boards.icon` |
| `V7__teams` | `teams` e `team_members` — a camada de equipe |
| `V8__board_team_and_visibility` | liga quadro a equipe (`team_id`) e cria `visibility` com check constraint. Entra com default `RESTRICTED` para não abrir os quadros existentes, e **depois** troca o default para `TEAM`, que é a regra de quadro novo |
| `V9__team_invites` | `team_invites` e `DROP TABLE board_invites` — o convite passou a ser para a equipe, não para o quadro |

A V8 é o modelo de como fazer migração destrutiva com cuidado: coluna nasce
`NULL`, é preenchida, só então vira `NOT NULL`.

## `BaseEntity` e auditoria

Toda entidade estende:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate  @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

Quem preenche as datas é o `AuditingEntityListener`, ligado por
`JpaAuditingConfig` — e o relógio é o `AuditingDateTimeProvider`, que devolve
**UTC explícito**:

```java
return Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
```

Sem esse provider o Spring usaria o fuso da JVM, e a mesma aplicação gravaria
horários diferentes conforme a máquina. Não escreva `createdAt` na mão.

## `open-in-view=false`

```properties
spring.jpa.open-in-view=false
```

A sessão do Hibernate fecha ao sair do service. Serializar a resposta acontece
**fora** dela, então tocar uma associação `LAZY` na hora de montar o JSON
estoura `LazyInitializationException`.

É o motivo de os DTOs terem factory estático: `CardResponse.from(card)` roda
dentro do `@Transactional`, onde as associações ainda carregam.

```java
List<LabelResponse> labels = card.getLabels().stream()   // carrega aqui, dentro da transação
        .map(LabelResponse::from)
        .sorted(Comparator.comparing(LabelResponse::name))
        .toList();
```

Se der `LazyInitializationException`, a causa quase sempre é conversão para DTO
acontecendo tarde demais — não é caso de ligar o `open-in-view`.

## Ordenação por `position`

Lista, cartão e item de checklist são ordenados por uma coluna `position`
inteira, começando em 0 e **sem buracos**. Quem mantém isso é o service.

Ao mover, o item é tirado da lista, reinserido no índice pedido e todos são
renumerados:

```java
private void insertAt(Card card, Long listId, int newPosition) {
    List<Card> siblings = siblingsOf(listId, card.getId());
    int target = Math.max(0, Math.min(newPosition, siblings.size()));  // clamp
    siblings.add(target, card);
    for (int i = 0; i < siblings.size(); i++) {
        siblings.get(i).setPosition(i);
    }
}
```

Ao apagar ou mover para fora, `reindex` fecha o buraco. Repare no `cardRepo.flush()`
antes do reindex em `move` e `delete`: sem ele o Hibernate ainda não gravou a
mudança, e a releitura dos irmãos traria o estado velho.

Posição fora do intervalo **não é erro** — é limitada ao extremo mais próximo.
Arrastar para além do fim da lista faz o que o usuário quis.

## Os dois bancos

| Ambiente | Porta | Volume | Quando sobe |
|---|---|---|---|
| Produção | `localhost:5433` | `planix_planix_pgdata` | no boot (`restart: always`) |
| Desenvolvimento | `localhost:5434` | `planix-dev_planix_pgdata` | sob demanda (`.\scripts\dev-up.ps1`) |

O default de `application.properties` é a **5433 — o banco de produção**. Ao
rodar a aplicação fora do Docker, aponte para a 5434:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5434/planix"
```

Os dados sobrevivem a `docker compose down`. O que os apaga é `down -v`,
`docker volume rm` e o "Clean / Purge data" do Docker Desktop. **Nunca use `-v`
no stack de produção sem rodar `.\scripts\backup.ps1` antes** — ele dumpa o
banco e empacota os `uploads/` em `backups/`, e as instruções de restauração
estão comentadas no fim do próprio script.

Nos testes o banco é descartável: cada execução sobe um `postgres:18` em
Testcontainers e roda as migrations do zero. Ver [TESTES.md](TESTES.md).

## Seed de desenvolvimento

O banco de dev nasce vazio, e banco vazio não exercita nada: não tem lista
longa, cartão vencido, thread de comentário nem quadro restrito. O seed enche
esse banco com um mundo inteiro — 12 pessoas, 4 equipes, 11 quadros, ~220
cartões, ~350 comentários, checklists, etiquetas, anexos e histórico.

```
src/main/resources/db/
  migration/          schema — produção, dev e Testcontainers
  seed/
    R__seed_dev.sql   dados — só sob o profile dev
```

**O seed não é uma migration de schema.** Ele mora em `db/seed/`, pasta que só
entra no `spring.flyway.locations` quando o profile `dev` está ativo — veja
`application-dev.properties`. Produção não ativa profile nenhum (confira o
`compose.yaml`) e os testes de integração também não, então nenhum dos dois
enxerga o arquivo. A regra de "migration aplicada nunca é editada" continua
valendo para tudo que está em `migration/`; o seed é `R__` justamente porque
foi feito para ser reescrito.

### Como rodar

```powershell
.\scripts\seed-uploads.ps1   # gera os arquivos de anexo e avatar (uma vez)
.\scripts\dev-up.ps1 -d      # o Flyway aplica o seed no boot
```

O `compose.dev.yaml` define `SPRING_PROFILES_ACTIVE=dev`, então **subir o stack
de dev já popula o banco**. O log da aplicação termina com o resumo:

```
DB: SEED DEV aplicado. Senha de todos os usuarios: senha123
DB:   usuarios=12 redes=20 equipes=4 membros_equipe=17 convites=4
DB:   quadros=11 membros_quadro=34 listas=42 etiquetas=73
...
```

Rodando a aplicação fora do Docker, ative o profile na mão:

```powershell
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

O `application-dev.properties` aponta o datasource para a **5434** de propósito:
o default do `application.properties` é a 5433 — produção —, e o seed começa com
`TRUNCATE`. Esta é a salvaguarda que impede o acidente óbvio; ela não protege
quem sobrescrever a URL na mão.

### Como recarregar

Sendo `R__`, o Flyway só reaplica quando o **checksum do arquivo muda**. Editou
o seed? Basta reiniciar. Quer o mesmo seed de novo, do zero?

```powershell
.\scripts\seed-reset.ps1
```

Ele apaga a linha do `R__seed_dev.sql` no `flyway_schema_history` e reinicia a
aplicação — o Flyway então vê um repeatable inédito e roda outra vez.

### Credenciais

Todos os 12 usuários usam a senha **`senha123`**. O hash sai do próprio
Postgres via `pgcrypto` (`crypt(…, gen_salt('bf', 10))` produz o mesmo `$2a$10$`
do `BCryptPasswordEncoder`), então não há constante mágica no arquivo.

| E-mail | Papel no mundo do seed |
|---|---|
| `sergio@gmail.com` | OWNER do Núcleo de Produto, ADMIN do Estúdio Aurora, MEMBER de Ops & Infra — vê quase tudo |
| `ana.souza@planix.dev` | OWNER do Estúdio Aurora |
| `bruno.lima@planix.dev` | ADMIN do Núcleo de Produto, dono de "Bugs e Incidentes" |
| `carla.mendes@planix.dev` | ADMIN do Estúdio Aurora, dona do "Design System" |
| `diego.rocha@planix.dev` | OWNER de Ops & Infra |
| `elisa.prado@planix.dev`, `felipe.antunes@planix.dev`, `gabriela.dias@planix.dev`, `henrique.matos@planix.dev` | MEMBER, com combinações diferentes de quadro |
| `isabela.nunes@planix.dev` | **perfil vazio** — sem bio, telefone ou endereço |
| `joao.pereira@planix.dev` | **só tem a própria equipe** — o caso de quem acabou de se cadastrar |
| `lara.figueiredo@planix.dev` | MEMBER do Estúdio Aurora |

Os convites cobrem os quatro estados, e o token viaja em claro na URL — o banco
guarda só o SHA-256. Os tokens do seed:

| Token | Equipe | Estado |
|---|---|---|
| `convite-nucleo-de-produto` | Núcleo de Produto | **ativo** (3 de 25 usos) |
| `convite-estudio-aurora` | Estúdio Aurora | esgotado (5 de 5) |
| `convite-ops-expirado` | Ops & Infra | expirado |
| `convite-ops-revogado` | Ops & Infra | revogado |

Para exercitar o fluxo de aceite: entre como `joao.pereira@planix.dev` e use o
token ativo.

### Casos de borda plantados de propósito

- **"Ideias Soltas"** — quadro sem nenhuma lista.
- **"Design System" → "Implementação"** — lista sem nenhum cartão.
- **"Roadmap 2026" → "Backlog"** — 22 cartões, para scroll e paginação.
- Três quadros **`RESTRICTED`**, invisíveis para quem não está em `board_members`.
- Cartões vencidos, vencendo hoje, futuros e sem prazo; ~30% concluídos.
- Comentários apagados (`deleted_at`), respostas e reações.

### Duas invariantes que o seed respeita

O seed insere direto no banco, sem passar pelos services — então ele precisa
manter na mão o que a API mantém sozinha:

1. **`position` começa em 0 e não tem buraco.** Toda inserção em massa tira a
   posição de `row_number() OVER (PARTITION BY pai ORDER BY …) - 1`, nunca do
   índice bruto da série.
2. **Coerência de escopo.** Responsável de cartão é sempre membro do quadro,
   etiqueta é sempre do quadro do cartão, resposta fica sempre no cartão da
   raiz e nunca passa de dois níveis.

Não há `random()` em lugar nenhum: a variação vem de aritmética sobre o índice,
então duas execuções geram os mesmos dados. A única exceção é o salt do bcrypt,
aleatório por definição — o hash muda a cada rodada, a senha não.
