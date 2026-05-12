# COMPREHENSIVE FORUM MANAGEMENT SYSTEM - DETAILED SPECIFICATION FOR SYMFONY MIGRATION

## 📋 PROJECT OVERVIEW
This document describes a complete forum management system originally built in Java/JavaFX that needs to be migrated to Symfony (PHP). The system includes advanced features like AI moderation, translation, text-to-speech, like/dislike system, reporting mechanism, and comprehensive admin dashboard.

---

## 🗄️ DATABASE ARCHITECTURE

### 1. **Table: `post`**
Main table for forum posts with engagement metrics.

```sql
CREATE TABLE post (
    post_id INT PRIMARY KEY AUTO_INCREMENT,
    post_titre VARCHAR(100) NOT NULL,
    post_contenu TEXT NOT NULL,
    author_id INT NOT NULL,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    category_id INT DEFAULT NULL,
    views INT DEFAULT 0,
    likes INT DEFAULT 0,
    dislikes INT DEFAULT 0,
    image_url VARCHAR(255) DEFAULT NULL,
    report_count INT DEFAULT 0,
    FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    INDEX idx_category (category_id),
    INDEX idx_author (author_id),
    INDEX idx_date (date_creation)
);
```

**Field Descriptions:**
- `post_id`: Unique identifier for each post
- `post_titre`: Post title (max 100 characters)
- `post_contenu`: Post content (unlimited text)
- `author_id`: Foreign key to user table
- `date_creation`: Timestamp when post was created
- `category_id`: Foreign key to category table (nullable)
- `views`: Number of unique views
- `likes`: Number of likes received
- `dislikes`: Number of dislikes received
- `image_url`: Path to uploaded image (optional)
- `report_count`: Cached count of reports (updated via trigger or manually)

---

### 2. **Table: `commentaire`**
Comments associated with posts.

```sql
CREATE TABLE commentaire (
    comment_id INT PRIMARY KEY AUTO_INCREMENT,
    post_id INT NOT NULL,
    contenu TEXT NOT NULL,
    author_id INT NOT NULL,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_post (post_id),
    INDEX idx_author (author_id)
);
```

**Field Descriptions:**
- `comment_id`: Unique identifier for each comment
- `post_id`: Foreign key to post table
- `contenu`: Comment text content
- `author_id`: Foreign key to user table
- `date_creation`: Timestamp when comment was created

---

### 3. **Table: `category`**
Forum categories for organizing posts.

```sql
CREATE TABLE category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(10) DEFAULT '🏷️',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**Field Descriptions:**
- `id`: Unique identifier for each category
- `name`: Category name (unique)
- `description`: Category description
- `icon`: Emoji icon for visual representation
- `created_at`: Timestamp when category was created

---

### 4. **Table: `report`**
User reports for inappropriate posts.

```sql
CREATE TABLE report (
    id INT PRIMARY KEY AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_report (post_id, user_id),
    INDEX idx_status (status),
    INDEX idx_post (post_id)
);
```

**Field Descriptions:**
- `id`: Unique identifier for each report
- `post_id`: Foreign key to reported post
- `user_id`: Foreign key to user who reported
- `reason`: Predefined reason (Spam, Toxic, Violence, Illegal, Offensive, Other)
- `details`: Additional details (required if reason is "Other")
- `status`: Report status (PENDING, RESOLVED, DISMISSED)
- `created_at`: Timestamp when report was created

**Constraint:** One user can only report a post once (unique constraint on post_id + user_id)

---

### 5. **Table: `post_views`**
Tracks unique views per user per post.

```sql
CREATE TABLE post_views (
    id INT PRIMARY KEY AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_view (post_id, user_id),
    INDEX idx_post (post_id)
);
```

**Field Descriptions:**
- `id`: Unique identifier
- `post_id`: Foreign key to post
- `user_id`: Foreign key to user
- `viewed_at`: Timestamp of first view

**Constraint:** One user can only view a post once (unique constraint prevents duplicate views)

---

### 6. **Table: `post_reactions`**
Tracks user reactions (likes/dislikes) to posts.

```sql
CREATE TABLE post_reactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    type ENUM('LIKE', 'DISLIKE') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_reaction (post_id, user_id),
    INDEX idx_post (post_id),
    INDEX idx_type (type)
);
```

**Field Descriptions:**
- `id`: Unique identifier
- `post_id`: Foreign key to post
- `user_id`: Foreign key to user
- `type`: Reaction type (LIKE or DISLIKE)
- `created_at`: Timestamp when reaction was created

**Constraint:** One user can only have one reaction per post (can toggle or switch between LIKE/DISLIKE)

---

## 🔧 CRUD OPERATIONS DETAILED

### **POSTS CRUD**

#### **CREATE (Add Post)**
**Validation Rules:**
- Title: Required, 3-100 characters
- Content: Required, minimum 10 characters
- Author: Must be authenticated user
- Category: Optional
- Image: Optional (PNG, JPG, JPEG, GIF, BMP)

**Business Logic:**
1. Check if user is authenticated
2. Validate title and content
3. Check for duplicate title by same author
4. Check for duplicate content by same author
5. **AI MODERATION**: Send title + content to moderation API
   - If toxic → reject with error message
   - If spam → reject with error message
   - If normal → proceed
6. Save image to `/images/posts/` directory (if provided)
7. Insert post into database with current timestamp
8. Initialize views, likes, dislikes to 0

**SQL Query:**
```sql
INSERT INTO post (post_titre, post_contenu, author_id, date_creation, category_id, image_url) 
VALUES (?, ?, ?, NOW(), ?, ?)
```

---

#### **READ (Get Posts)**

**Get All Posts:**
```sql
SELECT * FROM post ORDER BY date_creation DESC
```

**Get Posts by Category:**
```sql
SELECT * FROM post WHERE category_id = ? ORDER BY date_creation DESC
```

**Get Single Post by ID:**
```sql
SELECT * FROM post WHERE post_id = ?
```

**Get Post with Engagement Metrics:**
```sql
SELECT 
    p.*,
    COUNT(DISTINCT c.comment_id) as comment_count,
    COUNT(DISTINCT r.id) as report_count
FROM post p
LEFT JOIN commentaire c ON p.post_id = c.post_id
LEFT JOIN report r ON p.post_id = r.post_id AND r.status = 'PENDING'
WHERE p.post_id = ?
GROUP BY p.post_id
```

---

#### **UPDATE (Edit Post)**
**Validation Rules:**
- Title: Required, 3-100 characters
- Content: Required, minimum 10 characters
- Must detect if content actually changed

**Business Logic:**
1. Check if user is post author or admin
2. Validate new title and content
3. Check if content actually changed
4. Update post in database
5. Preserve original date_creation

**SQL Query:**
```sql
UPDATE post 
SET post_titre = ?, post_contenu = ?, category_id = ?, image_url = ? 
WHERE post_id = ?
```

---

#### **DELETE (Remove Post)**
**Business Logic:**
1. Check if user is post author or admin
2. Confirm deletion with user
3. Delete post (CASCADE will delete comments, views, reactions, reports)

**SQL Query:**
```sql
DELETE FROM post WHERE post_id = ?
```

---

### **COMMENTS CRUD**

#### **CREATE (Add Comment)**
**Validation Rules:**
- Content: Required, 3-1000 characters
- Author: Must be authenticated user
- Post: Must exist

**SQL Query:**
```sql
INSERT INTO commentaire (post_id, contenu, author_id, date_creation) 
VALUES (?, ?, ?, NOW())
```

---

#### **READ (Get Comments)**
**Get Comments by Post:**
```sql
SELECT * FROM commentaire WHERE post_id = ? ORDER BY date_creation ASC
```

**Count Comments for Post:**
```sql
SELECT COUNT(*) FROM commentaire WHERE post_id = ?
```

---

#### **UPDATE (Edit Comment)**
**Validation Rules:**
- Content: Required, 3-1000 characters
- Must detect if content actually changed

**SQL Query:**
```sql
UPDATE commentaire SET contenu = ? WHERE comment_id = ?
```

---

#### **DELETE (Remove Comment)**
**SQL Query:**
```sql
DELETE FROM commentaire WHERE comment_id = ?
```

---

### **CATEGORIES CRUD**

#### **CREATE (Add Category)**
**Validation Rules:**
- Name: Required, unique
- Description: Optional
- Icon: Optional (default: 🏷️)

**SQL Query:**
```sql
INSERT INTO category (name, description, icon) VALUES (?, ?, ?)
```

---

#### **READ (Get Categories)**
```sql
SELECT * FROM category ORDER BY name ASC
```

---

#### **UPDATE (Edit Category)**
```sql
UPDATE category SET name = ?, description = ?, icon = ? WHERE id = ?
```

---

#### **DELETE (Remove Category)**
**Business Logic:**
- Posts in this category will have category_id set to NULL (ON DELETE SET NULL)

```sql
DELETE FROM category WHERE id = ?
```

---

### **REPORTS CRUD**

#### **CREATE (Report Post)**
**Validation Rules:**
- User can only report a post once
- Reason: Required (predefined list)
- Details: Required if reason is "Other"

**Predefined Reasons:**
- Spam / Publicité
- Contenu toxique / Harcèlement
- Incitation à la violence
- Contenu illégal
- Propos offensants
- Autre

**SQL Query:**
```sql
INSERT INTO report (post_id, user_id, reason, details, status) 
VALUES (?, ?, ?, ?, 'PENDING')
```

---

#### **READ (Get Reports)**
**Get All Reports:**
```sql
SELECT r.*, p.post_titre 
FROM report r
LEFT JOIN post p ON r.post_id = p.post_id
ORDER BY r.created_at DESC
```

**Get Reports by Status:**
```sql
SELECT * FROM report WHERE status = ? ORDER BY created_at DESC
```

---

#### **UPDATE (Resolve Report)**
```sql
UPDATE report SET status = 'RESOLVED' WHERE id = ?
```

---

#### **DELETE (Remove Report)**
```sql
DELETE FROM report WHERE id = ?
```

---

## 🚀 ADVANCED FEATURES

### 1. **LIKE/DISLIKE SYSTEM**

**Business Logic:**
- User can like OR dislike a post (not both)
- User can toggle their reaction (click again to remove)
- User can switch from like to dislike and vice versa
- Counters are updated in real-time

**Implementation:**

**Check User's Current Reaction:**
```sql
SELECT type FROM post_reactions WHERE post_id = ? AND user_id = ?
```

**Add/Toggle Reaction:**
```php
// Pseudo-code logic
if (user has no reaction) {
    INSERT INTO post_reactions (post_id, user_id, type) VALUES (?, ?, ?)
    UPDATE post SET likes = likes + 1 WHERE post_id = ? // if LIKE
    UPDATE post SET dislikes = dislikes + 1 WHERE post_id = ? // if DISLIKE
}
else if (user has same reaction) {
    DELETE FROM post_reactions WHERE post_id = ? AND user_id = ?
    UPDATE post SET likes = likes - 1 WHERE post_id = ? // if was LIKE
    UPDATE post SET dislikes = dislikes - 1 WHERE post_id = ? // if was DISLIKE
}
else if (user has different reaction) {
    DELETE FROM post_reactions WHERE post_id = ? AND user_id = ?
    UPDATE post SET likes = likes - 1 WHERE post_id = ? // decrement old
    INSERT INTO post_reactions (post_id, user_id, type) VALUES (?, ?, ?)
    UPDATE post SET dislikes = dislikes + 1 WHERE post_id = ? // increment new
}
```

**UI Behavior:**
- Active reaction button: Blue (like) or Red (dislike), bold
- Inactive reaction button: Gray, normal weight
- Display counts next to buttons

---

### 2. **UNIQUE VIEW TRACKING**

**Business Logic:**
- Each user can only increment view count once per post
- View is recorded when user opens post detail page
- Anonymous users are not tracked (require authentication)

**Implementation:**

**Check if User Viewed Post:**
```sql
SELECT 1 FROM post_views WHERE post_id = ? AND user_id = ?
```

**Record View:**
```sql
INSERT INTO post_views (post_id, user_id) VALUES (?, ?)
UPDATE post SET views = views + 1 WHERE post_id = ?
```

---

### 3. **REPORT SYSTEM (Signaler)**

**User Flow:**
1. User clicks "🚩 Signaler" button on post
2. Modal opens with checkboxes for reasons
3. User selects one reason
4. If "Autre" is selected, text area appears for details
5. User submits report
6. System checks if user already reported this post
7. If not, report is saved with status "PENDING"
8. Admin receives notification

**Admin Dashboard:**
- View all reports with post titles
- Filter by status (PENDING, RESOLVED, DISMISSED)
- Actions:
  - View reported post
  - Mark as resolved
  - Delete reported post
- Posts with 3+ reports are highlighted in red

---

### 4. **AI MODERATION SYSTEM**

**Technology:** Python Flask API with Hugging Face Transformers

**API Endpoint:** `http://localhost:5001/moderate`

**Request:**
```json
{
  "text": "Post title and content combined"
}
```

**Response:**
```json
{
  "label": "toxic" | "spam" | "normal",
  "is_toxic": true | false,
  "is_spam": true | false
}
```

**Integration:**
- Called before saving new post
- If toxic or spam detected, reject post with error message
- User sees: "❌ Désolé, votre message contient des propos inappropriés. (toxic)"

**Python API Code (Flask):**
```python
from flask import Flask, request, jsonify
from transformers import pipeline

app = Flask(__name__)
classifier = pipeline("text-classification", model="unitary/toxic-bert")

@app.route('/moderate', methods=['POST'])
def moderate():
    data = request.json
    text = data.get('text', '')
    
    result = classifier(text)[0]
    label = result['label'].lower()
    
    return jsonify({
        'label': label,
        'is_toxic': label == 'toxic',
        'is_spam': label == 'spam'
    })

if __name__ == '__main__':
    app.run(port=5001)
```

---

### 5. **TRANSLATION TO ARABIC (Traduction)**

**Technology:** MyMemory Translation API (free, no API key required)

**API Endpoint:** `https://api.mymemory.translated.net/get?q={text}&langpair=fr|ar`

**Features:**
- Translate post title and content
- Translate all comments
- Toggle between original and translated version
- Button: "🌐 ترجمة" (Translate to Arabic)
- Button: "🔙 Version originale" (Show original)

**Implementation:**
```php
// Pseudo-code
function translateToArabic($text) {
    $url = "https://api.mymemory.translated.net/get?q=" . urlencode($text) . "&langpair=fr|ar";
    $response = file_get_contents($url);
    $json = json_decode($response, true);
    return $json['responseData']['translatedText'];
}
```

**UI Behavior:**
- User clicks "🌐 ترجمة" button
- Loading message: "🔄 Traduction en cours vers l'arabe..."
- Post title, content, and comments are translated
- "🔙 Version originale" button appears
- User can toggle back to original

---

### 6. **TEXT-TO-SPEECH (Speaker)**

**Technology:** Browser Web Speech API or Server-side TTS

**Features:**
- Read post title and content aloud
- Language selection: Français, English
- Button: "🔊 Écouter" (Listen)

**Implementation (JavaScript - Web Speech API):**
```javascript
function speakText(text, language) {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = language === 'Français' ? 'fr-FR' : 'en-US';
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
}
```

**Alternative (Server-side with Google TTS):**
```php
// Use Google Cloud Text-to-Speech API
// Or use espeak/festival on Linux server
```

---

### 7. **ADMIN DASHBOARD**

**Features:**
- Statistics cards:
  - Total posts
  - Total comments
  - Total categories
  - Pending reports
- Four tabs:
  - Posts management
  - Comments management
  - Categories management
  - Reports management

**Posts Tab:**
- Table columns: ID, Title, Content (excerpt), Date, Comment Count, Report Count, Actions
- Search filter
- Actions: Edit, Delete
- Posts with 3+ reports highlighted in red

**Comments Tab:**
- Table columns: ID, Post ID, Content, Actions
- Search filter
- Actions: Edit, Delete

**Categories Tab:**
- Table columns: ID, Icon, Name, Description, Actions
- Search filter
- Actions: Add, Edit, Delete
- Add category button

**Reports Tab:**
- Table columns: ID, Post ID, Post Title, Reason, Details, Status, Date, Actions
- Search filter
- Actions: View Post, Resolve, Delete Post
- Status badges: PENDING (yellow), RESOLVED (green), DISMISSED (gray)

---

## 🎨 UI/UX SPECIFICATIONS & NAVIGATION FLOW

### **COMPLETE USER NAVIGATION FLOW**

#### **Step 1: Main Navigation - Forum Button Click**
When user clicks the "Forum" button in the main navigation:

**Action:**
- Load the Forum Categories page
- Display all available categories in a grid layout
- Show welcome message or forum description

**Technical Implementation:**
```php
// Route: /forum
// Controller: ForumController::categories()
// Template: forum/categories.html.twig
```

---

#### **Step 2: Forum Categories Page (Landing Page)**

**Layout Structure:**
```
┌─────────────────────────────────────────────────────────┐
│  HEADER: "Forum - Catégories"                           │
│  Subtitle: "Choisissez une catégorie pour commencer"    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  🖼️      │  │  🏺      │  │  🎨      │             │
│  │  Art     │  │ Histoire │  │ Peinture │             │
│  │ Moderne  │  │          │  │          │             │
│  │          │  │          │  │          │             │
│  │[Browse→] │  │[Browse→] │  │[Browse→] │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  🗿      │  │  📸      │  │  ✨      │             │
│  │Sculpture │  │  Photo   │  │ Général  │             │
│  │          │  │          │  │          │             │
│  │          │  │          │  │          │             │
│  │[Browse→] │  │[Browse→] │  │[Browse→] │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Category Card Detailed Styling:**

```css
/* Category Card Container */
.category-card {
    background: white;
    border-radius: 20px;
    padding: 20px;
    width: 250px;
    height: 220px;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
}

/* Hover Effect - Card Lifts */
.category-card:hover {
    transform: translateY(-10px);
    box-shadow: 0 15px 30px rgba(0, 0, 0, 0.15);
}

/* Category Icon (Emoji) */
.category-icon {
    font-size: 52px;
    margin-bottom: 12px;
    animation: bounce 2s infinite;
}

/* Category Name */
.category-name {
    font-size: 16px;
    font-weight: bold;
    color: #2c3e50;
    text-align: center;
    margin-bottom: 8px;
}

/* Category Description */
.category-description {
    font-size: 11px;
    color: #7f8c8d;
    text-align: center;
    line-height: 1.4;
    flex-grow: 1;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* Browse Button */
.category-browse-btn {
    background-color: #f1f5f9;
    color: #475569;
    font-size: 13px;
    font-weight: bold;
    padding: 8px 20px;
    border-radius: 25px;
    border: none;
    cursor: pointer;
    transition: all 0.3s ease;
}

/* Browse Button Hover */
.category-browse-btn:hover {
    background-color: #3b82f6;
    color: white;
    box-shadow: 0 5px 10px rgba(59, 130, 246, 0.3);
    transform: scale(1.05);
}

/* Grid Layout */
.categories-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 30px;
    padding: 40px;
    max-width: 1200px;
    margin: 0 auto;
}
```

**Category Card HTML Structure:**
```html
<div class="category-card" onclick="loadCategoryPosts(categoryId)">
    <div class="category-icon">🖼️</div>
    <h3 class="category-name">Art Moderne</h3>
    <p class="category-description">
        Découvrez les discussions sur l'art contemporain, 
        les nouvelles tendances et les artistes émergents.
    </p>
    <button class="category-browse-btn">Parcourir →</button>
</div>
```

**Example Categories:**
1. **🖼️ Art Moderne** - "Découvrez les discussions sur l'art contemporain"
2. **🏺 Histoire de l'Art** - "Explorez les mouvements artistiques historiques"
3. **🎨 Peinture** - "Partagez vos techniques et inspirations"
4. **🗿 Sculpture** - "Discussions sur la sculpture et les installations"
5. **📸 Photographie** - "Techniques photo et critiques d'œuvres"
6. **✨ Général** - "Discussions générales sur l'art et la culture"

---

#### **Step 3: Category Selection - User Clicks "Parcourir →"**

**Action:**
- Smooth transition to Posts List page
- Load all posts belonging to selected category
- Display breadcrumb navigation
- Show category name in header

**Technical Implementation:**
```php
// Route: /forum/category/{id}
// Controller: ForumController::posts($categoryId)
// Template: forum/posts.html.twig
```

**Transition Animation:**
```css
/* Fade-in animation for posts page */
.posts-container {
    animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}
```

---

#### **Step 4: Posts List Page (Category Posts View)**

**Layout Structure:**
```
┌─────────────────────────────────────────────────────────────┐
│  BREADCRUMB: 🏠 Accueil > 📁 Art Moderne                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │  🔍 Rechercher un post...                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  [🔤 Trier A-Z]  [📅 Plus récents]  [➕ Nouveau post]      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 👤 Jean Dupont    📅 09/05/2026 14:30             │    │
│  │                                                     │    │
│  │ Les techniques de l'impressionnisme                │    │
│  │                                                     │    │
│  │ Bonjour à tous ! Je voudrais discuter des         │    │
│  │ techniques utilisées par les impressionnistes...   │    │
│  │                                                     │    │
│  │ 👁️ 245  👍 32  👎 2                                │    │
│  │                                                     │    │
│  │ 💬 12 commentaires                                 │    │
│  │ [Lire la suite →]  [🚩 Signaler]  [🌐 ترجمة]      │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 👤 Marie Martin    📅 08/05/2026 10:15            │    │
│  │                                                     │    │
│  │ Analyse de "La Nuit étoilée" de Van Gogh          │    │
│  │                                                     │    │
│  │ Cette œuvre emblématique représente...            │    │
│  │                                                     │    │
│  │ 👁️ 189  👍 45  👎 1                                │    │
│  │                                                     │    │
│  │ 💬 8 commentaires                                  │    │
│  │ [Lire la suite →]  [🚩 Signaler]  [🌐 ترجمة]      │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Post Card Detailed Styling (MODERN & ELEGANT DESIGN):**

```css
/* Post Card Container - Modern Glass Morphism Style */
.post-card {
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
    border-radius: 20px;
    padding: 28px;
    margin-bottom: 24px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06), 
                0 1px 3px rgba(0, 0, 0, 0.04);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    border: 1px solid rgba(226, 232, 240, 0.8);
    position: relative;
    overflow: hidden;
}

/* Gradient Accent Bar */
.post-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(180deg, #3b82f6 0%, #8b5cf6 100%);
    opacity: 0;
    transition: opacity 0.4s ease;
}

/* Post Card Hover Effect - Enhanced */
.post-card:hover {
    box-shadow: 0 12px 40px rgba(59, 130, 246, 0.15), 
                0 4px 12px rgba(0, 0, 0, 0.08);
    transform: translateY(-4px);
    border-color: rgba(59, 130, 246, 0.3);
}

.post-card:hover::before {
    opacity: 1;
}

/* Post Meta (Author & Date) - Enhanced */
.post-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e5e7eb;
}

.post-author {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 600;
    color: #1f2937;
    font-size: 14px;
}

.post-author-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 16px;
    font-weight: bold;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.post-date {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #6b7280;
    background: #f3f4f6;
    padding: 6px 12px;
    border-radius: 20px;
}

/* Post Title - Enhanced */
.post-title {
    font-size: 22px;
    font-weight: 700;
    color: #111827;
    margin-bottom: 14px;
    line-height: 1.4;
    cursor: pointer;
    transition: all 0.3s ease;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.post-title:hover {
    color: #3b82f6;
    transform: translateX(4px);
}

/* Post Excerpt - Enhanced */
.post-excerpt {
    font-size: 15px;
    color: #4b5563;
    line-height: 1.7;
    margin-bottom: 20px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-align: justify;
}

/* Post Stats Bar - Modern Design */
.post-stats {
    display: flex;
    align-items: center;
    gap: 24px;
    margin-bottom: 20px;
    padding: 16px;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-radius: 12px;
    border: 1px solid #e2e8f0;
}

.stat-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    padding: 6px 12px;
    border-radius: 8px;
    transition: all 0.3s ease;
}

.stat-views { 
    color: #6b7280;
    background: white;
}

.stat-likes { 
    color: #3b82f6;
    background: #eff6ff;
}

.stat-dislikes { 
    color: #ef4444;
    background: #fef2f2;
}

.stat-item:hover {
    transform: scale(1.05);
}

/* Post Actions Bar - Enhanced */
.post-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
}

.comment-count {
    font-size: 14px;
    color: #6b7280;
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    padding: 8px 14px;
    background: white;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
}

/* Action Buttons - Modern Gradient Design */
.btn-read-more {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 10px 24px;
    border-radius: 12px;
    border: none;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    position: relative;
    overflow: hidden;
}

.btn-read-more::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
    transition: left 0.5s ease;
}

.btn-read-more:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}

.btn-read-more:hover::before {
    left: 100%;
}

.btn-report {
    background: white;
    color: #ef4444;
    padding: 8px 16px;
    border: 2px solid #fecaca;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-report:hover {
    background: #fef2f2;
    border-color: #ef4444;
    transform: scale(1.05);
}

.btn-translate {
    background: white;
    color: #10b981;
    padding: 8px 16px;
    border: 2px solid #a7f3d0;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-translate:hover {
    background: #d1fae5;
    border-color: #10b981;
    transform: scale(1.05);
}

/* ========================================
   ENHANCED POST CARD FEATURES
   ======================================== */

/* Category Badge on Post Card */
.post-category-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
    color: white;
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 12px;
    box-shadow: 0 2px 8px rgba(251, 191, 36, 0.3);
}

/* Post Image Preview (if post has image) */
.post-image-preview {
    width: 100%;
    height: 200px;
    object-fit: cover;
    border-radius: 12px;
    margin-bottom: 16px;
    transition: transform 0.4s ease;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.post-card:hover .post-image-preview {
    transform: scale(1.02);
}

/* Trending/Hot Post Indicator */
.post-card.trending {
    border: 2px solid #fbbf24;
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

.post-card.trending::after {
    content: '🔥 Tendance';
    position: absolute;
    top: 16px;
    right: 16px;
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    color: white;
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 11px;
    font-weight: 700;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
    z-index: 10;
}

/* New Post Indicator */
.post-card.new-post::after {
    content: '✨ Nouveau';
    position: absolute;
    top: 16px;
    right: 16px;
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 11px;
    font-weight: 700;
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
    z-index: 10;
}

/* Pinned Post Style */
.post-card.pinned {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    border: 2px solid #3b82f6;
}

.post-card.pinned::before {
    content: '📌';
    position: absolute;
    top: 16px;
    left: 16px;
    font-size: 20px;
    z-index: 10;
}

/* Read Status Indicator */
.post-card.read {
    opacity: 0.85;
}

.post-card.read .post-title {
    color: #6b7280;
}

/* Skeleton Loading State */
.post-card.skeleton {
    pointer-events: none;
}

.skeleton-line {
    height: 16px;
    background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 4px;
    margin-bottom: 12px;
}

@keyframes shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

/* Smooth Entrance Animation */
.post-card {
    animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* Stagger animation for multiple posts */
.post-card:nth-child(1) { animation-delay: 0.1s; }
.post-card:nth-child(2) { animation-delay: 0.2s; }
.post-card:nth-child(3) { animation-delay: 0.3s; }
.post-card:nth-child(4) { animation-delay: 0.4s; }
.post-card:nth-child(5) { animation-delay: 0.5s; }

/* Dark Mode Support (Optional) */
@media (prefers-color-scheme: dark) {
    .post-card {
        background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
        border-color: #374151;
    }
    
    .post-title {
        color: #f9fafb;
    }
    
    .post-excerpt {
        color: #d1d5db;
    }
    
    .post-stats {
        background: linear-gradient(135deg, #374151 0%, #1f2937 100%);
        border-color: #4b5563;
    }
}
```

**Enhanced Post Card HTML Structure:**
```html
<div class="post-card new-post">
    <!-- Category Badge (Optional) -->
    <span class="post-category-badge">🎨 Peinture</span>
    
    <!-- Meta Information with Avatar -->
    <div class="post-meta">
        <div class="post-author">
            <div class="post-author-avatar">JD</div>
            <span>Jean Dupont</span>
        </div>
        <span class="post-date">📅 09/05/2026 14:30</span>
    </div>
    
    <!-- Post Title -->
    <h2 class="post-title">Les techniques de l'impressionnisme</h2>
    
    <!-- Post Image (Optional) -->
    <img src="/images/posts/impressionism.jpg" alt="Post image" class="post-image-preview">
    
    <!-- Post Excerpt -->
    <p class="post-excerpt">
        Bonjour à tous ! Je voudrais discuter des techniques 
        utilisées par les impressionnistes pour capturer la 
        lumière et le mouvement dans leurs œuvres. Les artistes 
        comme Monet, Renoir et Pissarro ont révolutionné...
    </p>
    
    <!-- Stats Bar with Modern Design -->
    <div class="post-stats">
        <span class="stat-item stat-views">👁️ 245</span>
        <span class="stat-item stat-likes">👍 32</span>
        <span class="stat-item stat-dislikes">👎 2</span>
    </div>
    
    <!-- Actions Bar -->
    <div class="post-actions">
        <span class="comment-count">💬 12 commentaires</span>
        <div style="display: flex; gap: 10px; margin-left: auto;">
            <button class="btn-read-more">Lire la suite →</button>
            <button class="btn-report">🚩 Signaler</button>
            <button class="btn-translate">🌐 ترجمة</button>
        </div>
    </div>
</div>
```

**Example: Trending Post**
```html
<div class="post-card trending">
    <!-- Content same as above -->
    <!-- "🔥 Tendance" badge will appear automatically via CSS -->
</div>
```

**Example: Pinned Post**
```html
<div class="post-card pinned">
    <!-- Content same as above -->
    <!-- "📌" icon will appear automatically via CSS -->
</div>
```

/* Sort Buttons */
.sort-buttons {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
}

.btn-sort {
    background: white;
    color: #4b5563;
    padding: 10px 20px;
    border: 2px solid #e5e7eb;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-sort:hover {
    border-color: #3b82f6;
    color: #3b82f6;
    background: #eff6ff;
}

.btn-sort.active {
    background: #3b82f6;
    color: white;
    border-color: #3b82f6;
}

/* New Post Button */
.btn-new-post {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
    padding: 10px 24px;
    border-radius: 10px;
    border: none;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 10px rgba(16, 185, 129, 0.3);
}

.btn-new-post:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 15px rgba(16, 185, 129, 0.4);
}

/* Search Bar */
.search-bar {
    width: 100%;
    padding: 14px 20px;
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    font-size: 14px;
    margin-bottom: 24px;
    transition: all 0.3s ease;
}

.search-bar:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* Breadcrumb */
.breadcrumb {
    font-size: 14px;
    color: #6b7280;
    margin-bottom: 24px;
    padding: 12px 0;
}

.breadcrumb a {
    color: #3b82f6;
    text-decoration: none;
    transition: color 0.2s ease;
}

.breadcrumb a:hover {
    color: #2563eb;
    text-decoration: underline;
}

/* Pagination */
.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    margin-top: 32px;
    padding: 20px 0;
}

.pagination-btn {
    background: white;
    color: #4b5563;
    padding: 10px 20px;
    border: 2px solid #e5e7eb;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.pagination-btn:hover:not(:disabled) {
    border-color: #3b82f6;
    color: #3b82f6;
    background: #eff6ff;
}

.pagination-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.pagination-info {
    font-size: 14px;
    color: #6b7280;
    font-weight: 500;
}
```

**Post Card HTML Structure:**
```html
<div class="post-card">
    <!-- Meta Information -->
    <div class="post-meta">
        <span class="post-author">👤 Jean Dupont</span>
        <span class="post-date">📅 09/05/2026 14:30</span>
    </div>
    
    <!-- Post Title -->
    <h2 class="post-title">Les techniques de l'impressionnisme</h2>
    
    <!-- Post Excerpt -->
    <p class="post-excerpt">
        Bonjour à tous ! Je voudrais discuter des techniques 
        utilisées par les impressionnistes pour capturer la 
        lumière et le mouvement dans leurs œuvres...
    </p>
    
    <!-- Stats Bar -->
    <div class="post-stats">
        <span class="stat-item stat-views">👁️ 245</span>
        <span class="stat-item stat-likes">� 32</span>
        <span class="stat-item stat-dislikes">👎 2</span>
    </div>
    
    <!-- Actions Bar -->
    <div class="post-actions">
        <span class="comment-count">💬 12 commentaires</span>
        <button class="btn-read-more">Lire la suite →</button>
        <button class="btn-report">🚩 Signaler</button>
        <button class="btn-translate">🌐 ترجمة</button>
    </div>
</div>
```

---

#### **Step 5: Post Card Click - Navigate to Post Detail**

**Action:**
- User clicks "Lire la suite →" button or post title
- Navigate to full post detail page
- Increment view count (once per user)
- Load all comments
- Display full content with image

**Technical Implementation:**
```php
// Route: /forum/post/{id}
// Controller: ForumController::postDetail($postId)
// Template: forum/post_detail.html.twig
```

---

#### **Step 6: Post Detail Page (Full Post View)**

**Layout Structure:**
```
┌─────────────────────────────────────────────────────────────┐
│  [← Retour aux posts]                                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Les techniques de l'impressionnisme                        │
│  ═══════════════════════════════════════                    │
│                                                              │
│  👤 Jean Dupont    📅 09/05/2026 14:30                      │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │                                                     │    │
│  │         [IMAGE IF PRESENT]                         │    │
│  │                                                     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  Bonjour à tous ! Je voudrais discuter des techniques      │
│  utilisées par les impressionnistes pour capturer la       │
│  lumière et le mouvement dans leurs œuvres. Les artistes   │
│  comme Monet, Renoir et Pissarro ont révolutionné...      │
│  [Full content displayed here]                              │
│                                                              │
│  ┌──────────────────────────────────────────────────┐      │
│  │  👁️ 245 vues  👍 32  👎 2                         │      │
│  │                                                    │      │
│  │  [👍 J'aime]  [👎 Je n'aime pas]  [🔊 Écouter]   │      │
│  │  [🌐 ترجمة]  [✏️ Modifier]  [🗑️ Supprimer]        │      │
│  └──────────────────────────────────────────────────┘      │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                              │
│  💬 Commentaires (12)                                       │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 👤 Marie Martin    📅 09/05/2026 15:45            │    │
│  │                                                     │    │
│  │ Excellente analyse ! J'ajouterais que la technique│    │
│  │ des touches rapides était également cruciale...    │    │
│  │                                                     │    │
│  │ [✏️ Modifier]  [🗑️ Supprimer]                      │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 👤 Pierre Dubois    📅 09/05/2026 16:20           │    │
│  │                                                     │    │
│  │ Merci pour ce partage ! Avez-vous des références  │    │
│  │ bibliographiques sur ce sujet ?                    │    │
│  │                                                     │    │
│  │ [✏️ Modifier]  [🗑️ Supprimer]                      │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  [💬 Ajouter un commentaire]                                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Post Detail Page Styling:**

```css
/* Post Detail Container */
.post-detail-container {
    max-width: 900px;
    margin: 0 auto;
    padding: 40px 20px;
    background: white;
    border-radius: 20px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* Back Button */
.btn-back {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: transparent;
    color: #3b82f6;
    padding: 10px 16px;
    border: 2px solid #3b82f6;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    margin-bottom: 24px;
}

.btn-back:hover {
    background: #3b82f6;
    color: white;
}

/* Post Title */
.post-detail-title {
    font-size: 32px;
    font-weight: bold;
    color: #1f2937;
    line-height: 1.3;
    margin-bottom: 8px;
    border-bottom: 3px solid #3b82f6;
    padding-bottom: 16px;
}

/* Post Meta */
.post-detail-meta {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 24px;
    font-size: 14px;
    color: #6b7280;
}

.post-detail-author {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: #374151;
}

/* Post Image */
.post-detail-image {
    width: 100%;
    max-height: 500px;
    object-fit: cover;
    border-radius: 15px;
    margin-bottom: 24px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

/* Post Content */
.post-detail-content {
    font-size: 16px;
    line-height: 1.8;
    color: #374151;
    margin-bottom: 32px;
    text-align: justify;
}

/* Engagement Section */
.post-engagement {
    background: #f9fafb;
    border-radius: 15px;
    padding: 24px;
    margin-bottom: 32px;
}

/* Stats Display */
.post-detail-stats {
    display: flex;
    align-items: center;
    gap: 24px;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 2px solid #e5e7eb;
}

.stat-detail {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
}

.stat-detail.views { color: #6b7280; }
.stat-detail.likes { color: #3b82f6; }
.stat-detail.dislikes { color: #ef4444; }

/* Action Buttons Container */
.post-detail-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
}

/* Like Button */
.btn-like {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: white;
    color: #9ca3af;
    padding: 12px 24px;
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-like:hover {
    border-color: #3b82f6;
    color: #3b82f6;
    background: #eff6ff;
}

.btn-like.active {
    background: #3b82f6;
    color: white;
    border-color: #3b82f6;
    font-weight: bold;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* Dislike Button */
.btn-dislike {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: white;
    color: #9ca3af;
    padding: 12px 24px;
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-dislike:hover {
    border-color: #ef4444;
    color: #ef4444;
    background: #fef2f2;
}

.btn-dislike.active {
    background: #ef4444;
    color: white;
    border-color: #ef4444;
    font-weight: bold;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

/* Text-to-Speech Button */
.btn-speak {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
    color: white;
    padding: 12px 24px;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.btn-speak:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

/* Translate Button */
.btn-translate-detail {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
    padding: 12px 24px;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.btn-translate-detail:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

/* Edit Button (Author/Admin only) */
.btn-edit-post {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: white;
    padding: 12px 24px;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.btn-edit-post:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
}

/* Delete Button (Author/Admin only) */
.btn-delete-post {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    color: white;
    padding: 12px 24px;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-delete-post:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
}

/* Comments Section */
.comments-section {
    margin-top: 48px;
    padding-top: 32px;
    border-top: 3px solid #e5e7eb;
}

.comments-header {
    font-size: 24px;
    font-weight: bold;
    color: #1f2937;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    gap: 12px;
}

/* Comment Card */
.comment-card {
    background: #f9fafb;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 16px;
    border-left: 4px solid #3b82f6;
    transition: all 0.3s ease;
}

.comment-card:hover {
    background: #f3f4f6;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* Comment Meta */
.comment-meta {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;
    font-size: 13px;
    color: #6b7280;
}

.comment-author {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: #374151;
}

/* Comment Content */
.comment-content {
    font-size: 15px;
    line-height: 1.6;
    color: #4b5563;
    margin-bottom: 12px;
}

/* Comment Actions */
.comment-actions {
    display: flex;
    gap: 12px;
}

.btn-edit-comment,
.btn-delete-comment {
    background: transparent;
    color: #6b7280;
    padding: 6px 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s ease;
}

.btn-edit-comment:hover {
    background: #eff6ff;
    color: #3b82f6;
    border-color: #3b82f6;
}

.btn-delete-comment:hover {
    background: #fef2f2;
    color: #ef4444;
    border-color: #ef4444;
}

/* Add Comment Button */
.btn-add-comment {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    padding: 14px 28px;
    border: none;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    margin-top: 24px;
}

.btn-add-comment:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
}

/* Language Selector for TTS */
.language-selector {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: white;
    padding: 8px 16px;
    border: 2px solid #e5e7eb;
    border-radius: 10px;
    font-size: 14px;
    cursor: pointer;
}

.language-selector select {
    border: none;
    background: transparent;
    font-size: 14px;
    font-weight: 600;
    color: #374151;
    cursor: pointer;
}
```

**Post Detail HTML Structure:**
```html
<div class="post-detail-container">
    <!-- Back Button -->
    <button class="btn-back" onclick="history.back()">
        ← Retour aux posts
    </button>
    
    <!-- Post Title -->
    <h1 class="post-detail-title">Les techniques de l'impressionnisme</h1>
    
    <!-- Post Meta -->
    <div class="post-detail-meta">
        <span class="post-detail-author">👤 Jean Dupont</span>
        <span class="post-detail-date">📅 09/05/2026 14:30</span>
    </div>
    
    <!-- Post Image (if exists) -->
    <img src="/images/posts/image.jpg" alt="Post image" class="post-detail-image">
    
    <!-- Post Content -->
    <div class="post-detail-content">
        <p>Bonjour à tous ! Je voudrais discuter des techniques...</p>
        <!-- Full content here -->
    </div>
    
    <!-- Engagement Section -->
    <div class="post-engagement">
        <!-- Stats -->
        <div class="post-detail-stats">
            <span class="stat-detail views">👁️ 245 vues</span>
            <span class="stat-detail likes">👍 32</span>
            <span class="stat-detail dislikes">👎 2</span>
        </div>
        
        <!-- Action Buttons -->
        <div class="post-detail-actions">
            <button class="btn-like" id="likeBtn">👍 J'aime</button>
            <button class="btn-dislike" id="dislikeBtn">👎 Je n'aime pas</button>
            
            <div class="language-selector">
                <select id="languageSelect">
                    <option value="fr">Français</option>
                    <option value="en">English</option>
                </select>
            </div>
            <button class="btn-speak">🔊 Écouter</button>
            
            <button class="btn-translate-detail">🌐 ترجمة</button>
            
            <!-- Only for author/admin -->
            <button class="btn-edit-post">✏️ Modifier</button>
            <button class="btn-delete-post">🗑️ Supprimer</button>
        </div>
    </div>
    
    <!-- Comments Section -->
    <div class="comments-section">
        <h2 class="comments-header">💬 Commentaires (12)</h2>
        
        <!-- Comment Card -->
        <div class="comment-card">
            <div class="comment-meta">
                <span class="comment-author">👤 Marie Martin</span>
                <span class="comment-date">📅 09/05/2026 15:45</span>
            </div>
            <p class="comment-content">
                Excellente analyse ! J'ajouterais que la technique 
                des touches rapides était également cruciale...
            </p>
            <div class="comment-actions">
                <button class="btn-edit-comment">✏️ Modifier</button>
                <button class="btn-delete-comment">🗑️ Supprimer</button>
            </div>
        </div>
        
        <!-- More comments... -->
        
        <!-- Add Comment Button -->
        <button class="btn-add-comment">💬 Ajouter un commentaire</button>
    </div>
</div>
```

---

### **RESPONSIVE DESIGN**

**Desktop (> 1024px):**
- Categories: 3-4 cards per row
- Posts: Full width cards with all information
- Sidebar with category navigation

**Tablet (768px - 1024px):**
- Categories: 2-3 cards per row
- Posts: Slightly narrower cards
- Compact action buttons

**Mobile (< 768px):**
- Categories: 1 card per row (full width)
- Posts: Stacked layout
- Hamburger menu for navigation
- Floating "New Post" button

```css
/* Responsive Grid */
@media (max-width: 768px) {
    .categories-grid {
        grid-template-columns: 1fr;
        padding: 20px;
    }
    
    .post-card {
        padding: 16px;
    }
    
    .post-actions {
        flex-direction: column;
        align-items: flex-start;
    }
}
```

---

### **LOADING STATES & ANIMATIONS**

**Category Cards Loading:**
```css
.category-card.loading {
    animation: pulse 1.5s infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}
```

**Posts Loading Skeleton:**
```html
<div class="post-card skeleton">
    <div class="skeleton-line" style="width: 60%;"></div>
    <div class="skeleton-line" style="width: 100%;"></div>
    <div class="skeleton-line" style="width: 80%;"></div>
</div>
```

---

### **Color Scheme**
- Primary: #3b82f6 (blue)
- Success: #10b981 (green)
- Error: #ef4444 (red)
- Warning: #f59e0b (orange)
- Gold: #d4af37 (for admin buttons)
- Gray: #6b7280 (for inactive elements)
- Background: #f9fafb (light gray)
- Card Background: #ffffff (white)

---

## 🔐 SECURITY & VALIDATION

### **Input Validation**
- **Title:** 3-100 characters, no HTML tags
- **Content:** 10-5000 characters, sanitize HTML
- **Comments:** 3-1000 characters, sanitize HTML
- **Image uploads:** Validate file type, max 5MB

### **Authentication**
- All write operations require authentication
- Check user session before allowing CRUD operations
- Store user ID in session (SessionManager)

### **Authorization**
- Users can only edit/delete their own posts and comments
- Admins can edit/delete any content
- Implement role-based access control (RBAC)

### **SQL Injection Prevention**
- Use prepared statements for all queries
- Never concatenate user input into SQL

### **XSS Prevention**
- Sanitize all user input before displaying
- Use Twig's auto-escaping in Symfony

---

## 📦 SYMFONY IMPLEMENTATION GUIDE

### **Entities to Create**
1. `Post` entity
2. `Comment` entity (or `Commentaire`)
3. `Category` entity
4. `Report` entity
5. `PostView` entity
6. `PostReaction` entity

### **Controllers to Create**
1. `ForumController` (public forum pages)
2. `PostController` (post CRUD)
3. `CommentController` (comment CRUD)
4. `CategoryController` (category CRUD)
5. `ReportController` (report handling)
6. `AdminForumController` (admin dashboard)

### **Services to Create**
1. `ModerationService` (AI moderation API client)
2. `TranslationService` (translation API client)
3. `PostService` (business logic for posts)
4. `ReactionService` (like/dislike logic)
5. `ViewTrackingService` (unique view tracking)

### **Routes to Define**
```yaml
# Public routes
forum_categories: /forum
forum_posts: /forum/category/{id}
forum_post_detail: /forum/post/{id}
forum_post_create: /forum/post/new
forum_post_edit: /forum/post/{id}/edit
forum_post_delete: /forum/post/{id}/delete
forum_comment_create: /forum/post/{id}/comment
forum_post_like: /forum/post/{id}/like
forum_post_dislike: /forum/post/{id}/dislike
forum_post_report: /forum/post/{id}/report
forum_post_translate: /forum/post/{id}/translate

# Admin routes
admin_forum_dashboard: /admin/forum
admin_forum_posts: /admin/forum/posts
admin_forum_comments: /admin/forum/comments
admin_forum_categories: /admin/forum/categories
admin_forum_reports: /admin/forum/reports
```

### **Forms to Create**
1. `PostType` (create/edit post)
2. `CommentType` (create/edit comment)
3. `CategoryType` (create/edit category)
4. `ReportType` (report post)

### **Twig Templates to Create**
1. `forum/categories.html.twig`
2. `forum/posts.html.twig`
3. `forum/post_detail.html.twig`
4. `forum/post_form.html.twig`
5. `forum/comment_form.html.twig`
6. `admin/forum/dashboard.html.twig`
7. `admin/forum/posts.html.twig`
8. `admin/forum/comments.html.twig`
9. `admin/forum/categories.html.twig`
10. `admin/forum/reports.html.twig`

---

## 🧪 TESTING CHECKLIST

### **Functional Tests**
- [ ] Create post with valid data
- [ ] Create post with invalid data (too short, too long)
- [ ] Create post with toxic content (should be rejected)
- [ ] Edit post
- [ ] Delete post
- [ ] Add comment
- [ ] Edit comment
- [ ] Delete comment
- [ ] Like post
- [ ] Unlike post
- [ ] Switch from like to dislike
- [ ] Report post
- [ ] Report same post twice (should fail)
- [ ] View post (should increment view count once)
- [ ] Translate post to Arabic
- [ ] Text-to-speech functionality
- [ ] Admin dashboard statistics
- [ ] Admin resolve report
- [ ] Admin delete reported post

### **Security Tests**
- [ ] Unauthorized user cannot edit others' posts
- [ ] Unauthorized user cannot delete others' posts
- [ ] SQL injection attempts are blocked
- [ ] XSS attempts are sanitized
- [ ] File upload validation works

---

## 📝 ADDITIONAL NOTES

### **Performance Optimization**
- Index frequently queried columns (category_id, author_id, date_creation)
- Cache category list
- Paginate posts and comments
- Lazy load images

### **Scalability Considerations**
- Use Redis for caching
- Implement queue system for AI moderation (async)
- Use CDN for uploaded images
- Implement rate limiting for API calls

### **Accessibility**
- Add ARIA labels to buttons
- Ensure keyboard navigation works
- Provide alt text for images
- Use semantic HTML

### **Internationalization**
- Support multiple languages (French, English, Arabic)
- Use Symfony translation component
- Store translations in YAML files

---

## 🎯 MIGRATION PRIORITY

1. **Phase 1: Core CRUD** (Posts, Comments, Categories)
2. **Phase 2: User Engagement** (Like/Dislike, Views)
3. **Phase 3: Moderation** (Reports, AI Moderation)
4. **Phase 4: Advanced Features** (Translation, Text-to-Speech)
5. **Phase 5: Admin Dashboard**
6. **Phase 6: UI/UX Polish**

---

## 📞 SUPPORT & DOCUMENTATION

For questions about this specification, refer to the original Java source code in:
- Controllers: `src/main/java/tn/esprit/museum/controllers/Forum*.java`
- Entities: `src/main/java/tn/esprit/museum/entities/`
- Services: `src/main/java/tn/esprit/museum/services/`

---

**END OF SPECIFICATION**
