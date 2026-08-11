# Sobe o backend em modo DESENVOLVIMENTO, isolado da producao.
#
# Projeto Compose proprio (planix-dev), o que da automaticamente volumes
# separados: o banco de dev e o volume planix-dev_planix_pgdata, sem nenhuma
# relacao com planix_planix_pgdata (producao).
#
#   API .... http://localhost:8081     (producao: 8080)
#   Postgres 127.0.0.1:5434            (producao: 5433)
#
# Este script existe para evitar erro humano: rodar o mesmo comando SEM
# "-p planix-dev" substituiria os containers de producao.
#
# Uso:
#   .\scripts\dev-up.ps1          -> em primeiro plano, com logs
#   .\scripts\dev-up.ps1 -d       -> em segundo plano
#   .\scripts\dev-down.ps1        -> derruba so o stack de dev

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

# Cache do Maven, declarado como external no compose.dev.yaml. Idempotente.
docker volume create planix_planix_m2 | Out-Null

docker compose -p planix-dev -f compose.yaml -f compose.dev.yaml --profile app up @args
