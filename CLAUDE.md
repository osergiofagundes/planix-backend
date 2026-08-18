# CLAUDE.md

Instruções para trabalhar neste repositório. Leia inteiro antes de mexer no
código — é curto de propósito. O detalhe está em [`docs/`](docs/README.md).

## O projeto

Planix é um gerenciador de tarefas no estilo Trello: equipes → quadros → listas
→ cartões. Este repositório é a **API REST**; o frontend React que a consome
vive em `../planix-frontend-2`.

**Stack:** Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security) ·
PostgreSQL 18 · Flyway · Lombok · JWT (jjwt 0.13) · springdoc + Scalar · Maven ·
JUnit 5 + Testcontainers.

## Comandos

```bash
./mvnw test            # unitários e de camada web (Surefire) — não precisa de Docker
./mvnw clean verify    # + testes de integração (Failsafe + Testcontainers) — EXIGE Docker
./mvnw spring-boot:run # sobe na máquina; veja o README para apontar o datasource
```

```powershell
.\scripts\dev-up.ps1 -d       # stack de dev: API em 8081, banco em 5434
.\scripts\dev-down.ps1        # derruba só o dev; a produção segue no ar
.\scripts\seed-uploads.ps1    # arquivos de anexo/avatar que o seed referencia
.\scripts\seed-reset.ps1      # repopula o banco de dev (apaga tudo antes)
```

O stack de dev sobe com o profile `dev`, que liga o seed de
`db/seed/R__seed_dev.sql` — 12 usuários (senha `senha123`), 4 equipes, 11
quadros e ~220 cartões. Produção e testes não enxergam essa pasta. Detalhes em
[`docs/BANCO-DE-DADOS.md`](docs/BANCO-DE-DADOS.md#seed-de-desenvolvimento).

**Antes de dar qualquer coisa por pronta:** `./mvnw clean verify` precisa passar.
A linha de base é **38 testes no Surefire e 135 no Failsafe, 0 falhas**. São os
`*IT` que exercitam os endpoints de ponta a ponta, então `./mvnw test` sozinho
não prova nada sobre HTTP.

**Se você moveu classe de pacote, rode `clean`.** Build incremental deixa o
`.class` antigo em `target/`, o component scan acha as duas versões e o
contexto do Spring quebra com `ConflictingBeanDefinitionException` — erro que
parece de código e não é.

Para Docker, backup e os dois ambientes (produção na 8080, dev na 8081), veja o
[README](README.md).

## Mapa de `src/main/java/com/sergio/planix/`

```
auth/        usuário, login, JWT, refresh token, conta (e-mail e senha)
board/       quadro + seus membros (BoardMember*) + visibilidade
list/        as colunas do quadro, ordenadas por position
card/        cartão, responsáveis e histórico de alterações (CardChange)
checklist/   itens marcáveis de um cartão
comment/     comentários de um cartão
link/        links externos de um cartão
attachment/  anexos de um cartão
label/       etiquetas do quadro
team/        equipe, membros, papéis e provisionamento da primeira equipe
invite/      convites por link para entrar numa equipe
profile/     perfil público do usuário, avatar e redes sociais
storage/     gravação de arquivo em disco (usado por anexo e avatar)
common/      BaseEntity, Tokens, dto/ (ApiError, MoveRequest), exception/
config/      Security, OpenAPI, Storage, JpaAuditing
```

## A anatomia de um pacote de feature

O projeto é **package-by-feature**: tudo que é de um domínio mora junto, e cada
pacote repete a mesma estrutura.

```
card/
  CardController.java    HTTP: rota, validação, status. Não tem regra de negócio.
  CardService.java       a regra. @Transactional. Devolve DTO, nunca entidade.
  CardAccess.java        "esta pessoa pode ver este cartão?" — autorização
  CardRepository.java    Spring Data JPA
  Card.java              entidade, extends BaseEntity
  dto/                   records de entrada (*Request) e saída (*Response)
```

O fluxo é sempre o mesmo:

```
Controller ──▶ Service ──▶ Access ──▶ Repository ──▶ Entidade
     │              │
     └── dto/ ◀─────┘
```

## Regras invioláveis

1. **Controller nunca injeta `Repository`.** Sempre passa pelo Service. Hoje
   nenhum controller viola isso — mantenha assim.
2. **Entidade JPA não cruza a fronteira HTTP.** Controller devolve `record` de
   `dto/`, montado por um factory estático `from(...)` / `of(...)`. Ver
   `card/dto/CardResponse.java`.
3. **Autorização é dos `*Access`** (`BoardAccess`, `CardAccess`, `TeamAccess`),
   nunca inline no service. E recurso que o usuário não pode ver responde
   **404, não 403** — 403 confirmaria que o recurso existe.
4. **Erro de API nasce como exceção de `common/exception/`**, traduzida pelo
   `GlobalExceptionHandler`. Controller não monta `ResponseEntity` de erro nem
   escolhe status HTTP na mão.
5. **O schema é do Flyway.** `ddl-auto=validate` — mudou entidade, escreva uma
   migration `V{n}__descricao.sql`. **Migration já aplicada nunca é editada**;
   corrija com uma nova.
6. **Todo endpoint carrega `@Operation` + `@ApiResponses`.** O Scalar em
   `/scalar` é a documentação viva da API, e ela só é boa se o anotado for.
7. **Mensagem de erro e comentário em português.** Identificadores em inglês
   (`BoardNotEmptyException`, `findByListIdOrderByPositionAsc`). Nomes de teste
   em português, no formato `cenario_resultadoEsperado`.
8. **Comentário explica *por quê*, não *o quê*.** O código já diz o que faz. Se
   não há nada não-óbvio a explicar, não comente.

## Onde olhar

| Você vai… | Leia |
|---|---|
| entender as camadas e por que os pacotes são assim | [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) |
| **implementar uma feature nova** | [`docs/NOVA-FEATURE.md`](docs/NOVA-FEATURE.md) |
| nomear classe, DTO, teste ou rota | [`docs/CONVENCOES.md`](docs/CONVENCOES.md) |
| mexer em login, token, CORS ou permissão | [`docs/SEGURANCA.md`](docs/SEGURANCA.md) |
| escolher ou criar um status de erro | [`docs/ERROS.md`](docs/ERROS.md) |
| mexer em entidade, migration ou auditoria | [`docs/BANCO-DE-DADOS.md`](docs/BANCO-DE-DADOS.md) |
| escrever ou consertar um teste | [`docs/TESTES.md`](docs/TESTES.md) |

## Ao terminar

- Camadas respeitadas: controller → service → access → repository.
- Nenhuma entidade vazando em resposta; nenhum status HTTP escolhido no controller.
- Mudou entidade? Tem migration nova, e o `validate` do Hibernate passou.
- Endpoint novo anotado para o Scalar.
- `./mvnw clean verify` limpo.
