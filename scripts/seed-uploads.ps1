# Gera os arquivos que o seed de desenvolvimento referencia.
#
# O R__seed_dev.sql grava caminhos em attachments.stored_filename e em
# users.avatar_path, mas SQL nao escreve em disco. Sem este script o metadado
# aparece na UI e o download estoura StorageException ("Arquivo nao encontrado
# no disco").
#
# Os arquivos vao para os DOIS lugares onde a API pode procurar:
#   .\uploads\            -> quando voce roda a aplicacao no host
#   volume do dev         -> quando roda pelo .\scripts\dev-up.ps1 (/data/uploads)
#
# Uso:
#   .\scripts\seed-uploads.ps1

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$utf8SemBom = New-Object System.Text.UTF8Encoding($false)

$brief = @'
# Brief do produto — Planix

## Problema

Times pequenos perdem o fio da meada entre a reuniao e a entrega. O que foi
combinado vira mensagem solta no chat e ninguem sabe de quem e a bola.

## Publico

Times de 3 a 15 pessoas que ja usam quadro Kanban e querem menos ceremonia.

## Metricas

- ativacao: primeiro cartao criado em ate 5 minutos apos o cadastro
- retencao: 40% dos times ativos na quarta semana

## Fora de escopo

Relatorio gerencial, controle de horas e integracao com ERP.
'@

$ata = @'
Ata da reuniao de planejamento — quinta-feira, 14h

Presentes: Sergio, Ana, Bruno, Carla.

1. Roadmap do trimestre aprovado sem mudancas.
2. Busca global entra antes do modo escuro: apareceu em 7 das 10 entrevistas.
3. Bruno assume a investigacao do erro 500 no login ate sexta.
4. Carla apresenta o novo fluxo de convite na proxima terca.
5. Pendencia: decidir se o plano gratuito limita quadros ou membros.

Proxima reuniao: mesma hora, na semana que vem.
'@

$spec = @'
# Especificacao — convite por link

## Regra

Um convite pertence a uma EQUIPE, nunca a um quadro. Quem aceita entra como
MEMBER ou ADMIN, conforme o papel gravado no convite.

## Estados

| Estado    | Como identificar                  |
|-----------|-----------------------------------|
| ativo     | uses < max_uses, nao expirou      |
| esgotado  | uses = max_uses                   |
| expirado  | expires_at no passado             |
| revogado  | revoked_at preenchido             |

## Seguranca

O token viaja na URL e so o hash SHA-256 fica no banco. Convite invalido
responde 404 — 403 confirmaria que aquele link existiu.

## Concorrencia

Dois cliques simultaneos no ultimo uso disponivel: o segundo recebe 409.
'@

# Cada avatar/mockup e um PNG solido, so para o arquivo existir de verdade.
function New-PngSolido {
    param([string]$Caminho, [int]$Lado, [int]$R, [int]$G, [int]$B)

    Add-Type -AssemblyName System.Drawing
    $bmp = New-Object System.Drawing.Bitmap($Lado, $Lado)
    $gfx = [System.Drawing.Graphics]::FromImage($bmp)
    $cor = [System.Drawing.Color]::FromArgb($R, $G, $B)
    $gfx.Clear($cor)
    $bmp.Save($Caminho, [System.Drawing.Imaging.ImageFormat]::Png)
    $gfx.Dispose()
    $bmp.Dispose()
}

# Monta a arvore num diretorio temporario e depois copia para os dois destinos.
$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("planix-seed-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path (Join-Path $tmp "documents")        | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tmp "profile_pictures") | Out-Null

[System.IO.File]::WriteAllText((Join-Path $tmp "documents\seed-brief-produto.md"), $brief, $utf8SemBom)
[System.IO.File]::WriteAllText((Join-Path $tmp "documents\seed-ata-reuniao.txt"),  $ata,   $utf8SemBom)
[System.IO.File]::WriteAllText((Join-Path $tmp "documents\seed-especificacao.md"), $spec,  $utf8SemBom)

New-PngSolido -Caminho (Join-Path $tmp "documents\seed-mockup.png")          -Lado 480 -R 226 -G 232 -B 240
New-PngSolido -Caminho (Join-Path $tmp "profile_pictures\seed-avatar-1.png") -Lado 128 -R  37 -G  99 -B 235
New-PngSolido -Caminho (Join-Path $tmp "profile_pictures\seed-avatar-2.png") -Lado 128 -R 219 -G  39 -B 119
New-PngSolido -Caminho (Join-Path $tmp "profile_pictures\seed-avatar-3.png") -Lado 128 -R  22 -G 163 -B  74
New-PngSolido -Caminho (Join-Path $tmp "profile_pictures\seed-avatar-4.png") -Lado 128 -R 217 -G 119 -B   6

# Destino 1: a pasta do host, usada quando a aplicacao roda fora do Docker.
New-Item -ItemType Directory -Path ".\uploads" -Force | Out-Null
Copy-Item -Path (Join-Path $tmp "*") -Destination ".\uploads" -Recurse -Force
Write-Host "Arquivos copiados para .\uploads"

# Destino 2: o volume do stack de dev. Se o container nao estiver de pe, o
# script avisa em vez de falhar — a copia do host ja esta feita.
$compose = @("-p", "planix-dev", "-f", "compose.yaml", "-f", "compose.dev.yaml", "--profile", "app")
$appId = (docker compose @compose ps -q app 2>$null)

if ([string]::IsNullOrWhiteSpace($appId)) {
    Write-Warning "Container 'app' do stack de dev nao esta rodando. Suba com .\scripts\dev-up.ps1 -d e rode este script de novo para popular /data/uploads."
} else {
    docker compose @compose cp "$tmp\." app:/data/uploads
    Write-Host "Arquivos copiados para /data/uploads no container de dev"
}

Remove-Item -Recurse -Force $tmp

# Os tamanhos abaixo sao os que o seed grava em attachments.size_bytes. Se
# voce mudar o conteudo dos arquivos acima, atualize o R__seed_dev.sql.
Write-Host ""
Write-Host "Tamanhos em bytes (conferir com o R__seed_dev.sql):"
Get-ChildItem -Recurse -File ".\uploads" |
    Where-Object { $_.Name -like "seed-*" } |
    ForEach-Object { "{0,-28} {1,6}" -f $_.Name, $_.Length }
