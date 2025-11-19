# fix-code.ps1
Write-Host "🔧 Iniciando correções automáticas..." -ForegroundColor Cyan

# Save current location
$originalLocation = Get-Location

try
{
    # Change to parent directory where Gradle project is located
    Set-Location ".."

    # 1. Limpar build anterior
    Write-Host "`n📦 Limpando build anterior..." -ForegroundColor Yellow
    ./gradlew clean

    # 2. Aplicar Spotless
    Write-Host "`n✨ Aplicando formatação com Spotless..." -ForegroundColor Yellow
    ./gradlew spotlessApply

    # 3. Correção específica para espaços entre anotações (caso o Spotless não resolva)
    Write-Host "`n🛠️  Aplicando correções específicas para anotações..." -ForegroundColor Yellow
    Get-ChildItem -Path "src" -Recurse -Include "*.java" | ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        if ($content -match "@(?:\w+\.)*\w+(?:\(.*?\))?\s*\r?\n\s*\r?\n\s*@(?:\w+\.)*\w+(?:\(.*?\))?")
        {
            Write-Host "Corrigindo espaçamento em $( $_.Name )" -ForegroundColor Yellow
            $content = $content -replace "(@(?:\w+\.)*\w+(?:\(.*?\))?)\s*\r?\n\s*\r?\n\s*(@(?:\w+\.)*\w+(?:\(.*?\))?)", "`$1`n`$2"
            $content = $content -replace "(@(?:\w+\.)*\w+(?:\(.*?\))?)\s*\r?\n\s*\r?\n\s*(protected|private|public|static)", "`$1`n`$2"
            Set-Content -Path $_.FullName -Value $content -NoNewline
        }
    }

    # 4. Verificar com Checkstyle
    Write-Host "`n🔍 Verificando com Checkstyle..." -ForegroundColor Yellow
    ./gradlew checkstyleMain checkstyleTest

    if ($LASTEXITCODE -eq 0)
    {
        Write-Host "`n✅ Código está conforme o padrão!" -ForegroundColor Green
    }
    else
    {
        Write-Host "`n⚠️  Ainda há problemas a corrigir manualmente." -ForegroundColor Yellow
        Write-Host "📊 Abrindo relatório..." -ForegroundColor Cyan

        # Check if the report file exists before trying to open it
        $reportPath = "build/reports/checkstyle/main.html"
        if (Test-Path $reportPath)
        {
            Start-Process $reportPath
        }
        else
        {
            Write-Host "⚠️  Relatório não encontrado em: $reportPath" -ForegroundColor Red
        }
    }

    Write-Host "`n📈 Gerando relatório de cobertura..." -ForegroundColor Yellow
    ./gradlew jacocoTestReport

    Write-Host "`n✅ Processo concluído!" -ForegroundColor Green
}
finally
{
    # Return to original location
    Set-Location $originalLocation
}
