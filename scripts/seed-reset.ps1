# Força o seed de desenvolvimento a rodar de novo.
#
# O R__seed_dev.sql e uma repeatable migration: o Flyway so reaplica quando o
# checksum do arquivo muda. Para recarregar o banco sem editar o SQL, apagamos
# o registro dele no flyway_schema_history e reiniciamos a aplicacao — no boot
# o Flyway ve um repeatable "novo" e roda de novo.
#
# ATENCAO: isto APAGA todos os dados do banco de DEV (o seed comeca com
# TRUNCATE) e os repovoa. Nao toca em producao: o stack de dev tem projeto,
# volume e porta proprios.
#
# Uso:
#   .\scripts\seed-reset.ps1

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$compose = @("-p", "planix-dev", "-f", "compose.yaml", "-f", "compose.dev.yaml", "--profile", "app")

if ([string]::IsNullOrWhiteSpace((docker compose @compose ps -q db))) {
    throw "O banco de dev nao esta rodando. Suba o stack com .\scripts\dev-up.ps1 -d antes."
}

Write-Host "Removendo o registro do seed no flyway_schema_history..."
docker compose @compose exec -T db `
    psql -U planix -d planix -v ON_ERROR_STOP=1 `
    -c "DELETE FROM flyway_schema_history WHERE script = 'R__seed_dev.sql'"

Write-Host "Reiniciando a aplicacao para o Flyway reaplicar o seed..."
docker compose @compose restart app

Write-Host ""
Write-Host "Pronto. Acompanhe o resultado com:"
Write-Host "  docker compose -p planix-dev -f compose.yaml -f compose.dev.yaml --profile app logs -f app"
