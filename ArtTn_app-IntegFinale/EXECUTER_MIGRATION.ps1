# Script PowerShell pour exécuter la migration
# Encodage UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "╔══════════════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║              🔧 MIGRATION: type → reaction_type                              ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Ce script va migrer les données de 'type' vers 'reaction_type'" -ForegroundColor Yellow
Write-Host ""
Write-Host "⚠️  IMPORTANT: Assurez-vous que MySQL est en cours d'exécution!" -ForegroundColor Red
Write-Host ""

# Demander confirmation
$confirmation = Read-Host "Voulez-vous continuer? (O/N)"
if ($confirmation -ne 'O' -and $confirmation -ne 'o') {
    Write-Host "❌ Migration annulée." -ForegroundColor Red
    exit
}

Write-Host ""

# Demander les informations de connexion
$mysqlUser = Read-Host "Entrez le nom d'utilisateur MySQL (par défaut: root)"
if ([string]::IsNullOrWhiteSpace($mysqlUser)) {
    $mysqlUser = "root"
}

$mysqlDb = Read-Host "Entrez le nom de la base de données (par défaut: museum_db)"
if ([string]::IsNullOrWhiteSpace($mysqlDb)) {
    $mysqlDb = "museum_db"
}

Write-Host ""
Write-Host "📊 Connexion à MySQL..." -ForegroundColor Cyan
Write-Host "   Utilisateur: $mysqlUser" -ForegroundColor Gray
Write-Host "   Base de données: $mysqlDb" -ForegroundColor Gray
Write-Host ""

# Vérifier si le fichier SQL existe
if (-not (Test-Path "MIGRATION_TYPE_TO_REACTION_TYPE.sql")) {
    Write-Host "❌ ERREUR: Le fichier MIGRATION_TYPE_TO_REACTION_TYPE.sql n'existe pas!" -ForegroundColor Red
    Write-Host ""
    pause
    exit
}

# Exécuter le script SQL
Write-Host "🔄 Exécution de la migration..." -ForegroundColor Yellow
Write-Host ""

try {
    # Exécuter MySQL
    $process = Start-Process -FilePath "mysql" -ArgumentList "-u", $mysqlUser, "-p", $mysqlDb -RedirectStandardInput "MIGRATION_TYPE_TO_REACTION_TYPE.sql" -NoNewWindow -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        Write-Host ""
        Write-Host "✅ MIGRATION TERMINÉE AVEC SUCCÈS!" -ForegroundColor Green
        Write-Host ""
        Write-Host "📋 Prochaines étapes:" -ForegroundColor Cyan
        Write-Host "   1. Vérifier les résultats dans MySQL" -ForegroundColor Gray
        Write-Host "   2. Compiler le projet Java: mvn clean compile" -ForegroundColor Gray
        Write-Host "   3. Lancer l'application: mvn javafx:run" -ForegroundColor Gray
        Write-Host "   4. Tester les réactions Like/Dislike" -ForegroundColor Gray
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "❌ ERREUR LORS DE LA MIGRATION!" -ForegroundColor Red
        Write-Host ""
        Write-Host "🔍 Vérifiez:" -ForegroundColor Yellow
        Write-Host "   - MySQL est en cours d'exécution" -ForegroundColor Gray
        Write-Host "   - Le mot de passe est correct" -ForegroundColor Gray
        Write-Host "   - La base de données existe" -ForegroundColor Gray
        Write-Host ""
    }
} catch {
    Write-Host ""
    Write-Host "❌ ERREUR: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "🔍 Vérifiez que MySQL est installé et accessible depuis le PATH" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host ""
pause
