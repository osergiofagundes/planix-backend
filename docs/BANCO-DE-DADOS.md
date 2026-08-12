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
