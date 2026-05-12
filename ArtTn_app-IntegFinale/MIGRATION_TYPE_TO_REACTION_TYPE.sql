-- ============================================
-- MIGRATION: type → reaction_type
-- Correction du conflit entre Symfony et Java
-- ============================================

-- PROBLÈME:
-- - Symfony utilise la colonne 'reaction_type'
-- - Java utilisait la colonne 'type'
-- - Cela créait une confusion dans les compteurs

-- SOLUTION:
-- - Java utilise maintenant 'reaction_type' (comme Symfony)
-- - Ce script migre les données existantes

-- ============================================
-- ÉTAPE 1: DIAGNOSTIC
-- ============================================

-- Vérifier la structure de la table
DESCRIBE post_reactions;

-- Vérifier les données dans les deux colonnes
SELECT 
    'Réactions avec reaction_type' as description,
    COUNT(*) as count
FROM post_reactions
WHERE reaction_type IS NOT NULL AND reaction_type != ''
UNION ALL
SELECT 
    'Réactions avec type',
    COUNT(*)
FROM post_reactions
WHERE type IS NOT NULL AND type != ''
UNION ALL
SELECT 
    'Réactions avec les deux',
    COUNT(*)
FROM post_reactions
WHERE (reaction_type IS NOT NULL AND reaction_type != '')
  AND (type IS NOT NULL AND type != '')
UNION ALL
SELECT 
    'Réactions sans aucune',
    COUNT(*)
FROM post_reactions
WHERE (reaction_type IS NULL OR reaction_type = '')
  AND (type IS NULL OR type = '');

-- Afficher les incohérences
SELECT 
    id,
    post_id,
    user_id,
    reaction_type,
    type,
    created_at,
    CASE 
        WHEN reaction_type = type THEN '✅ COHÉRENT'
        WHEN (reaction_type IS NULL OR reaction_type = '') AND (type IS NOT NULL AND type != '') THEN '⚠️ Seulement type'
        WHEN (type IS NULL OR type = '') AND (reaction_type IS NOT NULL AND reaction_type != '') THEN '⚠️ Seulement reaction_type'
        WHEN reaction_type != type THEN '❌ DIFFÉRENT'
        ELSE '❓ AUTRE'
    END as status
FROM post_reactions
ORDER BY id DESC;

-- ============================================
-- ÉTAPE 2: MIGRATION
-- ============================================

-- Copier les données de 'type' vers 'reaction_type' si reaction_type est vide
UPDATE post_reactions 
SET reaction_type = type 
WHERE (reaction_type IS NULL OR reaction_type = '') 
  AND (type IS NOT NULL AND type != '');

-- Vérifier le résultat
SELECT 
    'Après migration - Réactions avec reaction_type' as description,
    COUNT(*) as count
FROM post_reactions
WHERE reaction_type IS NOT NULL AND reaction_type != '';

-- ============================================
-- ÉTAPE 3: RECALCULER LES COMPTEURS
-- ============================================

-- Recalculer les likes (utiliser reaction_type)
UPDATE post p
SET likes = (
    SELECT COUNT(*) 
    FROM post_reactions 
    WHERE post_id = p.post_id AND reaction_type = 'LIKE'
);

-- Recalculer les dislikes (utiliser reaction_type)
UPDATE post p
SET dislikes = (
    SELECT COUNT(*) 
    FROM post_reactions 
    WHERE post_id = p.post_id AND reaction_type = 'DISLIKE'
);

-- Vérifier la cohérence
SELECT 
    p.post_id,
    p.post_titre,
    p.likes as likes_in_post,
    (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'LIKE') as actual_likes,
    p.dislikes as dislikes_in_post,
    (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'DISLIKE') as actual_dislikes,
    CASE 
        WHEN p.likes = (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'LIKE')
        AND p.dislikes = (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND reaction_type = 'DISLIKE')
        THEN '✅ OK'
        ELSE '❌ INCOHÉRENT'
    END as status
FROM post p
ORDER BY p.post_id;

-- ============================================
-- ÉTAPE 4: VÉRIFICATION FINALE
-- ============================================

-- Afficher toutes les réactions avec leur statut
SELECT 
    pr.id,
    pr.post_id,
    p.post_titre,
    pr.user_id,
    u.full_name,
    pr.reaction_type,
    pr.type,
    pr.created_at,
    CASE 
        WHEN pr.reaction_type = pr.type THEN '✅'
        WHEN pr.reaction_type IS NOT NULL AND pr.reaction_type != '' THEN '✅'
        ELSE '❌'
    END as status
FROM post_reactions pr
LEFT JOIN post p ON pr.post_id = p.post_id
LEFT JOIN users u ON pr.user_id = u.id
ORDER BY pr.created_at DESC
LIMIT 50;

-- Statistiques finales
SELECT 
    'Total réactions' as metric,
    COUNT(*) as value
FROM post_reactions
UNION ALL
SELECT 
    'Réactions LIKE (reaction_type)',
    COUNT(*)
FROM post_reactions
WHERE reaction_type = 'LIKE'
UNION ALL
SELECT 
    'Réactions DISLIKE (reaction_type)',
    COUNT(*)
FROM post_reactions
WHERE reaction_type = 'DISLIKE'
UNION ALL
SELECT 
    'Total likes dans post',
    SUM(likes)
FROM post
UNION ALL
SELECT 
    'Total dislikes dans post',
    SUM(dislikes)
FROM post;

-- ============================================
-- ÉTAPE 5: NETTOYAGE (OPTIONNEL)
-- ============================================

-- ATTENTION: Ne pas exécuter si vous n'êtes pas sûr!
-- Cette étape supprime la colonne 'type' après migration

-- Vérifier que toutes les données sont dans reaction_type
SELECT 
    COUNT(*) as reactions_sans_reaction_type
FROM post_reactions
WHERE reaction_type IS NULL OR reaction_type = '';

-- Si le résultat est 0, vous pouvez supprimer la colonne 'type'
-- DÉCOMMENTER SEULEMENT APRÈS VALIDATION COMPLÈTE:
-- ALTER TABLE post_reactions DROP COLUMN type;

-- ============================================
-- RAPPORT FINAL
-- ============================================

SELECT '========================================' as '';
SELECT 'RAPPORT DE MIGRATION' as '';
SELECT '========================================' as '';

SELECT 
    'Total posts' as metric,
    COUNT(*) as value
FROM post
UNION ALL
SELECT 
    'Total réactions',
    COUNT(*)
FROM post_reactions
UNION ALL
SELECT 
    'Réactions avec reaction_type',
    COUNT(*)
FROM post_reactions
WHERE reaction_type IS NOT NULL AND reaction_type != ''
UNION ALL
SELECT 
    'Réactions LIKE',
    COUNT(*)
FROM post_reactions
WHERE reaction_type = 'LIKE'
UNION ALL
SELECT 
    'Réactions DISLIKE',
    COUNT(*)
FROM post_reactions
WHERE reaction_type = 'DISLIKE'
UNION ALL
SELECT 
    'Total likes (compteur)',
    SUM(likes)
FROM post
UNION ALL
SELECT 
    'Total dislikes (compteur)',
    SUM(dislikes)
FROM post;

SELECT '========================================' as '';
SELECT 'MIGRATION TERMINÉE' as '';
SELECT '========================================' as '';

-- ============================================
-- FIN DU SCRIPT
-- ============================================
