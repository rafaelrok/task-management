# setup-checkstyle.ps1
Write-Host "Configurando Checkstyle..." -ForegroundColor Green

# Remover diretório antigo
if (Test-Path "src/main/resources/checkstyle") {
    Write-Host "Removendo diretório antigo..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "src/main/resources/checkstyle"
}

# Criar estrutura
Write-Host "Criando estrutura de diretórios..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "config/checkstyle" | Out-Null

# Verificar arquivos
$xmlExists = Test-Path "config/checkstyle/checkstyle.xml"
$suppressExists = Test-Path "config/checkstyle/checkstyle-suppressions.xml"

if ($xmlExists -and $suppressExists) {
    Write-Host "✓ Arquivos de configuração encontrados!" -ForegroundColor Green
} else {
    Write-Host "✗ Arquivos de configuração não encontrados!" -ForegroundColor Red
    Write-Host "Crie manualmente os arquivos:" -ForegroundColor Yellow
    Write-Host "  - config/checkstyle/checkstyle.xml" -ForegroundColor Yellow
    Write-Host "  - config/checkstyle/checkstyle-suppressions.xml" -ForegroundColor Yellow
    exit 1
}

# Limpar build
Write-Host "Limpando build..." -ForegroundColor Yellow
./gradlew clean

Write-Host "`n✓ Configuração concluída!" -ForegroundColor Green
Write-Host "Execute: ./gradlew checkstyleMain" -ForegroundColor Cyan