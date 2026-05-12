<div align="center">

<img src="src/main/resources/images/museum_bardo.jpg" alt="ArtTn Banner" width="100%" style="border-radius:12px"/>

# 🏛️ ArtTn — Museum Digital Desktop App

**Application de gestion complète d'un musée d'art tunisien**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-0078D4?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Python](https://img.shields.io/badge/Python-3.10+-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-Academic-green?style=for-the-badge)](./README.md)

*Projet académique — ESPRIT, Tunis, Tunisie*

</div>

---

## 📌 Description

**ArtTn** est une application desktop riche développée avec **JavaFX 21** et **Java 17**, conçue pour la gestion complète d'un musée d'art tunisien. Elle s'intègre avec un site web **Symfony** en partageant la même base de données **MySQL**, offrant une expérience unifiée entre les deux plateformes.

L'application propose deux interfaces distinctes — une pour les **administrateurs** et une pour les **visiteurs** — couvrant la gestion des événements, des réservations, de la boutique, du forum, des offres d'emploi et des sponsors, le tout enrichi par des fonctionnalités d'**intelligence artificielle** et de **machine learning**.

---

## 🏷️ Topics & Mots-clés

`javafx` `java17` `mysql` `maven` `desktop-app` `museum` `tunisia` `symfony-integration`
`gemini-ai` `machine-learning` `scikit-learn` `qr-code` `pdf-generation` `google-oauth`
`bcrypt` `javafx-fxml` `mvc-pattern` `event-management` `reservation-system` `forum`
`e-commerce` `job-board` `i18n` `text-to-speech` `fraud-detection` `esprit`

---

## 📋 Table des matières

- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#-architecture)
- [Stack technique](#-stack-technique)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Modules ML](#-modules-machine-learning)
- [Structure du projet](#-structure-du-projet)
- [Scripts SQL](#-scripts-sql-utilitaires)
- [FAQ & Dépannage](#-faq--dépannage)
- [Équipe](#-équipe)

---

## ✨ Fonctionnalités

<details>
<summary><b>🔐 Authentification & Gestion des utilisateurs</b></summary>

- Inscription et connexion sécurisées avec hachage **BCrypt**
- Authentification via **Google OAuth 2.0**
- Réinitialisation de mot de passe par email
- Gestion de profil (avatar, informations personnelles)
- Détection automatique de fraude sur les comptes (modèle ML Python)
- Gestion de session avec `SessionManager`

</details>

<details>
<summary><b>📅 Gestion des événements</b></summary>

- Catalogue visuel avec filtres par catégorie, date et prix
- Catégories : `EXPOSITION` · `CONFERENCE` · `ATELIER` · `VISITE_GUIDEE` · `SPECTACLE`
- Statuts : `A_VENIR` · `EN_COURS` · `TERMINE` · `ANNULE`
- Recommandations personnalisées par historique utilisateur
- Synthèse vocale (**Text-to-Speech**) des détails d'événement
- Export **PDF** de la liste des événements (iText 7)
- **Optimisation des prix par IA** (Google Gemini)
- Compatibilité totale avec les événements créés depuis le site web Symfony

</details>

<details>
<summary><b>🎟️ Système de réservation</b></summary>

- Sélection visuelle interactive des sièges (grille **8 × 10 = 80 places**)
- Sièges **VIP** (+10 DT) sur les colonnes latérales
- Code promo `ArtTn` → **-10%**
- Génération de **QR Code** unique par réservation (ZXing)
- Calcul des places disponibles en temps réel depuis la base de données
- Statuts : `EN_ATTENTE` · `CONFIRMEE` · `ANNULEE` · `REFUSEE`
- Notifications email automatiques à chaque changement de statut
- Export **Google Calendar / iCal** pour les réservations confirmées
- Historique complet des réservations par utilisateur

</details>

<details>
<summary><b>🛍️ Boutique & Produits</b></summary>

- Catalogue de produits dérivés du musée
- Panier d'achat avec gestion des quantités
- Historique des commandes
- Interface d'administration CRUD complète
- Export **PDF** du catalogue produits
- **Prédiction des ventes** par modèle ML Python (scikit-learn)

</details>

<details>
<summary><b>💼 Offres d'emploi & Candidatures</b></summary>

- Catalogue des offres avec filtres avancés
- Dépôt et suivi de candidatures
- Gestion des offres favorites
- **Matching CV / offre** par IA (Python + Gemini)
- Génération automatique de description d'offre par **Gemini AI**
- Export **Excel** des candidatures (Apache POI)

</details>

<details>
<summary><b>🤝 Sponsors & Packs</b></summary>

- Gestion complète des sponsors du musée
- Catalogue de packs de sponsoring par événement
- **Génération automatique de packs** par Gemini AI
- Interface de navigation dédiée aux sponsors

</details>

<details>
<summary><b>💬 Forum communautaire</b></summary>

- Posts organisés par catégories
- Commentaires et réponses imbriquées
- Système **Like / Dislike** (compatible Symfony via `reaction_type`)
- Signalement de contenu (Report)
- Modération automatique du contenu
- **Traduction automatique** des posts (Google Translate API)
- Affichage des noms d'utilisateurs réels

</details>

<details>
<summary><b>🤖 Chatbot & IA</b></summary>

- Assistant conversationnel intégré pour les visiteurs
- Optimisation des prix par **Google Gemini**
- Génération de contenu (descriptions, packs) par IA

</details>

<details>
<summary><b>📊 Dashboard & Statistiques</b></summary>

- Dashboard admin avec KPIs en temps réel
- Statistiques : réservations, revenus, taux de remplissage
- Graphiques et indicateurs visuels colorés

</details>

<details>
<summary><b>🌍 Internationalisation (i18n)</b></summary>

- Interface disponible en **Français 🇫🇷**, **Anglais 🇬🇧** et **Arabe 🇹🇳**
- Sélecteur de langue dynamique sans redémarrage

</details>

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                  ArtTn Desktop App (JavaFX)                  │
│                                                              │
│   ┌─────────────┐  ┌─────────────┐  ┌────────────────────┐  │
│   │ Controllers │  │  Services   │  │      Utils         │  │
│   │  (FXML MVC) │→ │ (Business   │→ │ DB · Email · PDF   │  │
│   │  43 classes │  │  Logic)     │  │ QR · AI · ML · ... │  │
│   └─────────────┘  └─────────────┘  └────────────────────┘  │
│                          │                                   │
│                   ┌──────▼──────┐                            │
│                   │  Entities   │                            │
│                   │ 15 modèles  │                            │
│                   └─────────────┘                            │
└──────────────────────────┬───────────────────────────────────┘
                           │  JDBC (MySQL Connector 8.0.33)
                           ▼
              ┌────────────────────────┐
              │   MySQL — esprit_museum │
              └────────────┬───────────┘
                           │  Base partagée
                           ▼
              ┌────────────────────────┐
              │   Application Web      │
              │      (Symfony)         │
              └────────────────────────┘

              ┌────────────────────────┐
              │   Modules ML (Python)  │
              │  fraud · sales · match │
              │  Appelés via Java      │
              │  ProcessBuilder        │
              └────────────────────────┘
```

**Pattern architectural :** MVC (Model-View-Controller) avec séparation stricte des couches.

---

## 🛠️ Stack technique

### Application Desktop

| Technologie | Version | Rôle |
|---|---|---|
| **Java** | 17 LTS | Langage principal |
| **JavaFX** | 21 | Interface graphique (FXML + CSS) |
| **Maven** | 3.6+ | Build & gestion des dépendances |
| **MySQL Connector/J** | 8.0.33 | Accès base de données |
| **jBCrypt** | 0.4 | Hachage sécurisé des mots de passe |
| **JavaMail** | 1.6.2 | Envoi d'emails SMTP |
| **iText 7** | 7.2.5 | Génération de PDF |
| **ZXing** | 3.5.3 | Génération de QR Codes |
| **OkHttp** | 4.12.0 | Appels REST vers Gemini AI |
| **Gson** | 2.10.1 | Sérialisation / parsing JSON |
| **Apache POI** | 5.2.5 | Export Excel (.xlsx) |
| **Google Cloud Translate** | 2.4.0 | Traduction automatique |
| **JUnit 5** | 5.10.1 | Tests unitaires |

### Machine Learning

| Bibliothèque | Rôle |
|---|---|
| **scikit-learn** | Modèles de classification et régression |
| **pandas** | Manipulation et analyse des données |
| **numpy** | Calcul numérique |
| **pickle** | Sérialisation des modèles entraînés |

### Infrastructure

| Composant | Détail |
|---|---|
| **Base de données** | MySQL 8.0 — `esprit_museum` |
| **Projet web associé** | Symfony (même base de données) |
| **IA externe** | Google Gemini API |
| **Auth externe** | Google OAuth 2.0 |

---

## 📦 Prérequis

Avant de lancer le projet, assurez-vous d'avoir installé :

| Outil | Version minimale | Lien |
|---|---|---|
| JDK | 17 | [Adoptium Temurin](https://adoptium.net/) |
| Maven | 3.6 | [apache.org](https://maven.apache.org/download.cgi) |
| MySQL | 8.0 | [mysql.com](https://dev.mysql.com/downloads/) |
| Python *(optionnel, ML)* | 3.10 | [python.org](https://www.python.org/downloads/) |

> **Connexion internet requise** pour : Google Gemini AI, Google OAuth, Google Translate API.

---

## 🚀 Installation

### 1 — Cloner le dépôt

```bash
git clone https://github.com/<votre-org>/ArtTn_app-IntegFinale.git
cd ArtTn_app-IntegFinale
```

### 2 — Créer la base de données

```sql
CREATE DATABASE esprit_museum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Puis importer le schéma fourni par le projet Symfony ou créer les tables manuellement.

### 3 — Configurer la connexion DB

Éditer `src/main/java/tn/esprit/museum/utils/DatabaseConnection.java` :

```java
private static final String URL      = "jdbc:mysql://localhost:3306/esprit_museum"
        + "?useSSL=false&serverTimezone=UTC";
private static final String USER     = "root";       // ← votre utilisateur MySQL
private static final String PASSWORD = "";           // ← votre mot de passe MySQL
```

### 4 — Installer les dépendances Maven

```bash
mvn clean install -DskipTests
```

### 5 — Lancer l'application

```bash
mvn javafx:run
```

> Ou depuis **IntelliJ IDEA** : Run → `tn.esprit.museum.MainApp`

### 6 — (Optionnel) Dépendances Python pour le ML

```bash
pip install scikit-learn pandas numpy
```

---

## ⚙️ Configuration

### Variables de connexion DB

```
Fichier : src/main/java/tn/esprit/museum/utils/DatabaseConnection.java
```

### Clés API

Les clés API (Gemini, Google OAuth, SMTP) sont configurées directement dans les classes de service concernées :

| Service | Classe |
|---|---|
| Google Gemini AI | `PriceOptimizationService.java`, `ChatbotService.java`, `GeminiOfferDescriptionService.java` |
| Google OAuth | `GoogleAuthService.java` |
| Email SMTP | `EmailUtil.java` |
| Google Translate | `TranslationService.java` |

### Compatibilité avec Symfony

Les deux projets partagent la même base de données. En cas d'incompatibilité de schéma :

**Problème `status` ENUM :**
```sql
-- Convertir en VARCHAR pour accepter les valeurs des deux projets
ALTER TABLE reservations
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE';
```

**Resynchroniser les places disponibles :**
```bash
# Exécuter le script fourni dans phpMyAdmin
FIX_SYNC_CAPACITES.sql
```

---

## 🤖 Modules Machine Learning

Les modèles Python sont dans `ml/` et invoqués depuis Java via `ProcessBuilder`.

### 🔍 Détection de fraude utilisateur

```
Fichiers  : ml/train_model.py · ml/predict.py
Dataset   : ml/datasets/users_fraud_dataset.csv
Modèle    : ml/models/fraud_model.pkl
Classe    : utils/FraudDetectionService.java
```

```bash
# Ré-entraîner le modèle
python ml/train_model.py
```

### 📈 Prédiction des ventes produits

```
Fichiers  : ml/train_sales_model.py · ml/predict_sales.py
Modèle    : ml/models/sales_prediction_model.pkl
Classe    : utils/SalesPredictionService.java
```

```bash
python ml/train_sales_model.py
```

### 🎯 Matching candidats / offres d'emploi

```
Fichier   : ml/candidate_matching.py
Entrée    : ml/sample_matching_input.json
Classe    : services/PythonCandidateMatchingService.java
```

```bash
python ml/candidate_matching.py
```

---

## 📁 Structure du projet

```
ArtTn_app-IntegFinale/
│
├── 📄 pom.xml                          # Configuration Maven & dépendances
├── 📄 README.md
│
├── 📂 src/
│   ├── 📂 main/
│   │   ├── 📂 java/tn/esprit/museum/
│   │   │   │
│   │   │   ├── 🚀 MainApp.java         # Point d'entrée JavaFX
│   │   │   │
│   │   │   ├── 📂 controllers/         # 43 controllers (MVC)
│   │   │   │   ├── LoginController
│   │   │   │   ├── EventFrontController
│   │   │   │   ├── EventManagementController
│   │   │   │   ├── SeatSelectionController
│   │   │   │   ├── ReservationController
│   │   │   │   ├── ForumPostsController
│   │   │   │   ├── ProductsViewController
│   │   │   │   ├── JobFrontController
│   │   │   │   ├── DashboardController
│   │   │   │   └── ... (34 autres)
│   │   │   │
│   │   │   ├── 📂 entities/            # 15 modèles de données
│   │   │   │   ├── User · Event · Reservation
│   │   │   │   ├── Product · Order · OrderItem
│   │   │   │   ├── Posts · Category · commentaire
│   │   │   │   ├── OfferJob · DemandeJob
│   │   │   │   ├── Sponsor · Pack
│   │   │   │   └── UserDonation · Report
│   │   │   │
│   │   │   ├── 📂 services/            # 26 services métier
│   │   │   │   ├── EventService
│   │   │   │   ├── ReservationService
│   │   │   │   ├── UserService
│   │   │   │   ├── PriceOptimizationService  ← Gemini AI
│   │   │   │   ├── ChatbotService            ← Gemini AI
│   │   │   │   ├── RecommendationService
│   │   │   │   ├── ModerationService
│   │   │   │   ├── TranslationService        ← Google Translate
│   │   │   │   └── ... (18 autres)
│   │   │   │
│   │   │   ├── 📂 interfaces/          # Contrats de service (IService, etc.)
│   │   │   │
│   │   │   └── 📂 utils/              # 18 utilitaires transverses
│   │   │       ├── DatabaseConnection  ← Connexion MySQL
│   │   │       ├── EmailUtil           ← SMTP JavaMail
│   │   │       ├── QRCodeGenerator     ← ZXing
│   │   │       ├── GoogleAuthService   ← OAuth 2.0
│   │   │       ├── FraudDetectionService ← ML Python
│   │   │       ├── SalesPredictionService ← ML Python
│   │   │       ├── TextToSpeechUtil    ← TTS
│   │   │       ├── SessionManager      ← Session utilisateur
│   │   │       ├── PDFExporter         ← iText
│   │   │       ├── ExcelExporter       ← Apache POI
│   │   │       └── ... (8 autres)
│   │   │
│   │   └── 📂 resources/
│   │       ├── 📂 fxml/               # 44 vues FXML
│   │       ├── 📂 css/                # 3 feuilles de style
│   │       │   ├── style.css
│   │       │   ├── front.css
│   │       │   └── forum_style.css
│   │       ├── 📂 i18n/               # Fichiers de traduction
│   │       │   ├── messages_fr.properties
│   │       │   ├── messages_en.properties
│   │       │   └── messages_ar.properties
│   │       └── 📂 images/             # Assets visuels
│   │
│   └── 📂 test/java/                  # Tests JUnit 5
│
└── 📂 ml/                             # Modules Python ML
    ├── 📂 models/
    │   ├── fraud_model.pkl
    │   └── sales_prediction_model.pkl
    ├── 📂 datasets/
    │   └── users_fraud_dataset.csv
    ├── train_model.py
    ├── predict.py
    ├── train_sales_model.py
    ├── predict_sales.py
    └── candidate_matching.py
```

---

## 🗃️ Scripts SQL utilitaires

| Fichier | Quand l'utiliser |
|---|---|
| [`FIX_SYNC_CAPACITES.sql`](./FIX_SYNC_CAPACITES.sql) | Places disponibles incorrectes après des réservations web |
| [`DIAGNOSTIC_STATUS_ENUM.sql`](./DIAGNOSTIC_STATUS_ENUM.sql) | Erreur `Data truncated for column 'status'` |
| [`MIGRATION_TYPE_TO_REACTION_TYPE.sql`](./MIGRATION_TYPE_TO_REACTION_TYPE.sql) | Compteurs likes/dislikes incohérents entre Symfony et Java |
| [`FIX_RAPIDE.sql`](./FIX_RAPIDE.sql) | Corrections rapides sur la table `post_reactions` |

---

## ❓ FAQ & Dépannage

<details>
<summary><b>❌ Data truncated for column 'status' at row 1</b></summary>

La colonne `status` de `reservations` est un ENUM Symfony incompatible avec les valeurs Java.

```sql
ALTER TABLE reservations
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE';
```

</details>

<details>
<summary><b>❌ Les événements du site web n'apparaissent pas</b></summary>

Le mapping est défensif et gère les différences de schéma automatiquement. Vérifier que la table `events` contient les colonnes `event_date`, `max_capacity` et `status`. Consulter les logs de la console pour les avertissements `⚠️`.

</details>

<details>
<summary><b>❌ Places disponibles incorrectes</b></summary>

Exécuter `FIX_SYNC_CAPACITES.sql` dans phpMyAdmin pour recalculer `current_capacity` depuis les réservations réelles.

</details>

<details>
<summary><b>❌ Erreur de connexion à la base de données</b></summary>

1. Vérifier que le service MySQL est démarré
2. Contrôler les paramètres dans `DatabaseConnection.java` (URL, USER, PASSWORD)
3. S'assurer que la base `esprit_museum` existe

</details>

<details>
<summary><b>❌ Gemini AI / Google services ne répondent pas</b></summary>

1. Vérifier la connexion internet
2. Contrôler la validité des clés API dans les classes de service
3. Vérifier les quotas de l'API Google Cloud

</details>

<details>
<summary><b>❌ Les modules ML Python échouent</b></summary>

```bash
# Vérifier l'installation Python
python --version

# Installer les dépendances
pip install scikit-learn pandas numpy

# Ré-entraîner les modèles si les .pkl sont absents
python ml/train_model.py
python ml/train_sales_model.py
```

</details>

---

## 👥 Équipe

Projet réalisé dans le cadre du cursus ingénierie à **ESPRIT — École Supérieure Privée d'Ingénierie et de Technologies**, Tunis, Tunisie.

| Rôle | Module |
|---|---|
| Développeur | Événements & Réservations |
| Développeur | Forum & Modération |
| Développeur | Boutique & Produits |
| Développeur | Offres d'emploi & Candidatures |
| Développeur | Sponsors & Packs |
| Développeur | Authentification & Utilisateurs |

---

## 📄 Licence

```
Projet académique — ESPRIT, Tunis, Tunisie.
Tous droits réservés © 2025.
Usage interne uniquement.
```

---

<div align="center">

**ArtTn** — Valoriser le patrimoine culturel tunisien à travers la technologie 🇹🇳

*Développé avec ❤️ à ESPRIT*

</div>
