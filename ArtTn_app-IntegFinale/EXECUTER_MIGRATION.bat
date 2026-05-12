@echo off
chcp 65001 >nul
echo ╔══════════════════════════════════════════════════════════════════════════════╗
echo ║              🔧 MIGRATION: type → reaction_type                              ║
echo ╚══════════════════════════════════════════════════════════════════════════════╝
echo.
echo 📋 Ce script va migrer les données de 'type' vers 'reaction_type'
echo.
echo ⚠️  IMPORTANT: Assurez-vous que MySQL est en cours d'exécution!
echo.
pause
echo.
echo 🔄 Exécution de la migration...
echo.

REM Demander les informations de connexion
set /p MYSQL_USER="Entrez le nom d'utilisateur MySQL (par défaut: root): "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_DB="Entrez le nom de la base de données (par défaut: museum_db): "
if "%MYSQL_DB%"=="" set MYSQL_DB=museum_db

echo.
echo 📊 Connexion à MySQL...
echo    Utilisateur: %MYSQL_USER%
echo    Base de données: %MYSQL_DB%
echo.

REM Exécuter le script SQL
mysql -u %MYSQL_USER% -p %MYSQL_DB% < MIGRATION_TYPE_TO_REACTION_TYPE.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ MIGRATION TERMINÉE AVEC SUCCÈS!
    echo.
    echo 📋 Prochaines étapes:
    echo    1. Vérifier les résultats dans MySQL
    echo    2. Compiler le projet Java: mvn clean compile
    echo    3. Lancer l'application: mvn javafx:run
    echo    4. Tester les réactions Like/Dislike
    echo.
) else (
    echo.
    echo ❌ ERREUR LORS DE LA MIGRATION!
    echo.
    echo 🔍 Vérifiez:
    echo    - MySQL est en cours d'exécution
    echo    - Le mot de passe est correct
    echo    - La base de données existe
    echo    - Le fichier MIGRATION_TYPE_TO_REACTION_TYPE.sql est présent
    echo.
)

echo.
pause
