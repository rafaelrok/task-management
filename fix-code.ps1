# fix-code.ps1
Write-Host "🔧 Iniciando correções automáticas..." -ForegroundColor Cyan

# 1. Limpar build anterior
Write-Host "`n📦 Limpando build anterior..." -ForegroundColor Yellow
./gradlew clean

# 2. Aplicar Spotless
Write-Host "`n✨ Aplicando formatação com Spotless..." -ForegroundColor Yellow
./gradlew spotlessApply

# 3. Verificar com Checkstyle
Write-Host "`n🔍 Verificando com Checkstyle..." -ForegroundColor Yellow
./gradlew checkstyleMain checkstyleTest

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Código está conforme o padrão!" -ForegroundColor Green
} else {
    Write-Host "`n⚠️  Ainda há problemas a corrigir manualmente." -ForegroundColor Yellow
    Write-Host "📊 Abrindo relatório..." -ForegroundColor Cyan
    Start-Process "build/reports/checkstyle/main.html"
}

Write-Host "`n📈 Gerando relatório de cobertura..." -ForegroundColor Yellow
./gradlew jacocoTestReport

Write-Host "`n✅ Processo concluído!" -ForegroundColor Green