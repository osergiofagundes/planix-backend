# Segurança

Duas perguntas diferentes, respondidas em lugares diferentes:

- **Quem é você?** → autenticação, no `auth/` e no `SecurityConfig`. Vale para
  a requisição inteira.
- **Você pode isto?** → autorização, nos `*Access`. Vale por recurso, e é
  decidida dentro do service.

Spring Security só responde a primeira. **A segunda é código nosso** — não há
`@PreAuthorize` no projeto, e o `SecurityFilterChain` não sabe nada sobre
quadros.

## Autenticação

### O fluxo

```
POST /api/auth/register    cria usuário + primeira equipe   → accessToken + refreshToken
POST /api/auth/login       confere a senha                  → accessToken + refreshToken
POST /api/auth/refresh     troca o refresh por um par novo  → accessToken + refreshToken
POST /api/auth/logout      revoga o refresh apresentado
```

As três primeiras são `permitAll` no `SecurityConfig` — junto com `/v3/api-docs`
e `/scalar`. **Todo o resto exige token.**

### O access token

JWT assinado em HMAC-SHA (`JwtService`), TTL de 15 min
(`planix.jwt.access-ttl`). O `subject` é o **id do usuário**; o e-mail vai junto
como claim informativa.

```java
return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("email", user.getEmail())
        .expiration(Date.from(now.plus(accessTtl)))
        .signWith(key)
        .compact();
```

O `JwtAuthFilter` roda antes do `UsernamePasswordAuthenticationFilter`, valida
a assinatura e põe o **`Long` do id** como principal — sem authorities:

```java
var auth = UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of());
```

Isso é o que faz `CurrentUser` funcionar em qualquer lugar:

```java
public Long id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
        throw new IllegalStateException("Nenhum usuário autenticado no contexto");
    }
    return userId;
}
public User reference() { return userRepo.getReferenceById(id()); }   // proxy, sem SELECT
```

> Token inválido **não** vira 401 no filtro: ele simplesmente não autentica, e a
> requisição segue sem principal. Quem devolve 401 é o `AuthenticationEntryPoint`
> ao barrar a rota protegida. Efeito prático: token expirado e token ausente são
> indistinguíveis para o cliente — proposital.

### O refresh token

Não é JWT. É um valor aleatório de 32 bytes (`Tokens.random()`), TTL de 7 dias,
e **no banco só fica o SHA-256 dele** (`Tokens.sha256`) — vazar a tabela não
dá acesso a ninguém.

Ele **rotaciona**: cada `refresh` revoga o token apresentado e emite um par novo.
E há detecção de reuso — apresentar um token já revogado derruba todas as
sessões do usuário:

```java
if (stored.isRevoked()) {
    refreshRepo.revokeAllOf(stored.getUser().getId(), OffsetDateTime.now());
    throw new InvalidRefreshTokenException("Refresh token inválido ou expirado");
}
```

Por isso o método é `@Transactional(noRollbackFor = InvalidRefreshTokenException.class)`:
sem isso, o rollback desfaria a revogação em massa junto com a exceção.

Trocar e-mail ou senha (`AccountService`) também rotaciona tudo, via
`rotateSessions`.

### Senha

BCrypt (`PasswordEncoder` no `SecurityConfig`). O login compara contra um
`dummyHash` quando o e-mail não existe:

```java
String hash = found.map(User::getPasswordHash).orElse(dummyHash);
boolean senhaConfere = encoder.matches(req.password(), hash);
```

Não é frescura: sem isso, "e-mail inexistente" responderia bem mais rápido que
"senha errada", e o tempo de resposta viraria um oráculo de quais e-mails têm
conta.

## Autorização

### A regra dos 404

**Recurso que o usuário não pode ver responde 404, não 403.** Um 403 confirma
que o recurso existe, e isso já é vazamento — dá para enumerar ids de quadro e
descobrir quais estão em uso.

Na prática, "não existe" e "existe mas não é seu" levantam a mesma exceção com a
mesma mensagem:

```java
public void requireMember(Long boardId) {
    if (!isMember(boardId)) {
        throw new NotFoundException("Quadro %d não encontrado".formatted(boardId));
    }
}
```

**403 fica para quando o usuário já enxerga o recurso, mas não pode aquela
operação** — membro comum tentando renomear o quadro, por exemplo. Aí a
existência não é segredo, e o 403 é a resposta honesta.

### Os três `*Access`

| Classe | Pergunta que responde |
|---|---|
| `TeamAccess` | é membro da equipe? é admin? é o dono? |
| `BoardAccess` | enxerga o quadro? pode gerenciá-lo? |
| `CardAccess` | enxerga o cartão? (delega ao `BoardAccess` do quadro dele) |

Cada um expõe dois tipos de método: `isMember(...)` devolve `boolean` para quem
precisa decidir; `requireMember(...)` / `requireAdmin(...)` lança a exceção certa
para quem precisa barrar. `CardAccess.require(id)` faz as duas coisas — autoriza
e devolve a entidade carregada.

Quem **não** tem `*Access` próprio herda o de cima: lista, etiqueta, comentário,
checklist, link e anexo perguntam ao `BoardAccess` ou ao `CardAccess`.

### Quem enxerga um quadro

A regra vive em `BoardRepository.hasAccess` — uma query só, para não fazer três
consultas por request. Você enxerga o quadro se **qualquer uma** valer:

1. você é o **dono** dele;
2. você foi adicionado como **membro do quadro** (`board_members`);
3. você é da **equipe** e o quadro é `TEAM` (visível para a equipe toda);
4. você é **OWNER ou ADMIN da equipe** — admin enxerga até quadro `RESTRICTED`.

`canManage` é mais estreito: só o dono do quadro, ou OWNER/ADMIN da equipe.

Os dois enums que governam isso:

- **`BoardVisibility`** — `TEAM` (padrão) ou `RESTRICTED`.
- **`TeamRole`** — `OWNER`, `ADMIN`, `MEMBER`; `isAdmin()` cobre os dois
  primeiros.

## CORS

Configurado em `SecurityConfig.corsConfigurationSource`, valendo só para
`/api/**`, com as origens vindo de `planix.cors.allowed-origins`
(`http://localhost:5173,http://localhost:5174`).

**Em produção isso não é exercitado.** O nginx do frontend faz proxy de `/api`,
então o navegador conversa com a API na mesma origem e não há preflight. A
configuração existe para o modo dev, em que o Vite roda em porta própria.

`Location` está em `exposedHeaders` — sem isso o JavaScript não leria o header
que os `201` devolvem.

## Ao mexer aqui

- Endpoint novo **já nasce protegido** (`anyRequest().authenticated()`). Só
  mexa na lista de `permitAll` se a rota tiver mesmo que ser pública.
- Regra de permissão nova entra num `*Access`, nunca inline no service e nunca
  no controller.
- Recurso invisível → 404. Operação proibida em recurso visível → 403.
- Cubra o caminho negado no `*IT`. Todo `*FlowIT` do projeto tem um teste de
  "usuário de fora leva 404" — o seu também precisa ter.
