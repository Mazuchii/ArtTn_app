# 🧪 TEST LIKE/DISLIKE - GUIDE DE TEST MANUEL

## ✅ Corrections Appliquées

### 1. **Design des Commentaires**
- ✅ Avatar circulaire avec icône utilisateur
- ✅ Nom réel de l'utilisateur (au lieu de "Auteur #ID")
- ✅ Carte blanche avec ombre subtile
- ✅ Boutons d'action avec hover effects
- ✅ Séparateur visuel entre contenu et actions
- ✅ Espacement et padding améliorés

### 2. **Affichage des Noms d'Utilisateurs**
- ✅ Post: Affiche le nom complet de l'auteur (via UserService)
- ✅ Commentaires: Affiche le nom complet de l'auteur (via UserService)
- ✅ Fallback sur username si full_name est vide
- ✅ Fallback sur "Utilisateur #ID" si erreur

### 3. **Système Like/Dislike**
Le système utilise une logique de toggle avec base de données:

#### **Comportement Attendu:**

1. **Premier clic sur Like:**
   - ✅ Ajoute un LIKE dans `post_reactions`
   - ✅ Incrémente `likes` dans la table `post`
   - ✅ Bouton Like devient BLEU et BOLD
   - ✅ Message: "👍 Vous avez aimé ce post !"

2. **Deuxième clic sur Like (toggle off):**
   - ✅ Supprime le LIKE de `post_reactions`
   - ✅ Décrémente `likes` dans la table `post`
   - ✅ Bouton Like redevient GRIS
   - ✅ Message: "👍 Like retiré"

3. **Clic sur Dislike après Like:**
   - ✅ Supprime le LIKE de `post_reactions`
   - ✅ Décrémente `likes`
   - ✅ Ajoute un DISLIKE dans `post_reactions`
   - ✅ Incrémente `dislikes`
   - ✅ Bouton Like devient GRIS
   - ✅ Bouton Dislike devient ROUGE et BOLD
   - ✅ Message: "👎 Vous n'avez pas aimé ce post !"

4. **Clic sur Like après Dislike:**
   - ✅ Supprime le DISLIKE de `post_reactions`
   - ✅ Décrémente `dislikes`
   - ✅ Ajoute un LIKE dans `post_reactions`
   - ✅ Incrémente `likes`
   - ✅ Bouton Dislike devient GRIS
   - ✅ Bouton Like devient BLEU et BOLD
   - ✅ Message: "👍 Vous avez aimé ce post !"

---

## 🧪 PROCÉDURE DE TEST

### **Étape 1: Vérifier la Base de Données**

Exécutez ces requêtes SQL pour vérifier la structure:

```sql
-- Vérifier la table post_reactions
DESCRIBE post_reactions;

-- Vérifier les contraintes
SHOW CREATE TABLE post_reactions;

-- Vérifier les données existantes
SELECT * FROM post_reactions ORDER BY created_at DESC LIMIT 10;

-- Vérifier les compteurs d'un post spécifique
SELECT post_id, likes, dislikes FROM post WHERE post_id = 1;
```

### **Étape 2: Test Manuel dans l'Application**

1. **Lancer l'application**
   ```bash
   mvn clean javafx:run
   ```

2. **Se connecter avec un utilisateur**

3. **Naviguer vers le Forum**

4. **Ouvrir un post**

5. **Tester les scénarios suivants:**

#### **Scénario 1: Like Simple**
- [ ] Cliquer sur 👍
- [ ] Vérifier que le compteur de likes augmente de 1
- [ ] Vérifier que le bouton Like devient BLEU
- [ ] Vérifier le message de confirmation

#### **Scénario 2: Toggle Like**
- [ ] Cliquer à nouveau sur 👍
- [ ] Vérifier que le compteur de likes diminue de 1
- [ ] Vérifier que le bouton Like redevient GRIS
- [ ] Vérifier le message "Like retiré"

#### **Scénario 3: Dislike Simple**
- [ ] Cliquer sur 👎
- [ ] Vérifier que le compteur de dislikes augmente de 1
- [ ] Vérifier que le bouton Dislike devient ROUGE
- [ ] Vérifier le message de confirmation

#### **Scénario 4: Toggle Dislike**
- [ ] Cliquer à nouveau sur 👎
- [ ] Vérifier que le compteur de dislikes diminue de 1
- [ ] Vérifier que le bouton Dislike redevient GRIS
- [ ] Vérifier le message "Dislike retiré"

#### **Scénario 5: Switch Like → Dislike**
- [ ] Cliquer sur 👍 (Like actif)
- [ ] Cliquer sur 👎
- [ ] Vérifier que likes diminue de 1
- [ ] Vérifier que dislikes augmente de 1
- [ ] Vérifier que Like devient GRIS et Dislike devient ROUGE

#### **Scénario 6: Switch Dislike → Like**
- [ ] Cliquer sur 👎 (Dislike actif)
- [ ] Cliquer sur 👍
- [ ] Vérifier que dislikes diminue de 1
- [ ] Vérifier que likes augmente de 1
- [ ] Vérifier que Dislike devient GRIS et Like devient BLEU

#### **Scénario 7: Persistance**
- [ ] Liker un post
- [ ] Fermer la fenêtre du post
- [ ] Rouvrir le même post
- [ ] Vérifier que le Like est toujours actif (bouton BLEU)

#### **Scénario 8: Multi-utilisateurs**
- [ ] Se connecter avec User A
- [ ] Liker un post
- [ ] Se déconnecter
- [ ] Se connecter avec User B
- [ ] Ouvrir le même post
- [ ] Vérifier que le compteur de likes est correct
- [ ] Vérifier que User B n'a pas de réaction active

---

## 🔍 VÉRIFICATION EN BASE DE DONNÉES

Après chaque test, vérifiez la cohérence:

```sql
-- Vérifier les réactions d'un utilisateur
SELECT pr.*, p.post_titre, p.likes, p.dislikes
FROM post_reactions pr
JOIN post p ON pr.post_id = p.post_id
WHERE pr.user_id = 1;

-- Vérifier qu'il n'y a qu'une seule réaction par utilisateur par post
SELECT post_id, user_id, COUNT(*) as count
FROM post_reactions
GROUP BY post_id, user_id
HAVING count > 1;

-- Vérifier la cohérence des compteurs
SELECT 
    p.post_id,
    p.likes as likes_in_post,
    (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND type = 'LIKE') as actual_likes,
    p.dislikes as dislikes_in_post,
    (SELECT COUNT(*) FROM post_reactions WHERE post_id = p.post_id AND type = 'DISLIKE') as actual_dislikes
FROM post p
WHERE p.post_id = 1;
```

---

## 🐛 PROBLÈMES CONNUS ET SOLUTIONS

### **Problème 1: Les compteurs ne se mettent pas à jour**
**Solution:** Vérifier que `refreshPosts()` est appelé dans le parent controller

### **Problème 2: Plusieurs réactions pour le même utilisateur**
**Solution:** La contrainte UNIQUE dans `post_reactions` empêche cela

### **Problème 3: Les compteurs deviennent négatifs**
**Solution:** Les requêtes SQL incluent `AND likes > 0` et `AND dislikes > 0`

### **Problème 4: Le style des boutons ne change pas**
**Solution:** Vérifier que `updateButtonsStyle()` est appelé après chaque action

---

## 📊 RÉSULTATS ATTENDUS

✅ **Tous les tests doivent passer**
✅ **Aucune erreur dans la console**
✅ **Les compteurs sont cohérents avec la base de données**
✅ **L'interface est réactive et fluide**
✅ **Les messages de confirmation s'affichent correctement**

---

## 🎨 VÉRIFICATION DU DESIGN

### **Commentaires:**
- [ ] Carte blanche avec ombre subtile
- [ ] Avatar circulaire violet
- [ ] Nom d'utilisateur en gras et noir
- [ ] Texte du commentaire lisible (14px)
- [ ] Boutons avec hover effect (bleu pour modifier, rouge pour supprimer)
- [ ] Séparateur gris entre contenu et actions

### **Posts:**
- [ ] Nom d'utilisateur affiché (pas "Auteur #ID")
- [ ] Boutons Like/Dislike avec couleurs appropriées
- [ ] Compteurs visibles et à jour

---

## 🚀 COMMANDES UTILES

```bash
# Compiler le projet
mvn clean compile

# Lancer l'application
mvn javafx:run

# Vérifier les logs
tail -f logs/application.log

# Réinitialiser les réactions (si nécessaire)
mysql -u root -p museum_db < SQL_FIX_POST_REACTIONS.sql
```

---

## 📝 NOTES

- Les modifications sont appliquées dans `ForumPostDetailController.java`
- Le CSS est mis à jour dans `forum_style.css`
- La logique de réaction est dans `PostServices.java`
- Les noms d'utilisateurs sont récupérés via `UserService.java`

**Date de test:** _________________
**Testeur:** _________________
**Résultat:** ✅ PASS / ❌ FAIL
**Commentaires:** _________________
