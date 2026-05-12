# 🔧 CORRECTION: Conflit type vs reaction_type

## 🐛 PROBLÈME IDENTIFIÉ

### **Situation:**
La table `post_reactions` a **deux colonnes** pour stocker le type de réaction:
- `reaction_type` (utilisée par **Symfony**)
- `type` (utilisée par **Java**)

### **Conséquence:**
- Les réactions créées par Symfony vont dans `reaction_type`
- Les réactions créées par Java allaient dans `type`
- Les compteurs de likes/dislikes étaient **incohérents**
- Confusion totale dans les données

### **Exemple du problème:**
```
| id | post_id | user_id | reaction_type | type    | created_at          |
|----|---------|---------|---------------|---------|---------------------|
| 5  | 1       | 66      | DISLIKE       | LIKE    | 2026-05-09 01:10:35 |
| 6  | 1       | 3       | LIKE          | LIKE    | 2026-05-09 11:27:47 |
| 19 | 5       | 60      | LIKE          | LIKE    | 2026-05-09 13:01:12 |
| 43 | 6       | 60      | LIKE          | DISLIKE | 2026-05-09 13:01:46 |
```

**Ligne 5:** reaction_type = DISLIKE mais type = LIKE → **INCOHÉRENT!**
**Ligne 43:** reaction_type = LIKE mais type = DISLIKE → **INCOHÉRENT!**

---

## ✅ SOLUTION APPLIQUÉE

### **Décision:**
Java utilise maintenant **`reaction_type`** (comme Symfony) au lieu de `type`.

### **Avantages:**
- ✅ Une seule source de vérité
- ✅ Compatibilité totale avec Symfony
- ✅ Compteurs cohérents
- ✅ Pas de confusion

---

## 📝 MODIFICATIONS EFFECTUÉES

### **1. PostServices.java**

#### **Avant:**
```java
// Récupérer la réaction
String sql = "SELECT type FROM post_reactions WHERE post_id = ? AND user_id = ?";
ResultSet rs = pst.executeQuery();
if (rs.next()) {
    return rs.getString("type");  // ❌ Mauvaise colonne
}

// Insérer une réaction
String insertSql = "INSERT INTO post_reactions (post_id, user_id, type) VALUES (?, ?, ?)";
```

#### **Après:**
```java
// Récupérer la réaction
String sql = "SELECT reaction_type FROM post_reactions WHERE post_id = ? AND user_id = ?";
ResultSet rs = pst.executeQuery();
if (rs.next()) {
    return rs.getString("reaction_type");  // ✅ Bonne colonne
}

// Insérer une réaction
String insertSql = "INSERT INTO post_reactions (post_id, user_id, reaction_type) VALUES (?, ?, ?)";
```

### **2. Fichiers modifiés:**
- `PostServices.java`:
  - Méthode `getUserReaction()` - ligne ~218
  - Méthode `addReaction()` - ligne ~268

---

## 🔄 MIGRATION DES DONNÉES

### **Script SQL créé:**
`MIGRATION_TYPE_TO_REACTION_TYPE.sql`

### **Ce que fait le script:**

1. **Diagnostic:**
   - Compte les réactions dans `reaction_type`
   - Compte les réactions dans `type`
   - Identifie les incohérences

2. **Migration:**
   ```sql
   -- Copier les données de 'type' vers 'reaction_type' si vide
   UPDATE post_reactions 
   SET reaction_type = type 
   WHERE (reaction_type IS NULL OR reaction_type = '') 
     AND (type IS NOT NULL AND type != '');
   ```

3. **Recalcul des compteurs:**
   ```sql
   -- Recalculer les likes
   UPDATE post p
   SET likes = (
       SELECT COUNT(*) 
       FROM post_reactions 
       WHERE post_id = p.post_id AND reaction_type = 'LIKE'
   );
   
   -- Recalculer les dislikes
   UPDATE post p
   SET dislikes = (
       SELECT COUNT(*) 
       FROM post_reactions 
       WHERE post_id = p.post_id AND reaction_type = 'DISLIKE'
   );
   ```

4. **Vérification:**
   - Compare les compteurs avec les données réelles
   - Affiche les incohérences restantes

---

## 🧪 COMMENT APPLIQUER LA CORRECTION

### **Étape 1: Exécuter la migration SQL**

```bash
# Se connecter à MySQL
mysql -u root -p museum_db

# Exécuter le script de migration
source MIGRATION_TYPE_TO_REACTION_TYPE.sql

# OU en une seule commande:
mysql -u root -p museum_db < MIGRATION_TYPE_TO_REACTION_TYPE.sql
```

### **Étape 2: Vérifier les résultats**

Le script affiche automatiquement:
- Nombre de réactions migrées
- Compteurs recalculés
- Incohérences restantes (devrait être 0)

### **Étape 3: Compiler et tester l'application Java**

```bash
# Compiler
mvn clean compile

# Lancer
mvn javafx:run
```

### **Étape 4: Tester les réactions**

1. Ouvrir un post
2. Cliquer sur Like → Vérifier que ça fonctionne
3. Cliquer sur Dislike → Vérifier le switch
4. Vérifier en base de données:
   ```sql
   SELECT * FROM post_reactions ORDER BY created_at DESC LIMIT 10;
   ```
5. Vérifier que `reaction_type` est rempli (pas `type`)

---

## 📊 VÉRIFICATION DE LA COHÉRENCE

### **Requête de vérification:**

```sql
SELECT 
    p.post_id,
    p.post_titre,
    p.likes as likes_compteur,
    (SELECT COUNT(*) FROM post_reactions 
     WHERE post_id = p.post_id AND reaction_type = 'LIKE') as likes_reel,
    p.dislikes as dislikes_compteur,
    (SELECT COUNT(*) FROM post_reactions 
     WHERE post_id = p.post_id AND reaction_type = 'DISLIKE') as dislikes_reel,
    CASE 
        WHEN p.likes = (SELECT COUNT(*) FROM post_reactions 
                        WHERE post_id = p.post_id AND reaction_type = 'LIKE')
        AND p.dislikes = (SELECT COUNT(*) FROM post_reactions 
                          WHERE post_id = p.post_id AND reaction_type = 'DISLIKE')
        THEN '✅ OK'
        ELSE '❌ INCOHÉRENT'
    END as status
FROM post p
ORDER BY p.post_id;
```

**Résultat attendu:** Tous les posts doivent avoir le status `✅ OK`

---

## 🎯 AVANT / APRÈS

### **AVANT (Incohérent):**

**Base de données:**
```
post_reactions:
| id | post_id | user_id | reaction_type | type    |
|----|---------|---------|---------------|---------|
| 1  | 1       | 66      | DISLIKE       | LIKE    | ❌
| 2  | 1       | 3       | LIKE          | LIKE    | ✅
| 3  | 5       | 60      | LIKE          | DISLIKE | ❌
```

**Compteurs:**
```
post:
| post_id | likes | dislikes |
|---------|-------|----------|
| 1       | 2     | 1        | ❌ Faux!
```

**Problème:** Java comptait avec `type`, Symfony avec `reaction_type`

---

### **APRÈS (Cohérent):**

**Base de données:**
```
post_reactions:
| id | post_id | user_id | reaction_type | type    |
|----|---------|---------|---------------|---------|
| 1  | 1       | 66      | DISLIKE       | DISLIKE | ✅
| 2  | 1       | 3       | LIKE          | LIKE    | ✅
| 3  | 5       | 60      | LIKE          | LIKE    | ✅
```

**Compteurs:**
```
post:
| post_id | likes | dislikes |
|---------|-------|----------|
| 1       | 1     | 1        | ✅ Correct!
```

**Solution:** Java et Symfony utilisent tous les deux `reaction_type`

---

## 🔍 DIAGNOSTIC DES PROBLÈMES

### **Problème 1: Les compteurs ne correspondent pas**

**Symptôme:**
```sql
SELECT post_id, likes, dislikes FROM post WHERE post_id = 1;
-- Résultat: likes = 5, dislikes = 2

SELECT COUNT(*) FROM post_reactions 
WHERE post_id = 1 AND reaction_type = 'LIKE';
-- Résultat: 3  ❌ Pas 5!
```

**Solution:**
```bash
mysql -u root -p museum_db < MIGRATION_TYPE_TO_REACTION_TYPE.sql
```

---

### **Problème 2: Les réactions ne s'enregistrent pas**

**Symptôme:**
- Cliquer sur Like ne fait rien
- Erreur dans la console Java

**Cause possible:**
- La colonne `reaction_type` n'existe pas dans la table

**Solution:**
```sql
-- Vérifier la structure
DESCRIBE post_reactions;

-- Si reaction_type n'existe pas, l'ajouter:
ALTER TABLE post_reactions 
ADD COLUMN reaction_type VARCHAR(10) AFTER user_id;
```

---

### **Problème 3: Doublons de réactions**

**Symptôme:**
```sql
SELECT post_id, user_id, COUNT(*) as count
FROM post_reactions
GROUP BY post_id, user_id
HAVING count > 1;
-- Résultat: Plusieurs lignes  ❌
```

**Solution:**
```sql
-- Supprimer les doublons (garder le plus récent)
DELETE pr1 FROM post_reactions pr1
INNER JOIN post_reactions pr2 
WHERE pr1.id < pr2.id 
AND pr1.post_id = pr2.post_id 
AND pr1.user_id = pr2.user_id;
```

---

## 📚 FICHIERS CRÉÉS/MODIFIÉS

### **Code Java:**
1. `PostServices.java` - Utilise maintenant `reaction_type`

### **Scripts SQL:**
2. `MIGRATION_TYPE_TO_REACTION_TYPE.sql` - Script de migration complet
3. `TEST_AND_FIX_REACTIONS.sql` - Mis à jour pour utiliser `reaction_type`

### **Documentation:**
4. `CORRECTION_CONFLIT_TYPE_REACTION_TYPE.md` - Ce fichier

---

## ✅ CHECKLIST DE VALIDATION

### **Avant la migration:**
- [ ] Sauvegarder la base de données
- [ ] Vérifier que Symfony fonctionne
- [ ] Noter les compteurs actuels

### **Pendant la migration:**
- [ ] Exécuter `MIGRATION_TYPE_TO_REACTION_TYPE.sql`
- [ ] Vérifier qu'il n'y a pas d'erreurs SQL
- [ ] Vérifier les compteurs recalculés

### **Après la migration:**
- [ ] Compiler le projet Java
- [ ] Tester les likes/dislikes dans Java
- [ ] Tester les likes/dislikes dans Symfony
- [ ] Vérifier la cohérence en BDD
- [ ] Vérifier qu'il n'y a pas de doublons

### **Validation finale:**
- [ ] Tous les compteurs sont cohérents
- [ ] Les réactions fonctionnent dans Java
- [ ] Les réactions fonctionnent dans Symfony
- [ ] Pas d'erreurs dans les logs
- [ ] Les utilisateurs peuvent liker/disliker normalement

---

## 🎓 EXPLICATION TECHNIQUE

### **Pourquoi deux colonnes?**

Probablement:
1. Symfony a créé la table avec `reaction_type`
2. Java a été développé séparément et a utilisé `type`
3. Personne n'a remarqué le conflit jusqu'à maintenant

### **Pourquoi garder reaction_type?**

- ✅ Nom plus explicite (`reaction_type` vs `type`)
- ✅ Symfony l'utilise déjà (plus ancien)
- ✅ Évite de casser Symfony
- ✅ Standard de nommage plus clair

### **Peut-on supprimer la colonne 'type'?**

**OUI**, mais seulement après:
1. Migration complète des données
2. Validation que tout fonctionne
3. Vérification que Symfony n'utilise pas `type`

**Commande (à exécuter avec précaution):**
```sql
ALTER TABLE post_reactions DROP COLUMN type;
```

---

## 📞 SUPPORT

### **En cas de problème:**

1. **Vérifier les logs Java:**
   - Chercher "Erreur addReaction"
   - Chercher "SQLException"

2. **Vérifier la structure de la table:**
   ```sql
   DESCRIBE post_reactions;
   ```

3. **Vérifier les données:**
   ```sql
   SELECT * FROM post_reactions ORDER BY created_at DESC LIMIT 10;
   ```

4. **Recalculer les compteurs:**
   ```bash
   mysql -u root -p museum_db < MIGRATION_TYPE_TO_REACTION_TYPE.sql
   ```

---

## 🎯 RÉSUMÉ

**Problème:** Conflit entre `type` (Java) et `reaction_type` (Symfony)

**Solution:** Java utilise maintenant `reaction_type`

**Actions:**
1. ✅ Code Java modifié
2. ✅ Script de migration créé
3. ✅ Documentation créée

**Prochaines étapes:**
1. Exécuter la migration SQL
2. Tester l'application
3. Valider la cohérence

---

**Date:** 09/05/2026  
**Développeur:** Kiro AI Assistant  
**Status:** ✅ CORRECTION APPLIQUÉE - MIGRATION À EXÉCUTER
