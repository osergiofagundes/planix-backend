# Planix

Gerenciador de tarefas no estilo Trello.

## Stack

Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation) · PostgreSQL 18 · Flyway · Lombok · Maven · Docker Compose · JUnit 5 + Testcontainers

## Pré-requisitos

- **JDK 21**
- **Docker** e **Docker Compose 2.24+**

Em todos os modos abaixo a API sobe em `http://localhost:8080` e o PostgreSQL fica exposto em `localhost:5433`.

## Antes da primeira execução

Copie `.env.example` para `.env` e troque o segredo. O `docker compose` lê esse arquivo sozinho e **recusa subir sem ele** — inclusive o banco, porque a interpolação vale para o arquivo inteiro.

```bash
cp .env.example .env          # no Windows: copy .env.example .env
```

A API exige autenticação: comece por `POST /api/auth/register` e mande `Authorization: Bearer <accessToken>` nas demais chamadas. O roteiro completo está em `docs/api.http`.

## Rodando localmente

Banco em container, aplicação rodando direto na máquina. É o fluxo do dia a dia:

```bash
docker compose up -d          # sobe só o Postgres
./mvnw spring-boot:run        # no Windows: .\mvnw.cmd spring-boot:run
```

## Documentação da API

**http://localhost:8080/scalar**

## Rodando via Docker

```bash
docker compose --profile app up --build
```

### Modo dev (hot reload)

```bash
docker compose -f compose.yaml -f compose.dev.yaml --profile app up
```

### Comandos úteis

```bash
docker compose ps                            # o que está no ar
docker compose logs -f app                   # logs da aplicação
docker compose exec db psql -U planix -d planix
docker compose down                          # derruba os containers, mantém os dados
docker compose down -v                       # derruba e APAGA banco e anexos
```

## Testes

```bash
./mvnw test            # unitários e de camada web
./mvnw clean verify    # + testes de integração (exige Docker rodando)
```