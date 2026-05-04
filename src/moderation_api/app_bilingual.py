from flask import Flask, request, jsonify
from flask_cors import CORS
import re

app = Flask(__name__)
CORS(app)

# ============================================================
# 1. LISTES
# ============================================================

TOXIC_WORDS = ['merde', 'con', 'connard', 'pute', 'salope', 'enculé',
               'stupide', 'imbécile', 'idiot', 'nul', 'fuck', 'shit',
               'asshole', 'bastard', 'crever', 'haine', 'bitch', 'fck',
               'déteste', 'hais', 'dégueulasse']

SPAM_WORDS = ['gagnez', 'argent', 'cliquez', 'abonnez', 'offre', 'promo',
              'gratuit', 'iphone', 'bitcoin', 'viagra', 'spam', 'earn',
              'click', 'subscribe', 'free', 'money', 'crypto', 'invest']

# ============================================================
# 2. RÈGLES DE REFUS STRICTES
# ============================================================

def is_toxic(text):
    return any(word in text.lower() for word in TOXIC_WORDS)

def is_too_much_uppercase(text):
    if len(text) < 5:
        return False
    upper = sum(1 for c in text if c.isupper())
    letters = sum(1 for c in text if c.isalpha())
    if letters == 0:
        return False
    return (upper / letters) > 0.5

def has_repeated_char(text):
    return bool(re.search(r'(.)\1{2,}', text))

def is_long_useless(text):
    """Longueur > 10 ET pas de sens (peu de voyelles ET peu de lettres)"""
    if len(text) <= 10:
        return False
    clean = re.sub(r'[^a-zA-Z]', '', text)
    if len(clean) < 8:
        return True
    vowels = sum(1 for c in clean.lower() if c in "aeiouy")
    return vowels < 3

def is_random_noise(text):
    specials = sum(1 for c in text if c in "$@#%*+=/|\\<>^")
    letters = sum(1 for c in text if c.isalpha())
    total = len(text)
    if total == 0:
        return False
    if letters / total < 0.4 and specials > 2:
        return True
    # Consonnes uniquement sur mot long
    clean = re.sub(r'[^a-z]', '', text.lower())
    if len(clean) > 10 and not any(c in "aeiouy" for c in clean):
        return True
    return False

def is_spam_commercial(text):
    count = sum(1 for w in SPAM_WORDS if w in text.lower())
    return count >= 2

# ============================================================
# 3. DÉCISION FINALE
# ============================================================

def moderate_text(text):
    # 1️⃣ TOXIQUE
    if is_toxic(text):
        return {"label": "toxique", "is_toxic": True, "is_spam": False}

    # 2️⃣ TROP DE MAJUSCULES
    if is_too_much_uppercase(text):
        return {"label": "spam", "is_toxic": False, "is_spam": True}

    # 3️⃣ RÉPÉTITION CARACTÈRE
    if has_repeated_char(text):
        return {"label": "spam", "is_toxic": False, "is_spam": True}

    # 4️⃣ LONG ET INUTILE
    if is_long_useless(text):
        return {"label": "spam", "is_toxic": False, "is_spam": True}

    # 5️⃣ BRUIT ALÉATOIRE
    if is_random_noise(text):
        return {"label": "spam", "is_toxic": False, "is_spam": True}

    # 6️⃣ SPAM COMMERCIAL
    if is_spam_commercial(text):
        return {"label": "spam", "is_toxic": False, "is_spam": True}

    # ✅ NORMAL
    return {"label": "normal", "is_toxic": False, "is_spam": False}

# ============================================================
# 4. ROUTES API
# ============================================================

@app.route('/moderate', methods=['POST'])
def moderate():
    data = request.get_json()
    text = data.get('text', '')
    return jsonify(moderate_text(text))

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok'})

if __name__ == '__main__':
    print("\n🚀 Serveur de modération ULTRA STRICT")
    print("✅ Toxique")
    print("✅ +50% majuscules")
    print("✅ 3+ répétitions caractères")
    print("✅ Longueur > 10 et inutile")
    print("✅ Bruit / symboles")
    print("✅ Spam commercial")
    print("\n👉 http://localhost:5001\n")
    app.run(host='0.0.0.0', port=5001, debug=True)