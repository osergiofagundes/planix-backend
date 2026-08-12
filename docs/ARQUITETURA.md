# Arquitetura

## A ideia em uma frase

Tudo que é de um domínio mora no mesmo pacote, e dentro dele o caminho é sempre
`Controller → Service → Access → Repository → Entidade`.

## Por que package-by-feature, e não package-by-layer

A alternativa comum em Spring é agrupar por camada — `controllers/`,
`services/`, `repositories/`, `entities/` — com todos os domínios misturados
dentro de cada uma. Aqui a escolha foi por **feature**, e ela é deliberada:

- **Uma mudança fica num lugar só.** Adicionar um campo em cartão mexe em
  `card/` e mais nada. No arranjo por camada, a mesma mudança espalha o diff por
  quatro pastas distantes.
- **O pacote conta a história do domínio.** Abrir `team/` mostra que existe
  equipe, membro, papel e provisionamento. Abrir `services/` só mostra que
  existem 20 services.
- **Dá para restringir visibilidade.** `auth/Emails` é package-private porque só
  o `auth/` precisa dela. Isso é impossível quando a camada é a fronteira.

O custo é que features relacionadas se importam entre si — e é isso que a tabela
de dependências abaixo torna explícito, em vez de esconder.

## A anatomia de um pacote

```
card/
  CardController.java      HTTP: rota, validação de entrada, status de saída
  CardService.java         a regra de negócio. @Transactional
  CardAccess.java          autorização: "esta pessoa pode ver este cartão?"
  CardRepository.java      Spring Data JPA
  Card.java                entidade (extends BaseEntity)
  Priority.java            enum do domínio
  CardChange.java          histórico de alterações do cartão
  dto/
    CardCreateRequest.java   entrada, com Bean Validation
    CardResponse.java        saída, com @Schema e factory from()
```

Nem todo pacote tem todas as peças. `storage/` não tem controller nem entidade —
é um serviço de infraestrutura. `list/` não tem `*Access` próprio porque a
permissão de uma lista **é** a permissão do quadro dela, então usa `BoardAccess`.

### Quem faz o quê

| Camada | Responsabilidade | Nunca faz |
|---|---|---|
| `*Controller` | mapear rota, validar com `@Valid`, escolher status de sucesso, anotar para o OpenAPI | regra de negócio, acesso a repositório, montar erro |
| `*Service` | a regra, a transação, a orquestração entre repositórios | falar de HTTP, receber `HttpServletRequest`, devolver entidade |
| `*Access` | responder se o usuário atual pode ver ou gerenciar o recurso | alterar dado |
| `*Repository` | consulta | regra de negócio |
| Entidade | o estado e as invariantes do dado | conhecer DTO ou HTTP |
| `dto/` | o contrato público da API | lógica |

## O caminho de um request, de ponta a ponta

`PATCH /api/cards/100/move` — arrastar um cartão para outra lista:

**1. O filtro identifica quem está chamando** — `auth/JwtAuthFilter`

Lê o header `Authorization`, valida a assinatura e coloca o **id do usuário**
como principal no `SecurityContext`. Nada além disso: sem roles, sem carregar o
`User` do banco.

**2. O controller só traduz HTTP** — `card/CardController`

```java
@PatchMapping("/cards/{id}/move")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void move(@PathVariable Long id, @Valid @RequestBody CardMoveRequest req) {
    service.move(id, req.targetListId(), req.position());
}
```

O `@Valid` já barrou corpo inválido — se falhar, o `GlobalExceptionHandler`
devolve 400 sem o controller saber. Nenhuma decisão de negócio aqui.

**3. O service aplica a regra** — `card/CardService.move`

```java
Card card = cardAccess.require(cardId);   // autoriza e carrega, ou 404
...
if (!target.getBoard().getId().equals(card.getList().getBoard().getId())) {
    throw new CrossBoardMoveException("A lista de destino precisa pertencer ao mesmo quadro do cartão");
}
```

A primeira linha de quase todo método de service é um `*Access` — é ali que a
autorização acontece, e ela vem antes de qualquer regra.

**4. A autorização decide** — `card/CardAccess.require`

```java
Card card = cardRepo.findById(cardId).orElseThrow(() -> naoEncontrado(cardId));
if (!boardAccess.isMember(card.getList().getBoard().getId())) {
    throw naoEncontrado(cardId);   // 404, não 403 — ver SEGURANCA.md
}
```

**5. A volta.** O `@Transactional` do service faz o dirty checking gravar as
mudanças de `position` no commit — ninguém chama `save()` para atualizar. Se
uma exceção subiu, o `GlobalExceptionHandler` a converte em `ApiError` com o
status certo (ver [ERROS.md](ERROS.md)).

## Dependências entre pacotes

Medido pelos `import`, no estado atual:

| Pacote | Importa |
|---|---|
| `common` | — |
| `storage` | common |
| `auth` | common, team |
| `list` | board, common |
| `invite` | auth, common, team |
| `profile` | auth, common, storage |
| `config` | auth, common |
| `card` | auth, board, common, label, list |
| `board` | auth, card, common, list, team |
| `team` | auth, board, card, common |
| `label` | board, card, common |
| `checklist` | board, card, common |
| `comment` | auth, board, card, common |
| `link` | board, card, common |
| `attachment` | auth, board, card, common, storage |

**`common` não importa ninguém.** É a única regra estrutural que o projeto
sustenta com rigor, e a mais importante: `common` é folha, então nunca há ciclo
passando por ele. `storage` idem, tirando `common`.

### Os ciclos, e por que eles continuam aqui

Entre os pacotes de domínio existem ciclos: `board↔card`, `board↔list`,
`board↔team`, `card↔label`, `auth↔team`.

Eles não são acidente — são o agregado do Trello sendo navegado nos dois
sentidos. `Card` precisa chegar no `Board` para saber quem pode vê-lo;
`BoardService` precisa contar e apagar os cartões quando o quadro morre. O
`auth↔team` existe porque cadastrar um usuário cria a primeira equipe dele
(`TeamProvisioning.createFirstTeamFor`).

Quebrar isso exigiria inverter as dependências com interfaces em `common/` ou
publicar eventos de domínio. Para um projeto deste tamanho, isso troca um
acoplamento que o compilador enxerga por uma indireção que ninguém enxerga —
pior de ler e pior de depurar. **A decisão é conviver com os ciclos e mantê-los
declarados aqui.**

Se um dia forem quebrados, o candidato é a exclusão em cascata: hoje ela é
navegação explícita entre pacotes, e viraria um `@DomainEvent` por agregado.

## Decisões e limites conhecidos

Coisas que parecem inconsistência à primeira vista e são deliberadas:

**`Address` mora em `auth/`, mas seus DTOs em `profile/dto/`.** `Address` é
`@Embeddable` dentro de `User`, que é do `auth/`. Movê-lo para `profile/` faria
`auth/` — o pacote mais fundamental — depender de `profile/`, invertendo uma
dependência saudável. A divisão certa é a que está: `auth/` é dono do **dado**,
`profile/` é dono da **API** dele.

**Alguns controllers usam `@RequestMapping("/api")`, outros uma base
específica.** A regra é: use a base específica quando o controller serve uma só
raiz de recurso (`/api/boards`, `/api/teams`, `/api/me`); use `/api` quando ele
cruza mais de uma. `CardController` serve `/api/lists/{listId}/cards` **e**
`/api/cards/{id}` — não há prefixo comum para extrair.

**`checklist/`, `comment/`, `link/` e `attachment/` são pacotes de topo, embora
sejam sub-domínios do cartão.** Cada um tem controller, service e repositório
próprios e endpoints independentes — são features, não detalhes. Aninhá-los sob
`card/` esconderia isso sem eliminar dependência nenhuma.

**Não há ports-and-adapters.** As entidades JPA são o modelo de domínio, e os
services falam direto com o Spring Data. É acoplamento a framework assumido: o
ganho de uma camada de domínio isolada aparece quando há múltiplos adaptadores
de persistência ou regra de negócio densa o bastante para testar sem Spring.
Aqui, os `*IT` com Testcontainers cobrem isso com muito menos cerimônia.

**Nada impede um controller de injetar um repositório** além da disciplina de
quem escreve. Hoje nenhum faz (é verificável com
`grep -rn "Repository" --include=*Controller.java src/main`). Se o projeto
crescer, o passo natural é um teste **ArchUnit** que quebre o build ao detectar
a violação — hoje seria uma dependência a mais para resolver um problema que não
existe.
