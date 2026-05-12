-- ============================================
-- FIX POUR LA TABLE post_reactions
-- ============================================

-- Option 1: Si la table post_reactions existe déjà mais sans la colonne 'type'
-- Ajouter la colonne 'type'
ALTER TABLE post_reactions 
ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'LIKE';

-- ============================================
-- Option 2: Si la table n'existe pas du tout
-- Créer la table complète
-- ============================================

-- Supprimer la table si elle existe (ATTENTION: perte de données)
-- DROP TABLE IF EXISTS post_reactions;

-- Créer la table post_reactions
CREATE TABLE IF NOT EXISTS post_reactions (
    reaction_id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('LIKE', 'DISLIKE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_reaction (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

-- ============================================
-- Option 3: Vérifier la structure actuelle
-- ============================================

-- Exécuter cette requête pour voir la structure actuelle
DESCRIBE post_reactions;

-- Ou
SHOW COLUMNS FROM post_reactions;

-- ============================================
-- INSTRUCTIONS D'UTILISATION
-- ============================================

/*
1. Ouvrir phpMyAdmin ou votre client MySQL
2. Sélectionner votre base de données
3. Exécuter d'abord: DESCRIBE post_reactions;
4. Si la colonne 'type' n'existe pas, exécuter l'Option 1
5. Si la table n'existe pas du tout, exécuter l'Option 2
6. Tester l'application
*/

-- ============================================
-- VÉRIFICATION APRÈS CORRECTION
-- ============================================

-- Vérifier que la table est correcte
SELECT * FROM post_reactions LIMIT 5;

-- Vérifier la structure
SHOW CREATE TABLE post_reactions;
