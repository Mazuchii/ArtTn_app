-- ============================================================
-- FIX : Libérer les transactions bloquées (Lock wait timeout)
-- Exécuter dans phpMyAdmin si l'erreur persiste après le fix Java
-- ============================================================

-- 1. Voir les transactions actives qui bloquent
SELECT
    trx_id,
    trx_state,
    trx_started,
    trx_mysql_thread_id,
    trx_query,
    trx_rows_locked
FROM information_schema.INNODB_TRX
ORDER BY trx_started;

-- 2. Voir les locks en attente
SELECT
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread,
    b.trx_query AS blocking_query
FROM information_schema.INNODB_LOCK_WAITS w
JOIN information_schema.INNODB_TRX b ON b.trx_id = w.blocking_trx_id
JOIN information_schema.INNODB_TRX r ON r.trx_id = w.requesting_trx_id;

-- 3. Tuer les connexions bloquantes (remplacer X par le thread_id trouvé ci-dessus)
-- KILL X;

-- 4. Solution rapide : redémarrer MySQL libère tous les locks
-- (à faire depuis le panneau de contrôle XAMPP/WAMP)

-- 5. Vérifier que les tables ne sont plus verrouillées
SHOW OPEN TABLES WHERE In_use > 0;

-- 6. Réinitialiser les transactions orphelines
-- (si les tables orders/order_items/products sont bloquées)
UNLOCK TABLES;
