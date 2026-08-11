# Backup manual do Planix em PRODUCAO: dump do Postgres + tarball dos uploads.
#
# Os dados vivem em volumes nomeados (planix_planix_pgdata e
# planix_planix_uploads), que sobrevivem a reboot, restart e "docker compose
# down". O que os destroi e "down -v", "docker volume rm" e o Clean/Purge data
# do Docker Desktop. Este script e a rede de seguranca contra isso.
#
# Uso: .\scripts\backup.ps1

$ErrorActionPreference = "Stop"

$ts   = Get-Date -Format "yyyy-MM-dd_HHmm"
$dest = Join-Path $PSScriptRoot "..\backups"
New-Item -ItemType Directory -Force $dest | Out-Null
$dest = (Resolve-Path $dest).Path

# Formato custom (-F c, binario) gerado DENTRO do container e trazido com
# docker cp. Nao usar pipe do PowerShell aqui: ele reencodaria a saida e
# corromperia o dump.
Write-Host "Dump do banco..." -ForegroundColor Cyan
docker exec planix-db-1 pg_dump -U planix -d planix -F c -f /tmp/dump.pgc
docker cp planix-db-1:/tmp/dump.pgc "$dest\planix_$ts.pgc"
docker exec planix-db-1 rm /tmp/dump.pgc

# Anexos e avatares, lidos direto do volume por um container descartavel
# (assim funciona mesmo com a aplicacao parada).
Write-Host "Backup dos uploads..." -ForegroundColor Cyan
docker run --rm -v planix_planix_uploads:/data -v "${dest}:/backup" alpine `
  tar czf "/backup/uploads_$ts.tgz" -C /data .

Write-Host "`nPronto em $dest" -ForegroundColor Green
Get-ChildItem $dest -Filter "*$ts*" | Format-Table Name, @{n="MB";e={[math]::Round($_.Length/1MB,2)}}

# --- Como restaurar -----------------------------------------------------
# Banco (--clean derruba os objetos existentes antes de recriar):
#   docker cp .\backups\planix_AAAA-MM-DD_HHMM.pgc planix-db-1:/tmp/r.pgc
#   docker exec planix-db-1 pg_restore -U planix -d planix --clean --if-exists /tmp/r.pgc
#
# Uploads:
#   docker run --rm -v planix_planix_uploads:/data -v "${PWD}\backups:/backup" alpine `
#     tar xzf /backup/uploads_AAAA-MM-DD_HHMM.tgz -C /data
