-- ============================================
-- FIX RAPIDE - Likes et Dislikes
-- ============================================
-- Copier-coller ce script dans phpMyAdmin
-- ============================================

-- Étape 1: Ajouter la colonne 'type' manquante
ALTER TABLE post_reactions 
ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'LIKE';

-- Étape 2: Ajouter la contrainte UNIQUE (éviter les doublons)
ALTER TABLE post_reactions 
ADD UNIQUE KEY unique_reaction (post_id, user_id);

-- Étape 3: Vérifier que tout est OK
DESCRIBE post_reactions;

-- ============================================
-- C'EST TOUT ! Relancez votre application
-- ============================================
