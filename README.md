# Planix

Gerenciador de tarefas no estilo Trello.

## Stack

Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation) · PostgreSQL 18 · Flyway · Lombok · Maven · Docker Compose · JUnit 5 + Testcontainers

## Pré-requisitos

- **JDK 21**
- **Docker** e **Docker Compose 2.24+**

Há dois ambientes que rodam **ao mesmo tempo**, em portas e bancos separados:

| Ambiente | API | PostgreSQL | Volume do banco | Sobe no boot? |
|---|---|---|---|---|
| **Produção** | `localhost:8080` | `localhost:5433` | `planix_planix_pgdata` | sim (`restart: always`) |
| **Desenvolvimento** | `localhost:8081` | `localhost:5434` | `planix-dev_planix_pgdata` | não (`restart: "no"`) |

As portas ficam presas a `127.0.0.1` — acessíveis pela sua máquina, invisíveis na rede local.

O isolamento dos dados vem do nome do projeto Compose: o stack de dev roda com
`-p planix-dev`, então ganha volumes próprios. **Mexer no dev nunca toca nos
dados de produção.** Por isso o banco de dev nasce vazio, com o Flyway rodando
as migrations do zero.

Em produção o navegador **não** usa a porta 8080: o nginx do
[planix-frontend-2](../planix-frontend-2) faz proxy de `/api` para cá, de modo
que tudo passa por `http://localhost:5173`. A porta 8080 segue publicada só
para o Scalar e para depuração.

## Antes da primeira execução

Copie `.env.example` para `.env` e troque o segredo. O `docker compose` lê esse arquivo sozinho e **recusa subir sem ele** — inclusive o banco, porque a interpolação vale para o arquivo inteiro.

```bash
cp .env.example .env          # no Windows: copy .env.example .env
```

A API exige autenticação: comece por `POST /api/auth/register` e mande `Authorization: Bearer <accessToken>` nas demais chamadas. A referência completa dos endpoints é o [Scalar](#documentação-da-api).

## Produção

Sobe uma vez e volta sozinha a cada boot, junto com o Docker Desktop:

```bash
docker compose --profile app up -d --build
```

O `--profile app` é obrigatório — sem ele só o Postgres sobe. Este stack é o
dono da rede `planix-net`, que o frontend consome como externa, então **suba o
backend antes do frontend**.

## Desenvolvimento

```powershell
.\scripts\dev-up.ps1 -d       # projeto planix-dev: API em 8081, banco em 5434
.\scripts\dev-down.ps1        # derruba só o dev; a produção segue no ar
```

Hot reload via DevTools, com o projeto montado dentro do container.

Use os scripts em vez do comando cru: rodar
`docker compose -f compose.yaml -f compose.dev.yaml --profile app up` **sem**
`-p planix-dev` substituiria os containers de produção.

Para rodar a aplicação direto na máquina (fora do Docker), suba só o banco de
dev e aponte o datasource para a `5434`:

```powershell
docker compose -p planix-dev -f compose.yaml -f compose.dev.yaml up -d db
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5434/planix"
.\mvnw.cmd spring-boot:run
```

⚠️ O default de `application.properties` é a `5433`, que agora é o banco de
**produção**.

## Documentação da API

**http://localhost:8080/scalar** — gerado a partir das anotações dos
controllers, então acompanha o código.

Para trabalhar no código (arquitetura, convenções, como criar uma feature), veja
[`CLAUDE.md`](CLAUDE.md) e [`docs/`](docs/README.md).

## Backup

Os dados vivem em volumes nomeados e sobrevivem a reboot, `restart` e
`docker compose down`. O que os apaga é `down -v`, `docker volume rm` e o
"Clean / Purge data" do Docker Desktop.

```powershell
.\scripts\backup.ps1          # dump do banco + tarball dos uploads em backups/
```

As instruções de restauração estão comentadas no fim do próprio script.

### Comandos úteis

```bash
docker compose ps                            # o que está no ar
docker compose logs -f app                   # logs da aplicação
docker compose exec db psql -U planix -d planix
docker compose --profile app down            # derruba os containers, mantém os dados
docker compose --profile app down -v         # derruba e APAGA banco e anexos
```

⚠️ Nunca use `-v` no stack de produção sem ter rodado o backup antes.

## Testes

```bash
./mvnw test            # unitários e de camada web
./mvnw clean verify    # + testes de integração (exige Docker rodando)
```