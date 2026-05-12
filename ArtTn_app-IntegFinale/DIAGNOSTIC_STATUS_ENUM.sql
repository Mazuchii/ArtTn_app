-- ============================================================
-- DIAGNOSTIC : Vérifier les valeurs ENUM de la colonne status
-- Exécuter dans phpMyAdmin pour identifier le problème
-- ============================================================

-- 1. Voir le type exact de la colonne status dans reservations
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reservations'
  AND COLUMN_NAME = 'status';

-- 2. Voir toutes les valeurs distinctes actuellement en DB
SELECT DISTINCT status, COUNT(*) as nb
FROM reservations
GROUP BY status
ORDER BY nb DESC;

-- ============================================================
-- SOLUTION A : Si la colonne est ENUM avec des valeurs Symfony
-- (ex: pending, confirmed, cancelled, refused)
-- Ajouter les valeurs Java à l'ENUM existant
-- ============================================================

-- Décommenter et adapter selon les valeurs détectées ci-dessus :

-- ALTER TABLE reservations
-- MODIFY COLUMN status ENUM(
--     'pending', 'confirmed', 'cancelled', 'refused',
--     'EN_ATTENTE', 'CONFIRMEE', 'ANNULEE', 'REFUSEE'
-- ) NOT NULL DEFAULT 'pending';

-- ============================================================
-- SOLUTION B : Si la colonne est VARCHAR — aucun problème
-- Le code Java fonctionne directement
-- ============================================================

-- ============================================================
-- SOLUTION C (recommandée) : Convertir en VARCHAR pour
-- accepter n'importe quelle valeur des deux projets
-- ============================================================

-- ALTER TABLE reservations
-- MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE';

-- ============================================================
-- FIN DU DIAGNOSTIC
-- ============================================================
