# Erros

## A regra

Service lança **exceção**; `GlobalExceptionHandler` a traduz em status HTTP e
corpo. Controller nunca escolhe status de erro, nunca monta `ResponseEntity` de
erro, nunca captura exceção de domínio.

```
service  ──lança──▶  common/exception/*  ──@RestControllerAdvice──▶  ApiError + status
```

O ganho: a mesma regra devolve o mesmo status em qualquer endpoint que a
dispare, e adicionar um `try/catch` num controller é um erro visível.

## O corpo: `ApiError`

Todo erro da API — sem exceção — tem este formato
(`common/dto/ApiError.java`):

```json
{
  "timestamp": "2026-08-01T14:32:10.123-03:00",
  "status": 404,
  "error": "Not Found",
  "message": "Quadro 42 não encontrado",
  "path": "/api/boards/42",
  "fieldErrors": null
}
```

`fieldErrors` só é preenchido nos erros de validação (400) — um par
`campo → motivo` por campo recusado. Nos demais é `null`.

## Catálogo: exceção → status

Lido de `common/exception/GlobalExceptionHandler.java`. É a lista completa.

| Status | O que dispara |
|---|---|
| **400** | `MethodArgumentNotValidException` (o `@Valid` falhou) → preenche `fieldErrors` |
| | `InvalidFieldException` → `fieldErrors` com um campo só |
| | `MethodArgumentTypeMismatchException` (id não numérico, enum inválido) — se for enum, a mensagem lista os valores aceitos |
| **401** | `InvalidCredentialsException`, `InvalidRefreshTokenException` |
| **403** | `ForbiddenException` |
| **404** | `NotFoundException` |
| **409** | `BoardNotEmptyException`, `TeamNotEmptyException` — apagar contêiner com conteúdo dentro |
| | `NotBoardMemberException`, `NotTeamMemberException` — atribuir alguém que não é membro |
| | `BoardOpenToTeamException` — gerenciar membros de um quadro `TEAM`, que não tem lista de membros |
| | `EmailAlreadyUsedException`, `LabelNameAlreadyUsedException` |
| | `CrossBoardMoveException` — mover cartão para lista de outro quadro |
| | `DataIntegrityViolationException` — rede de proteção: `unique` do banco vira 409, não 500 |
| **413** | `MaxUploadSizeExceededException` (limite: 10 MB) |
| **415** | `UnsupportedFileTypeException` |
| **500** | `StorageException` — falha de disco ao salvar ou apagar arquivo |

Sobre o 409 de `DataIntegrityViolationException`: ele existe porque uma corrida
entre dois requests pode furar a checagem prévia do service e bater na constraint
do banco. Sem esse handler o cliente veria 500 num caso que é conflito legítimo.
Existe teste para isso — `violacaoDeUniqueNoBanco_retorna409EmVezDe500`.

## Os erros que não passam pelo handler

`@RestControllerAdvice` só alcança o que chega ao Spring MVC. Erro de
autenticação acontece **antes**, no filtro — por isso o `SecurityConfig` monta
o `ApiError` na mão:

```java
mapper.writeValue(response.getOutputStream(), new ApiError(
        OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
        message, request.getRequestURI(), null));
```

São dois casos:

| Origem | Status | Mensagem |
|---|---|---|
| `AuthenticationEntryPoint` — sem token, token expirado ou inválido | 401 | "Autenticação necessária" |
| `AccessDeniedHandler` — autenticado, mas barrado pelo filter chain | 403 | "Acesso negado" |

O formato é idêntico ao do handler, de propósito: **o cliente nunca precisa
saber de onde o erro veio.** O `ApiErrorPathsIT` cobre exatamente isso.

## Criar uma exceção nova

Só crie se o par (status, significado) ainda não existir. Antes de escrever uma
`CardAlreadyCompletedException`, veja se `InvalidFieldException` ou uma das
409 já existentes não diz a mesma coisa.

**1.** A classe, em `common/exception/`. São todas iguais — mensagem e nada mais:

```java
package com.sergio.planix.common.exception;

public class CrossBoardMoveException extends RuntimeException {
    public CrossBoardMoveException(String message) { super(message); }
}
```

Nome em inglês, `extends RuntimeException` (nunca checked — atravessaria a
assinatura de todo service à toa).

**2.** O handler, em `GlobalExceptionHandler`:

```java
@ExceptionHandler(CrossBoardMoveException.class)
public ResponseEntity<ApiError> handleCrossBoardMove(CrossBoardMoveException ex,
                                                     HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
}
```

Sempre via `build(...)` — é ele que garante o formato do `ApiError`.

**3.** A mensagem, em português e **específica**, incluindo o dado que causou o
problema:

```java
throw new NotFoundException("Quadro %d não encontrado".formatted(id));
throw new LabelNameAlreadyUsedException(
        "O quadro já tem uma etiqueta chamada \"%s\"".formatted(req.name()));
```

Nunca "Erro ao processar" nem "Operação inválida". A mensagem vai direto para a
tela do usuário — o frontend a exibe como veio.

**4.** O `@ApiResponse` no endpoint que pode lançá-la, para o Scalar mostrar o
status. Não declare 401 e 403: o `OpenApiConfig` acrescenta os dois em toda
operação protegida, e anexa o schema do `ApiError` a qualquer resposta 4xx/5xx
automaticamente.

**5.** O teste. Um `*ControllerTest` provando o status, e um `*IT` se a regra só
aparece com o banco de verdade.
