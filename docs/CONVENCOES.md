# Convenções

## Nomes de classe

O sufixo diz a camada. Não há exceção no projeto.

| Sufixo | O que é | Exemplo |
|---|---|---|
| `*Controller` | endpoint HTTP | `CardController` |
| `*Service` | regra de negócio, `@Transactional` | `LabelService` |
| `*Access` | autorização de um agregado | `BoardAccess`, `CardAccess`, `TeamAccess` |
| `*Repository` | Spring Data JPA | `CardRepository` |
| `*Request` | DTO de entrada, com Bean Validation | `LabelRequest` |
| `*Response` | DTO de saída, com `@Schema` | `CardResponse` |
| `*Exception` | erro de domínio, em `common/exception/` | `BoardNotEmptyException` |
| `*Config` | `@Configuration`, em `config/` | `SecurityConfig` |

Sem sufixo ficam a **entidade** (`Card`, `Board`, `Team`), o **enum de domínio**
(`Priority`, `TeamRole`, `BoardVisibility`) e os **utilitários estáticos**, que
usam plural: `Tokens`, `Emails`.

Duas classes fogem do padrão de propósito, e o nome explica por quê:
`TeamProvisioning` (cria a primeira equipe de um usuário no cadastro) e
`TeamResponses` (monta `TeamResponse` com as contagens, evitando N+1). Nenhuma
das duas é service — não têm regra própria, são colaboradores de quem tem.

## Idioma

**Identificador em inglês, texto humano em português.**

```java
public class BoardNotEmptyException extends RuntimeException { }   // inglês

throw new NotFoundException("Quadro %d não encontrado".formatted(id));  // português
```

Vale para mensagem de exceção, `@Schema(description = ...)`, `@Operation` e
comentário. O que o usuário ou o desenvolvedor lê é português; o que o
compilador lê é inglês.

Nome de **teste** é a exceção: português, no formato
`cenario_resultadoEsperado`.

```java
void criarSemNome_retorna400ComOCampoNoFieldErrors()
void cartaoQueVoceNaoEnxerga_retorna404ComCorpoApiError()
void violacaoDeUniqueNoBanco_retorna409EmVezDe500()
```

## Entidades

```java
@Entity
@Table(name = "cards")     // plural, snake_case
@Getter
@Setter
public class Card extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id")
    private BoardList list;

    @Enumerated(EnumType.STRING)          // nunca ORDINAL
    @Column(nullable = false)
    private Priority priority = Priority.NONE;

    protected Card() {}

    public Card(BoardList list, String title, int position) { ... }
}
```

- `extends BaseEntity` sempre — id, `createdAt`, `updatedAt` vêm de lá.
- `@ManyToOne` é **sempre** `LAZY`. O `open-in-view=false` significa que uma
  associação não carregada estoura fora da transação; carregue no service.
- `@Enumerated(EnumType.STRING)` sempre. `ORDINAL` amarra o banco à ordem de
  declaração do enum.
- Construtor `protected` vazio para o JPA, construtor público com os campos
  obrigatórios para o código.
- Lombok só `@Getter`/`@Setter`. Nada de `@Data` ou `@EqualsAndHashCode` em
  entidade — `equals` baseado em campo mutável quebra `Set` e cache do
  Hibernate.

## DTOs

Sempre `record`, sempre em `dto/`, sempre anotados para o OpenAPI.

**Entrada** valida com Bean Validation, e os limites batem com a migration:

```java
public record LabelRequest(
        @Schema(description = "Nome da etiqueta. Único dentro do quadro.", example = "Urgente")
        @NotBlank @Size(max = 100) String name
) {}
```

**Saída** converte com um factory estático — `from(entidade)` quando é conversão
direta, `of(...)` quando precisa de mais argumentos:

```java
public static LabelResponse from(Label label) { ... }
public static TeamResponse of(Team team, TeamRole myRole, long membros, long quadros) { ... }
```

Todo `@Schema` de saída leva `example`. É o que faz o Scalar ficar legível.

**DTO compartilhado** vai em `common/dto/`. Hoje há um: `MoveRequest`, usado por
lista e checklist. `CardMoveRequest` é separado porque o cartão também precisa
do `targetListId`.

## Controllers e rotas

Toda rota começa com `/api`. Onde declarar o resto:

- **Base específica** quando o controller serve uma só raiz de recurso:
  `@RequestMapping("/api/boards")`, `("/api/teams")`, `("/api/me")`.
- **`@RequestMapping("/api")`** com o caminho completo no método quando ele
  cruza mais de uma raiz. `CardController` serve
  `/api/lists/{listId}/cards` e `/api/cards/{id}` — não há prefixo comum.

Recurso no plural (`/cards`, `/boards`); ação que não é CRUD vira `PATCH` com
sufixo verbal (`/cards/{id}/move`, `/cards/{id}/complete`).

Status de saída:

| Situação | Resposta |
|---|---|
| criou | `201` + header `Location` via `ResponseEntity.created(...)` |
| alterou e devolve o novo estado | `200` com o `*Response` |
| alterou e não tem o que devolver | `204`, com `@ResponseStatus` e retorno `void` |

## Injeção de dependência

Construtor, sem `@Autowired`, campos `private final`. Nada de injeção por campo.

```java
private final CardRepository cardRepo;
private final CardAccess cardAccess;

public CardService(CardRepository cardRepo, CardAccess cardAccess) { ... }
```

Nome de campo abreviado para repositório (`cardRepo`, `listRepo`, `changeRepo`)
— é o padrão em todo o projeto.

## Organização dentro do arquivo

1. campos `final`
2. construtor
3. métodos públicos, leituras (`@Transactional(readOnly = true)`) antes das escritas
4. métodos privados, no fim

Helper que só monta exceção fica por último e tem nome em português, porque é
texto de usuário disfarçado de método: `naoEncontrada(id)`, `naoEncontrado(id)`.

## Comentários

Explique **por quê**, não **o quê**. Os bons comentários do projeto são todos
assim — o do `pom.xml` sobre Surefire vs Failsafe, o do `application.properties`
sobre por que a 5173 continua na lista de CORS. Se não há nada não-óbvio a
dizer, não comente.
