# Implementando uma feature nova

A receita, na ordem em que se escreve. O exemplo é o pacote `label/` — pequeno e
completo, com tudo que uma feature precisa ter.

**A ordem importa.** Ela vai do banco para fora, e cada passo compila antes do
próximo existir. Começar pelo controller é o caminho de escrever um endpoint
que não tem onde guardar o dado.

## 0. Antes de escrever nada

Decida a qual **pacote** a coisa pertence. As opções são três:

- é um domínio novo, com endpoints próprios → pacote novo (`label/`);
- é um campo ou operação de um domínio existente → o pacote dele;
- é infraestrutura sem domínio (disco, hash, data) → `storage/` ou `common/`.

Se for pacote novo, ele nasce com a estrutura de
[ARQUITETURA.md](ARQUITETURA.md#a-anatomia-de-um-pacote).

## 1. A migration

`src/main/resources/db/migration/V{n}__descricao.sql`, com `n` sendo o próximo
número livre. O Flyway é o dono do schema — o Hibernate roda com
`ddl-auto=validate` e vai **recusar subir** se a entidade não bater com a tabela.

```sql
CREATE TABLE labels (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id   BIGINT       NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(30)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_label_board_name UNIQUE (board_id, name)
);
CREATE INDEX idx_labels_board ON labels (board_id);
```

`created_at` e `updated_at` **sempre**, porque toda entidade estende
`BaseEntity`. Índice em toda FK usada para buscar. E ponha no banco a regra que
o service também checa — a `UNIQUE` acima é a rede que transforma uma corrida
entre dois requests em 409 em vez de 500. Detalhes e as regras de cascata em
[BANCO-DE-DADOS.md](BANCO-DE-DADOS.md).

> Migration já aplicada nunca é editada. Corrija com uma nova.

## 2. A entidade

```java
@Entity
@Table(name = "labels")
@Getter
@Setter
public class Label extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id")
    private Board board;

    @Column(nullable = false)
    private String name;

    protected Label() {}                                  // exigido pelo JPA

    public Label(Board board, String name, String color) { // o construtor real
        this.board = board;
        this.name = name;
        this.color = color;
    }
}
```

Três coisas não são negociáveis: `extends BaseEntity` (id + auditoria),
`FetchType.LAZY` em todo `@ManyToOne`, e o par construtor `protected` vazio +
construtor público com os campos obrigatórios.

## 3. O repositório

```java
public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findByBoardIdOrderByNameAsc(Long boardId);
    boolean existsByBoardIdAndName(Long boardId, String name);
}
```

Query derivada do nome sempre que der. `@Query` só quando o nome do método
ficaria ilegível — é o caso de `BoardRepository.hasAccess`.

## 4. A autorização

Se o recurso novo tem dono próprio, ele ganha um `*Access`. Se a permissão dele
é herdada de outro agregado — como etiqueta, cuja permissão é a do quadro —
**reuse o `*Access` existente** em vez de criar um novo:

```java
private Label findOrThrow(Long id) {
    Label label = labelRepo.findById(id).orElseThrow(() -> naoEncontrada(id));
    if (!boardAccess.isMember(label.getBoard().getId())) {
        throw naoEncontrada(id);        // 404, nunca 403 — ver SEGURANCA.md
    }
    return label;
}
```

Repare que "não existe" e "existe mas não é seu" devolvem **a mesma exceção com
a mesma mensagem**. Isso é proposital.

## 5. O service

```java
@Service
@Transactional
public class LabelService {

    // injeção por construtor, sem @Autowired

    @Transactional(readOnly = true)
    public List<LabelResponse> listByBoard(Long boardId) {
        boardAccess.requireMember(boardId);
        return labelRepo.findByBoardIdOrderByNameAsc(boardId).stream()
                .map(LabelResponse::from).toList();
    }

    public LabelResponse create(Long boardId, LabelRequest req) {
        boardAccess.requireMember(boardId);
        if (labelRepo.existsByBoardIdAndName(boardId, req.name())) {
            throw new LabelNameAlreadyUsedException(
                    "O quadro já tem uma etiqueta chamada \"%s\"".formatted(req.name()));
        }
        ...
    }

    public LabelResponse update(Long id, LabelRequest req) {
        Label label = findOrThrow(id);
        label.setName(req.name());     // sem save(): dirty checking grava no commit
        return LabelResponse.from(label);
    }
}
```

O checklist do service:

- `@Transactional` na classe, `@Transactional(readOnly = true)` nas leituras;
- **primeira linha de cada método público é a autorização**;
- devolve DTO, nunca entidade;
- update não chama `save()` — a entidade está gerenciada, o commit grava;
- conflito de regra vira exceção de `common/exception/`, não `ResponseEntity`.

## 6. Os DTOs

Dois records em `dto/`. O de entrada valida, o de saída converte:

```java
public record LabelRequest(
        @Schema(description = "Nome da etiqueta. Único dentro do quadro.", example = "Urgente")
        @NotBlank @Size(max = 100) String name,
        ...
) {}

public record LabelResponse(
        @Schema(description = "Id da etiqueta", example = "5") Long id,
        ...
) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getBoard().getId(), ...);
    }
}
```

O `@Size(max = 100)` tem que bater com o `VARCHAR(100)` da migration — senão o
banco recusa o que a validação deixou passar, e um 400 vira 500.

## 7. O controller

```java
@RestController
@RequestMapping("/api")
@Tag(name = "Etiquetas")
public class LabelController {

    @Operation(summary = "Criar uma etiqueta no quadro",
               description = "A etiqueta pertence ao **quadro**: crie uma vez, aplique em vários cartões.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Etiqueta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
            @ApiResponse(responseCode = "409", description = "Já existe etiqueta com esse nome")
    })
    @PostMapping("/boards/{boardId}/labels")
    public ResponseEntity<LabelResponse> create(@PathVariable Long boardId,
                                                @Valid @RequestBody LabelRequest req) {
        LabelResponse created = service.create(boardId, req);
        return ResponseEntity.created(URI.create("/api/labels/" + created.id())).body(created);
    }
}
```

- `201` devolve `Location`; `204` usa `@ResponseStatus(HttpStatus.NO_CONTENT)` e
  retorno `void`;
- `@Operation` + `@ApiResponses` em **todo** endpoint — 401 e 403 o
  `OpenApiConfig` acrescenta sozinho, não os declare;
- `@Tag` com um nome já registrado em `OpenApiConfig.tags()`; se for novo,
  registre-o lá com uma descrição.

## 8. Os testes

Dois níveis, e a feature precisa dos dois. Detalhes em [TESTES.md](TESTES.md).

- **`LabelControllerTest`** (`@WebMvcTest`, roda em `./mvnw test`): a camada
  HTTP com o service mockado. Cobre validação, formato do `ApiError` e o status
  de cada exceção.
- **`LabelFlowIT`** (`@SpringBootTest` + Testcontainers, roda em
  `./mvnw clean verify`): o fluxo real contra Postgres. Cobre o caminho feliz e,
  obrigatoriamente, **o acesso negado** — um usuário que não é do quadro tem que
  levar 404.

## 9. Antes de abrir o PR

```bash
./mvnw clean verify
```

E confira em `http://localhost:8081/scalar` que os endpoints novos aparecem com
descrição legível — se o Scalar ficou feio, a anotação está incompleta.
