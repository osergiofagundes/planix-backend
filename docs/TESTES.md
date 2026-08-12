# Testes

## Dois níveis, dois plugins

O sufixo do nome decide **quem roda o teste e quando**. Está explicado no
`pom.xml`, e é a coisa mais importante a entender aqui:

| | `*Test` | `*IT` |
|---|---|---|
| Plugin | Surefire | Failsafe |
| Comando | `./mvnw test` | `./mvnw clean verify` |
| Precisa de Docker? | não | **sim** |
| Banco | nenhum (ou mock) | Postgres real, em Testcontainers |
| Velocidade | segundos | dezenas de segundos |
| Serve para | validação, status HTTP, regra pura | o fluxo real, permissão, SQL |

Linha de base atual: **34 no Surefire, 125 no Failsafe, 0 falhas.**

Renomear um `LabelFlowIT` para `LabelFlowTest` o tira do Failsafe e o joga no
Surefire, onde ele vai tentar subir Testcontainers no meio da fase de teste
rápida. O sufixo não é decoração.

## `*Test` — a camada web isolada

`@WebMvcTest` sobe só o controller indicado, com o service mockado. É onde se
prova **contrato HTTP**: status, formato do `ApiError`, validação de entrada.

```java
@WebMvcTest(LabelController.class)
@AutoConfigureMockMvc(addFilters = false)   // sem filtro de segurança: aqui não se testa auth
class LabelControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean LabelService service;
    @MockitoBean CurrentUser currentUser;    // mockados porque o contexto os exige,
    @MockitoBean JwtService jwtService;      // não porque o teste os usa

    @Test
    void criarSemNomeNemCor_retorna400ComOsDoisCamposNoFieldErrors() throws Exception {
        mvc.perform(post("/api/boards/1/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"color\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.color").exists());
    }
}
```

O que vale a pena cobrir aqui: cada exceção que o service pode lançar virando o
status certo, e o `@Valid` recusando o que tem que recusar. Faça o mock lançar
a exceção e verifique o corpo:

```java
when(service.create(eq(1L), any(LabelRequest.class)))
        .thenThrow(new LabelNameAlreadyUsedException("O quadro já tem uma etiqueta chamada \"Urgente\""));
// → 409, e $.message com a mensagem do service, intacta
```

Há também `*ServiceTest` puros (`CardServiceTest`, `BoardServiceTest`,
`BoardListServiceTest`) — Mockito sem Spring, para a regra que não precisa de
banco. São os testes mais rápidos do projeto; use-os quando a lógica for
aritmética de `position` ou decisão de fluxo.

## `*IT` — o fluxo real

Sobem a aplicação inteira contra um Postgres de verdade. Herde da classe de
`support/` que corresponde ao que você precisa:

```
IntegrationTest                 @SpringBootTest + container Postgres
└── AuthenticatedIntegrationTest   + usuário logado, equipe pronta, chamadas via service
└── HttpIntegrationTest            + MockMvc, chamadas via HTTP com token
```

### `IntegrationTest` — a base

```java
@SpringBootTest
@TestPropertySource(properties = "planix.upload-dir=target/test-uploads")
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    static { postgres.start(); }
}
```

O container é `static` e sobe uma vez só para toda a suíte — não um por classe.
O `@ServiceConnection` aponta o datasource sozinho; não configure URL na mão. O
Flyway roda as migrations do zero em cada execução, o que significa que **a
suíte também é o teste das suas migrations**.

### `AuthenticatedIntegrationTest` — quando você chama o service direto

Cada teste começa com um usuário novo, sua equipe e o `SecurityContext` já
preenchido:

```java
@BeforeEach
void autenticaUsuarioDeTeste() {
    usuarioLogado = criarUsuario();
    equipeDoTeste = equipeDe(usuarioLogado);
    autenticarComo(usuarioLogado);
}
```

Os helpers que você vai usar:

| Helper | Para quê |
|---|---|
| `criarUsuario()` | outro usuário, com e-mail único — o "invasor" dos testes de permissão |
| `autenticarComo(user)` | troca quem está logado no meio do teste |
| `quadroAberto(nome)` | `BoardCreateRequest` com visibilidade `TEAM` |
| `quadroFechado(nome)` | idem, `RESTRICTED` |
| `quadroAbertoDe(dono, nome)` | quadro na equipe de outra pessoa |

### `HttpIntegrationTest` — quando você quer passar pelo HTTP

Aqui não há atalho de `SecurityContext`: o teste registra um usuário de verdade
e manda o token no header, exercitando filtro, segurança e serialização.

```java
String token = tokenDeUsuarioNovo();
int quadro = criarQuadro(token, "Meu quadro");

mvc.perform(get("/api/boards/" + quadro).with(comToken(token)))
        .andExpect(status().isOk());
```

Helpers: `tokenDeUsuarioNovo()`, `registrar(email, senha)`, `emailUnico()`,
`equipePadrao(token)`, `criarQuadro(token, nome[, visibilidade])`,
`idDe(resultado)` e o `RequestPostProcessor comToken(token)`.

Use esta base quando o que você testa **é** a camada HTTP — CORS, 401 sem token,
`ApiError` fora do MVC. Para regra de negócio, `AuthenticatedIntegrationTest` é
mais direto.

## Nomes

Português, no formato `cenario_resultadoEsperado`:

```java
void criarSemNome_retorna400ComOCampoNoFieldErrors()
void cartaoQueVoceNaoEnxerga_retorna404ComCorpoApiError()
void moverParaListaDeOutroQuadro_retorna409()
void violacaoDeUniqueNoBanco_retorna409EmVezDe500()
```

O nome tem que dizer o cenário **e** o que se espera. `deveCriarCartao()` não
diz nada; `criarSemTitulo_retorna400` diz tudo.

## O que uma feature nova precisa ter

1. **Caminho feliz** num `*IT` — criar, ler, alterar, apagar.
2. **Acesso negado**, obrigatório: um usuário que não é do quadro ou da equipe
   tem que levar **404** (não 403 — ver [SEGURANCA.md](SEGURANCA.md)). Todo
   `*FlowIT` do projeto tem esse teste; o seu também precisa.
3. **Validação** num `*Test`: campo obrigatório vazio → 400 com o campo em
   `fieldErrors`.
4. **Cada exceção nova** que o service lança, provada num `*Test` com o status
   correspondente.
5. **Conflito de regra**, se houver: nome duplicado, contêiner não vazio,
   movimento inválido.

Exemplos completos para copiar: `LabelFlowIT` (o menor), `BoardMemberFlowIT` (o
mais completo em permissão, 16 testes), `InviteConcurrencyIT` (corrida entre
dois requests).

## Rodando

```bash
./mvnw test                        # só os rápidos
./mvnw clean verify                # tudo — o portão de verdade
./mvnw test -Dtest=LabelControllerTest        # uma classe
./mvnw verify -Dit.test=LabelFlowIT           # um IT
```

**Depois de mover classe entre pacotes, use `clean`.** O `.class` antigo fica em
`target/`, o component scan acha as duas cópias e o contexto quebra com
`ConflictingBeanDefinitionException` — um erro que parece de código e é só
build sujo.

Se um `*IT` falhar com erro de conexão, confira se o Docker Desktop está no ar:
o Testcontainers precisa dele.
