# Derruba o stack de DESENVOLVIMENTO do backend. A producao segue de pe.
#
# Sem -v de proposito: mesmo o banco de dev costuma ter dados que voce quer
# manter entre sessoes. Para zerar o banco de dev de verdade:
#   docker compose -p planix-dev -f compose.yaml -f compose.dev.yaml down -v

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

docker compose -p planix-dev -f compose.yaml -f compose.dev.yaml --profile app down @args
