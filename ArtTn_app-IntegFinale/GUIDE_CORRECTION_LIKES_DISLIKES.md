# 🔧 Guide de Correction - Likes et Dislikes

## 🐛 Problème Identifié

**Erreur :** `Unknown column 'type' in 'field list'`

**Cause :** La table `post_reactions` dans votre base de données n'a pas de colonne `type`.

**Impact :** Les likes et dislikes ne sont pas sauvegardés.

---

## 🔍 Diagnostic

### Erreurs dans les Logs
```
Erreur: Unknown column 'type' in 'field list'
Erreur addReaction: Unknown column 'type' in 'field list'
like and dislike are not being saved
```

### Code Concerné
Le code dans `PostServices.java` essaie d'insérer/lire une colonne `type` :

```java
// Ligne 205 - Lecture
String sql = "SELECT type FROM post_reactions WHERE post_id = ? AND user_id = ?";

// Ligne 269 - Insertion
String insertSql = "INSERT INTO post_reactions (post_id, user_id, type) VALUES (?, ?, ?)";
```

---

## ✅ Solution

### Étape 1 : Vérifier la Structure Actuelle

1. Ouvrir **phpMyAdmin** (ou votre client MySQL)
2. Sélectionner votre base de données
3. Exécuter cette requête :

```sql
DESCRIBE post_reactions;
```

ou

```sql
SHOW COLUMNS FROM post_reactions;
```

### Résultat Attendu

Vous devriez voir quelque chose comme :

```
+-------------+-------------+------+-----+---------+----------------+
| Field       | Type        | Null | Key | Default | Extra          |
+-------------+-------------+------+-----+---------+----------------+
| reaction_id | int(11)     | NO   | PRI | NULL    | auto_increment |
| post_id     | int(11)     | NO   | MUL | NULL    |                |
| user_id     | int(11)     | NO   | MUL | NULL    |                |
+-------------+-------------+------+-----+---------+----------------+
```

**❌ Problème :** La colonne `type` est manquante !

---

### Étape 2 : Corriger la Table

#### Option A : Ajouter la Colonne Manquante (RECOMMANDÉ)

Si la table existe déjà avec des données :

```sql
ALTER TABLE post_reactions 
ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'LIKE';
```

Cette commande :
- ✅ Ajoute la colonne `type`
- ✅ Conserve les données existantes
- ✅ Met 'LIKE' par défaut pour les anciennes réactions

#### Option B : Recréer la Table Complète

Si la table est vide ou si vous voulez repartir de zéro :

```sql
-- Supprimer l'ancienne table (ATTENTION: perte de données)
DROP TABLE IF EXISTS post_reactions;

-- Créer la nouvelle table
CREATE TABLE post_reactions (
    reaction_id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('LIKE', 'DISLIKE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_reaction (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);
```

---

### Étape 3 : Vérifier la Correction

Exécuter cette requête pour vérifier :

```sql
DESCRIBE post_reactions;
```

**✅ Résultat Attendu :**

```
+-------------+-------------+------+-----+---------+----------------+
| Field       | Type        | Null | Key | Default | Extra          |
+-------------+-------------+------+-----+---------+----------------+
| reaction_id | int(11)     | NO   | PRI | NULL    | auto_increment |
| post_id     | int(11)     | NO   | MUL | NULL    |                |
| user_id     | int(11)     | NO   | MUL | NULL    |                |
| type        | varchar(10) | NO   |     | LIKE    |                |
| created_at  | timestamp   | YES  |     | CURRENT |                |
+-------------+-------------+------+-----+---------+----------------+
```

---

### Étape 4 : Tester l'Application

1. **Relancer l'application**
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

2. **Naviguer vers un post**
   - Aller dans le Forum
   - Ouvrir un post

3. **Tester les likes/dislikes**
   - Cliquer sur 👍 (Like)
   - Vérifier que le compteur augmente
   - Cliquer à nouveau → Le compteur diminue (toggle)
   - Cliquer sur 👎 (Dislike)
   - Vérifier que le compteur augmente
   - Le like devrait diminuer automatiquement

4. **Vérifier dans la base de données**
   ```sql
   SELECT * FROM post_reactions;
   ```

   **✅ Résultat Attendu :**
   ```
   +-------------+---------+---------+---------+---------------------+
   | reaction_id | post_id | user_id | type    | created_at          |
   +-------------+---------+---------+---------+---------------------+
   | 1           | 4       | 60      | LIKE    | 2026-05-09 14:30:00 |
   | 2           | 5       | 60      | DISLIKE | 2026-05-09 14:31:00 |
   +-------------+---------+---------+---------+---------------------+
   ```

---

## 🎯 Vérifications Finales

### Dans les Logs (Console)

**Avant la correction ❌**
```
Erreur: Unknown column 'type' in 'field list'
Erreur addReaction: Unknown column 'type' in 'field list'
```

**Après la correction ✅**
```
✅ Réaction LIKE enregistrée pour user 60
✅ Réaction DISLIKE enregistrée pour user 60
✅ Réaction LIKE retirée pour user 60
```

### Dans l'Interface

**Comportement Attendu :**

1. **Premier clic sur 👍**
   - Compteur likes : 0 → 1
   - Bouton devient actif (coloré)

2. **Deuxième clic sur 👍**
   - Compteur likes : 1 → 0
   - Bouton redevient inactif

3. **Clic sur 👎 après avoir liké**
   - Compteur likes : 1 → 0
   - Compteur dislikes : 0 → 1
   - Bouton like inactif, bouton dislike actif

4. **Rafraîchir la page**
   - Les réactions sont conservées
   - Les boutons affichent le bon état

---

## 📊 Structure Complète de la Table

### Colonnes

| Colonne      | Type         | Description                          |
|--------------|--------------|--------------------------------------|
| reaction_id  | INT          | ID unique (auto-increment)           |
| post_id      | INT          | ID du post                           |
| user_id      | INT          | ID de l'utilisateur                  |
| type         | VARCHAR(10)  | Type de réaction (LIKE ou DISLIKE)   |
| created_at   | TIMESTAMP    | Date de création                     |

### Contraintes

- **PRIMARY KEY** : `reaction_id`
- **UNIQUE KEY** : `(post_id, user_id)` → Un utilisateur ne peut avoir qu'une seule réaction par post
- **FOREIGN KEY** : `post_id` → `post(post_id)` ON DELETE CASCADE
- **FOREIGN KEY** : `user_id` → `user(user_id)` ON DELETE CASCADE
- **CHECK** : `type IN ('LIKE', 'DISLIKE')`

---

## 🔄 Flux de Fonctionnement

### Scénario 1 : Premier Like

```
1. Utilisateur clique sur 👍
2. getUserReaction(postId, userId) → NULL (pas de réaction)
3. INSERT INTO post_reactions (post_id, user_id, type) VALUES (4, 60, 'LIKE')
4. UPDATE post SET likes = likes + 1 WHERE post_id = 4
5. Affichage : 👍 1 (bouton actif)
```

### Scénario 2 : Toggle (Retirer le Like)

```
1. Utilisateur clique sur 👍 (déjà liké)
2. getUserReaction(postId, userId) → 'LIKE'
3. DELETE FROM post_reactions WHERE post_id = 4 AND user_id = 60
4. UPDATE post SET likes = likes - 1 WHERE post_id = 4
5. Affichage : 👍 0 (bouton inactif)
```

### Scénario 3 : Changer de Like à Dislike

```
1. Utilisateur clique sur 👎 (déjà liké)
2. getUserReaction(postId, userId) → 'LIKE'
3. DELETE FROM post_reactions WHERE post_id = 4 AND user_id = 60
4. UPDATE post SET likes = likes - 1 WHERE post_id = 4
5. INSERT INTO post_reactions (post_id, user_id, type) VALUES (4, 60, 'DISLIKE')
6. UPDATE post SET dislikes = dislikes + 1 WHERE post_id = 4
7. Affichage : 👍 0 (inactif), 👎 1 (actif)
```

---

## 🐛 Problèmes Potentiels et Solutions

### Problème 1 : Erreur de Clé Étrangère

**Erreur :**
```
Cannot add foreign key constraint
```

**Solution :**
Vérifier que les tables `post` et `user` existent et ont les bonnes colonnes :

```sql
-- Vérifier la table post
DESCRIBE post;

-- Vérifier la table user
DESCRIBE user;
```

Si les colonnes n'existent pas, créer la table sans les contraintes de clé étrangère :

```sql
CREATE TABLE post_reactions (
    reaction_id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_reaction (post_id, user_id)
);
```

---

### Problème 2 : Doublons de Réactions

**Symptôme :** Un utilisateur peut liker plusieurs fois le même post

**Solution :** Ajouter la contrainte UNIQUE :

```sql
ALTER TABLE post_reactions 
ADD UNIQUE KEY unique_reaction (post_id, user_id);
```

---

### Problème 3 : Les Compteurs ne se Mettent pas à Jour

**Vérification :**

```sql
-- Vérifier les compteurs dans la table post
SELECT post_id, post_titre, likes, dislikes FROM post;

-- Compter les réactions réelles
SELECT 
    post_id,
    SUM(CASE WHEN type = 'LIKE' THEN 1 ELSE 0 END) as real_likes,
    SUM(CASE WHEN type = 'DISLIKE' THEN 1 ELSE 0 END) as real_dislikes
FROM post_reactions
GROUP BY post_id;
```

**Solution :** Recalculer les compteurs :

```sql
-- Réinitialiser tous les compteurs
UPDATE post SET likes = 0, dislikes = 0;

-- Recalculer à partir des réactions
UPDATE post p
SET 
    likes = (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND type = 'LIKE'),
    dislikes = (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND type = 'DISLIKE');
```

---

## 📝 Checklist de Vérification

Avant de considérer le problème résolu :

- [ ] La colonne `type` existe dans `post_reactions`
- [ ] La contrainte UNIQUE sur `(post_id, user_id)` existe
- [ ] Les clés étrangères sont définies (optionnel)
- [ ] L'application se lance sans erreur
- [ ] Cliquer sur 👍 augmente le compteur
- [ ] Cliquer à nouveau sur 👍 diminue le compteur
- [ ] Cliquer sur 👎 après 👍 change la réaction
- [ ] Les réactions sont conservées après rafraîchissement
- [ ] Aucune erreur dans les logs
- [ ] Les données sont bien dans la base de données

---

## 📞 Support

### Si le Problème Persiste

1. **Vérifier les logs complets**
   ```java
   System.out.println("Debug - Post ID: " + postId);
   System.out.println("Debug - User ID: " + userId);
   System.out.println("Debug - Type: " + type);
   ```

2. **Vérifier la connexion à la base**
   ```sql
   SELECT DATABASE();
   ```

3. **Vérifier les permissions**
   ```sql
   SHOW GRANTS;
   ```

4. **Exporter la structure de la table**
   ```sql
   SHOW CREATE TABLE post_reactions;
   ```

---

**✅ Une fois corrigé, les likes et dislikes fonctionneront parfaitement !**

---

**Version** : 1.0  
**Date** : Mai 2026  
**Auteur** : Équipe de Développement ArtTn
