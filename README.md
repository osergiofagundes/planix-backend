# Planix

Gerenciador de tarefas no estilo Trello.

## Stack

Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation) · PostgreSQL 18 · Flyway · Lombok · Maven · Docker Compose · JUnit 5 + Testcontainers

## Pré-requisitos

- **JDK 21**
- **Docker** e **Docker Compose 2.24+**

Em todos os modos abaixo a API sobe em `http://localhost:8080` e o PostgreSQL fica exposto em `localhost:5433`.
## Rodando localmente

Banco em container, aplicação rodando direto na máquina. É o fluxo do dia a dia:

```bash
docker compose up -d          # sobe só o Postgres
./mvnw spring-boot:run        # no Windows: .\mvnw.cmd spring-boot:run
```

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