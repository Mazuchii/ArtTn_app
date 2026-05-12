-- ============================================================
-- FIX : Resynchronisation des capacités des événements
-- current_capacity = places RESTANTES (max_capacity - tickets réservés)
-- À exécuter si current_capacity est désynchronisé
-- ============================================================

-- Vérifier l'état actuel (avant correction)
SELECT 
    e.id,
    e.title,
    e.max_capacity,
    e.current_capacity AS places_restantes_stored,
    COALESCE(SUM(r.number_of_tickets), 0) AS tickets_reserves,
    e.max_capacity - COALESCE(SUM(r.number_of_tickets), 0) AS places_restantes_reelles,
    CASE 
        WHEN e.current_capacity = e.max_capacity - COALESCE(SUM(r.number_of_tickets), 0) THEN '✅ OK'
        ELSE '❌ DÉSYNCHRONISÉ'
    END AS statut
FROM events e
LEFT JOIN reservations r ON r.event_id = e.id 
    AND r.status NOT IN ('ANNULEE', 'REFUSEE')
GROUP BY e.id, e.title, e.max_capacity, e.current_capacity
ORDER BY e.id;

-- ============================================================
-- Resynchroniser : current_capacity = max_capacity - SUM(tickets réservés actifs)
-- ============================================================
UPDATE events e
SET e.current_capacity = GREATEST(0,
    e.max_capacity - (
        SELECT COALESCE(SUM(r.number_of_tickets), 0)
        FROM reservations r
        WHERE r.event_id = e.id
          AND r.status NOT IN ('ANNULEE', 'REFUSEE')
    )
);

-- Vérifier après correction
SELECT 
    e.id,
    e.title,
    e.max_capacity,
    e.current_capacity AS places_restantes,
    e.max_capacity - e.current_capacity AS tickets_reserves,
    CASE 
        WHEN e.current_capacity < 0 THEN '❌ NÉGATIF (problème!)'
        WHEN e.current_capacity > e.max_capacity THEN '❌ DÉPASSE MAX (problème!)'
        ELSE '✅ OK'
    END AS statut
FROM events e
ORDER BY e.id;

-- ============================================================
-- FIN
-- ============================================================
