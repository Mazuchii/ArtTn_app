-- ============================================
-- SCRIPT DE TEST ET CORRECTION DES RÉACTIONS
-- CORRECTION: Utiliser reaction_type au lieu de type
-- ============================================

-- 1. VÉRIFIER LA STRUCTURE DE LA TABLE
DESCRIBE post_reactions;

-- 2. VÉRIFIER LES CONTRAINTES
SHOW CREATE TABLE post_reactions;

-- 3. COPIER LES DONNÉES DE 'type' VERS 'reaction_type' (si nécessaire)
UPDATE post_reactions 
SET reaction_type = type 
WHERE reaction_type IS NULL OR reaction_type = '';

-- 4. VÉRIFIER S'IL Y A DES DOUBLONS (ne devrait pas en avoir)
SELECT post_id, user_id, COUNT(*) as count
FROM post_reactions
GROUP BY post_id, user_id
HAVING count > 1;

-- 5. SUPPRIMER LES DOUBLONS SI NÉCESSAIRE (garder le plus récent)
DELETE pr1 FROM post_reactions pr1
INNER JOIN post_reactions pr2 
WHERE pr1.id < pr2.id 
AND pr1.post_id = pr2.post_id 
AND pr1.user_id = pr2.user_id;

-- 6. RECALCULER LES COMPTEURS DE LIKES (utiliser reaction_type)
UPDATE post p
SET likes = (
    SELECT COUNT(*) 
    FROM post_reactions 
    WHERE post_id = p.post_id AND reaction_type = 'LIKE'
);

-- 7. RECALCULER LES COMPTEURS DE DISLIKES (utiliser reaction_type)
UPDATE post p
SET dislikes = (
    SELECT COUNT(*) 
    FROM post_reactions 
    WHERE post_id = p.post_id AND reaction_type = 'DISLIKE'
);

-- 8. VÉRIFIER LA COHÉRENCE DES COMPTEURS
SELECT 
    p.post_id,
    p.post_titre,
    p.likes as likes_in_table,
    (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'LIKE') as actual_likes,
    p.dislikes as dislikes_in_table,
    (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'DISLIKE') as actual_dislikes,
    CASE 
        WHEN p.likes = (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'LIKE')
        AND p.dislikes = (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'DISLIKE')
        THEN '✅ OK'
        ELSE '❌ INCOHÉRENT'
    END as status
FROM post p
ORDER BY p.post_id;

-- 9. AFFICHER LES RÉACTIONS PAR POST
SELECT 
    p.post_id,
    p.post_titre,
    pr.user_id,
    u.full_name,
    pr.reaction_type,
    pr.created_at
FROM post p
LEFT JOIN post_reactions pr ON p.post_id = pr.post_id
LEFT JOIN users u ON pr.user_id = u.id
ORDER BY p.post_id, pr.created_at DESC;

-- 10. STATISTIQUES GLOBALES
SELECT 
    COUNT(DISTINCT post_id) as total_posts_with_reactions,
    COUNT(DISTINCT user_id) as total_users_who_reacted,
    SUM(CASE WHEN reaction_type = 'LIKE' THEN 1 ELSE 0 END) as total_likes,
    SUM(CASE WHEN reaction_type = 'DISLIKE' THEN 1 ELSE 0 END) as total_dislikes
FROM post_reactions;

-- 11. POSTS LES PLUS AIMÉS
SELECT 
    p.post_id,
    p.post_titre,
    p.likes,
    p.dislikes,
    (p.likes - p.dislikes) as score
FROM post p
ORDER BY score DESC
LIMIT 10;

-- 12. UTILISATEURS LES PLUS ACTIFS (RÉACTIONS)
SELECT 
    u.id,
    u.full_name,
    u.username,
    COUNT(*) as total_reactions,
    SUM(CASE WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 END) as likes_given,
    SUM(CASE WHEN pr.reaction_type = 'DISLIKE' THEN 1 ELSE 0 END) as dislikes_given
FROM users u
JOIN post_reactions pr ON u.id = pr.user_id
GROUP BY u.id
ORDER BY total_reactions DESC
LIMIT 10;

-- 13. VÉRIFIER LES COMPTEURS NÉGATIFS (ne devrait pas en avoir)
SELECT post_id, post_titre, likes, dislikes
FROM post
WHERE likes < 0 OR dislikes < 0;

-- 14. CORRIGER LES COMPTEURS NÉGATIFS SI NÉCESSAIRE
UPDATE post
SET likes = 0
WHERE likes < 0;

UPDATE post
SET dislikes = 0
WHERE dislikes < 0;

-- 15. VÉRIFIER LES DONNÉES DANS LES DEUX COLONNES
SELECT 
    id,
    post_id,
    user_id,
    reaction_type,
    type,
    CASE 
        WHEN reaction_type = type THEN '✅ COHÉRENT'
        WHEN reaction_type IS NULL OR reaction_type = '' THEN '⚠️ reaction_type VIDE'
        WHEN type IS NULL OR type = '' THEN '⚠️ type VIDE'
        ELSE '❌ INCOHÉRENT'
    END as status
FROM post_reactions
ORDER BY id DESC
LIMIT 20;

-- 16. AFFICHER LES POSTS SANS RÉACTIONS
SELECT 
    p.post_id,
    p.post_titre,
    p.likes,
    p.dislikes,
    p.views
FROM post p
LEFT JOIN post_reactions pr ON p.post_id = pr.post_id
WHERE pr.id IS NULL
ORDER BY p.date_creation DESC;

-- 17. NETTOYER LA COLONNE 'type' (OPTIONNEL - À FAIRE APRÈS VALIDATION)
-- ATTENTION: Ne pas exécuter si Symfony utilise encore 'type'
-- UPDATE post_reactions SET type = NULL;

-- 18. CRÉER UN RAPPORT DE TEST
SELECT 
    'Total Posts' as metric,
    COUNT(*) as value
FROM post
UNION ALL
SELECT 
    'Total Reactions',
    COUNT(*)
FROM post_reactions
UNION ALL
SELECT 
    'Total Likes',
    SUM(likes)
FROM post
UNION ALL
SELECT 
    'Total Dislikes',
    SUM(dislikes)
FROM post
UNION ALL
SELECT 
    'Posts with Reactions',
    COUNT(DISTINCT post_id)
FROM post_reactions
UNION ALL
SELECT 
    'Users who Reacted',
    COUNT(DISTINCT user_id)
FROM post_reactions
UNION ALL
SELECT 
    'Reactions with reaction_type',
    COUNT(*)
FROM post_reactions
WHERE reaction_type IS NOT NULL AND reaction_type != ''
UNION ALL
SELECT 
    'Reactions with type',
    COUNT(*)
FROM post_reactions
WHERE type IS NOT NULL AND type != '';

-- ============================================
-- FIN DU SCRIPT
-- ============================================
