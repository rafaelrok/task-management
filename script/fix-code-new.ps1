# fix-code.ps1
Write-Host "🔧 Iniciando correções automáticas..." -ForegroundColor Cyan

$originalLocation = Get-Location

try
{
    Set-Location ".."

    # 1. Limpar cache corrompido
    Write-Host "`n🧹 Limpando cache do Gradle..." -ForegroundColor Yellow
    if (Test-Path ".gradle\configuration-cache")
    {
        Remove-Item -Recurse -Force ".gradle\configuration-cache"
        Write-Host "  ✓ Cache removido" -ForegroundColor Green
    }

    # 2. Limpar build
    Write-Host "`n📦 Limpando build anterior..." -ForegroundColor Yellow
    ./gradlew clean --no-configuration-cache

    # 3. Correção ANTES do Spotless (para @RequestBody)
    Write-Host "`n🛠️  Corrigindo indentação de @RequestBody..." -ForegroundColor Yellow
    Get-ChildItem -Path "src" -Recurse -Include "*.java" | ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        $modified = $false

        # Corrige @RequestBody com quebra de linha e indentação excessiva
        if ($content -match '@(?:Valid\s+)?@RequestBody\s*\r?\n\s{12,}')
        {
            Write-Host "  → $( $_.Name )" -ForegroundColor Yellow
            # Une @Valid @RequestBody na mesma linha seguido do parâmetro
            $content = $content -replace '(@Valid)\s+@RequestBody\s*\r?\n\s{12,}(\w+)', '$1 @RequestBody $2'
            $content = $content -replace '@RequestBody\s*\r?\n\s{12,}(\w+)', '@RequestBody $1'
            $modified = $true
        }

        # Remove linhas em branco duplicadas entre anotações
        if ($content -match '@\w+[^\r\n]*\r?\n\s*\r?\n\s*@\w+')
        {
            $content = $content -replace '(@\w+(?:\([^)]*\))?)\s*\r?\n\s*\r?\n\s*(@\w+)', "`$1`n`$2"
            $modified = $true
        }

        if ($modified)
        {
            Set-Content -Path $_.FullName -Value $content -NoNewline
        }
    }

    # 4. Aplicar Spotless SEM cache
    Write-Host "`n✨ Aplicando formatação com Spotless..." -ForegroundColor Yellow
    ./gradlew spotlessApply --no-configuration-cache --rerun-tasks

    # 5. Correção PÓS-Spotless (caso ele reintroduza o problema)
    Write-Host "`n🔧 Verificando correções finais..." -ForegroundColor Yellow
    Get-ChildItem -Path "src" -Recurse -Include "*.java" | ForEach-Object {
        $content = Get-Content $_.FullName -Raw

        # Última verificação para @RequestBody
        if ($content -match '@RequestBody\s*\r?\n\s{12,}')
        {
            Write-Host "  → Correção final em $( $_.Name )" -ForegroundColor Yellow
            $content = $content -replace '@RequestBody\s*\r?\n\s{12,}', '@RequestBody '
            Set-Content -Path $_.FullName -Value $content -NoNewline
        }
    }

    # 6. Verificar com Checkstyle
    Write-Host "`n🔍 Verificando com Checkstyle..." -ForegroundColor Yellow
    ./gradlew checkstyleMain checkstyleTest --no-configuration-cache

    if ($LASTEXITCODE -eq 0)
    {
        Write-Host "`n✅ Código está conforme o padrão!" -ForegroundColor Green
    }
    else
    {
        Write-Host "`n⚠️  Ainda há problemas. Abrindo relatório..." -ForegroundColor Yellow
        $reportPath = "build/reports/checkstyle/main.html"
        if (Test-Path $reportPath)
        {
            Start-Process $reportPath
        }
    }

    Write-Host "`n📈 Gerando relatório de cobertura..." -ForegroundColor Yellow
    ./gradlew jacocoTestReport --no-configuration-cache

    Write-Host "`n✅ Processo concluído!" -ForegroundColor Green

}
finally
{
    Set-Location $originalLocation
}
