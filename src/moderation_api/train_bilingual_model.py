import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import classification_report, confusion_matrix
import joblib
import os
import re

print("📊 Création des données d'entraînement enrichies...")

# ==================== TEXTES NORMAUX ====================
normal_texts = [
    # Français
    "Bonjour, j'ai une question sur l'exposition",
    "Merci pour ces informations très utiles",
    "Quel est le prix du billet d'entrée ?",
    "J'adore ce musée, les œuvres sont magnifiques",
    "Peut-on prendre des photos à l'intérieur ?",
    "Très belle exposition, je reviendrai",
    "Bravo pour l'organisation de cet événement",
    "Super site, très instructif et bien conçu",
    "Les horaires d'ouverture sont-ils disponibles en ligne ?",
    "La visite guidée était passionnante",
    "Je recommande ce musée à tous mes amis",
    "L'exposition temporaire est exceptionnelle",
    "Merci beaucoup pour votre aide précieuse",
    "C'est une très belle initiative",
    "Je suis ravi de cette découverte",
    "Félicitations pour ce beau projet",
    "Continuez comme ça, c'est excellent",
    "Très bonne ambiance dans ce musée",
    "Les guides sont très compétents",
    "La boutique du musée est très agréable",
    "Je reviendrai avec ma famille",
    "Un grand merci à toute l'équipe",
    "Exposition à ne pas manquer",
    "C'est un lieu magnifique",
    "Très bonne expérience, je recommande",
    "Les œuvres sont magnifiquement présentées",
    "J'ai passé un excellent moment",
    "Le personnel est très accueillant",
    "La collection est impressionnante",
    "Un endroit magique à découvrir",

    # Anglais
    "Hello, I have a question about the exhibition",
    "Thank you for this very useful information",
    "How much is the entrance ticket?",
    "I love this museum, the artworks are beautiful",
    "Can we take photos inside?",
    "Very nice exhibition, I will come back",
    "Great job organizing this event",
    "Great site, very informative",
    "Thank you so much for your help",
    "This is a wonderful place to visit",
    "I really enjoyed my time here",
    "The staff is very friendly",
    "Highly recommended for art lovers",
    "Beautiful collection, very well curated",
    "I will definitely come again",
    "Amazing experience, thank you",
    "The audio guide is very helpful",
    "Perfect for a family day out",
]

# ==================== TEXTES TOXIQUES ====================
toxic_texts = [
    # Français
    "Tu es stupide et incompetente",
    "C'est de la merde, vous êtes nul",
    "Ferme ta gueule, personne veut ton avis",
    "Tu mérites de crever, imbécile",
    "Je te hais, va te faire voir",
    "Quelle bande d'incapables",
    "Vous êtes tous des idiots",
    "Va te faire foutre",
    "T'es vraiment une merde humaine",
    "Je vais te casser la gueule",
    "Je te déteste",
    "Tu es vraiment con",
    "C'est de la grosse merde",
    "T'es qu'un connard fini",
    "Va crever sale con",
    "Fils de pute va",
    "Bande de sales cons",
    "Vous êtes pathétiques",
    "C'est nul à chier",
    "Dégage d'ici sale merde",

    # Anglais
    "You are stupid and incompetent",
    "This is fucking garbage, you suck",
    "Shut the fuck up, nobody wants your opinion",
    "You deserve to die, idiot",
    "I hate you, go fuck yourself",
    "What a bunch of losers",
    "You are all idiots",
    "Go to hell you piece of shit",
    "You're a total asshole",
    "Fuck off, nobody cares",
    "You are a bitch",
    "You are a fucking idiot",
    "What the fuck is wrong with you",
    "You are so stupid and dumb",
    "Bitch please, you know nothing",
    "You suck",
    "Fuck you",
    "Kill yourself",
    "You are worthless",
    "Eat shit",
]

# ==================== TEXTES SPAM ====================
spam_texts = [
    # Français
    "Gagnez 1000€ par jour en cliquant ici http://spam.com",
    "Achetez des followers Instagram sur mon site",
    "Médicament miracle, Viagra pas cher",
    "Coffret cadeau exceptionnel -50% code SPAM",
    "Cliquez ici pour gagner un iPhone gratuit",
    "Investissez dans le Bitcoin, rendement garanti",
    "Abonnez-vous à ma chaîne YouTube",
    "Perdez 10kg en 1 semaine sans effort",
    "Prêt bancaire immédiat sans justificatif",
    "Votre colis est bloqué, cliquez ici",
    "Gagnez de l'argent facilement depuis chez vous",
    "Formation gratuite pour devenir riche",
    "Code promo -70% valable aujourd'hui seulement",
    "URGENT: votre compte va être fermé",
    "Vous avez gagné un lot de 1000€",
    "Medicament sans ordonnance, livraison discrète",
    "Crypto monnaie: investissez maintenant",
    "Vendez vos produits sur notre marketplace",
    "azeroaùzeorgjùqisjh",
    "ùjksfhp",
    "qkdfhoipqkjdfhipqdh",
    "qbfhipq",
    "phdkqhjqùohjùoqeirjhoùejrùhijb!ldwjhj",
    "azeiajt$azij$taoijgoaẑijg",
    "aeôhôejq^hod^fokhqd^hknq^dh",
    "azekra$zpekt$iarezjgi$je$rphijqofh",

    # Anglais
    "Earn 1000$ per day by clicking here http://spam.com",
    "Buy Instagram followers on my site",
    "Miracle medicine, cheap Viagra",
    "Special gift box -50% code SPAM",
    "Click here to win a free iPhone",
    "Invest in Bitcoin, guaranteed returns",
    "Subscribe to my YouTube channel",
    "Lose weight fast with this miracle pill",
    "Your package is blocked, click here",
    "Congratulations! You won a free prize",
    "Limited time offer - 80% off",
    "Make money fast from home",
    "Urgent: Your account will be closed",
    "Free Bitcoin giveaway",
    "Crypto investment opportunity",
    "Get rich quick scheme",
    "Viagra pills cheap",
    "Pharmacy online no prescription",

    # Spam avec caractères spéciaux / répétitions
    "***CLIQUE ICI*** pour gagner 10000€",
    "!!!! PROMO EXTRAORDINAIRE !!!! -80%",
    "❤️🔥❤️🔥 OFFRE SPÉCIALE ❤️🔥❤️🔥",
    "✅✅✅ GAGNEZ UN IPHONE ✅✅✅",
    "💰💰💰 INVESTISSEZ MAINTENANT 💰💰💰",
    ">>> CLIQUEZ <<< pour votre cadeau",
    "********** OFFRE LIMITEE **********",
    "!!! URGENT !!! VOTRE COMPTE EST BLOQUE !!!",
    "azeiajt$azij$taoijgoaẑijg",
    "aeôhôejq^hod^fokhqd^hknq^dh",
    "azekra$zpekt$iarezjgi$je$rphijqofh",
    "jkhgfd qsdfgh jklm",
    "ù*$^ù*$^ù*$^",
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "qsdfghjklmwxcvbn",
]

# ==================== CONSTRUCTION DU DATASET ====================
texts = normal_texts + toxic_texts + spam_texts
labels = [0] * len(normal_texts) + [1] * len(toxic_texts) + [2] * len(spam_texts)

print(f"\n📊 Statistiques du dataset:")
print(f"   - Normaux (0): {len(normal_texts)} exemples")
print(f"   - Toxiques (1): {len(toxic_texts)} exemples")
print(f"   - Spam (2): {len(spam_texts)} exemples")
print(f"   - Total: {len(texts)} exemples")

df = pd.DataFrame({'text': texts, 'label': labels})

# ==================== ENTRAÎNEMENT ====================
print("\n🔄 Entraînement du modèle...")

pipeline = Pipeline([
    ('tfidf', TfidfVectorizer(
        max_features=15000,
        ngram_range=(1, 4),
        analyzer='char_wb',
        sublinear_tf=True
    )),
    ('classifier', LogisticRegression(
        C=5.0,
        max_iter=2000,
        class_weight='balanced',
        solver='lbfgs'
    ))
])

X_train, X_test, y_train, y_test = train_test_split(
    df['text'], df['label'],
    test_size=0.2,
    random_state=42,
    stratify=df['label']
)

pipeline.fit(X_train, y_train)

# ==================== SAUVEGARDE ====================
os.makedirs('model', exist_ok=True)
joblib.dump(pipeline, 'model/bilingual_moderation_model.pkl')
print(f"\n✅ Modèle bilingue entraîné et sauvegardé")
print(f"📈 Accuracy: {pipeline.score(X_test, y_test):.2f}")

# ==================== RAPPORT DÉTAILLÉ ====================
y_pred = pipeline.predict(X_test)
print("\n📋 Rapport de classification:")
print(classification_report(y_test, y_pred, target_names=["normal", "toxique", "spam"]))

# ==================== TESTS FINAUX ====================
print("\n🧪 Tests du modèle bilingue enrichi:")

test_cases = [
    # Normaux
    ("Merci pour votre aide", 0),
    ("Bonjour tout le monde", 0),
    ("I love this museum", 0),
    ("Très belle exposition", 0),

    # Toxiques
    ("You are a bitch", 1),
    ("What the fuck", 1),
    ("You are stupid and dumb", 1),
    ("Je te déteste", 1),
    ("Fuck you asshole", 1),
    ("Tu es vraiment con", 1),

    # Spam
    ("Gagnez de l'argent facilement", 2),
    ("Click here to win a free iPhone", 2),
    ("CLIQUE ICI pour gagner 10000€", 2),
    ("!!!! PROMO EXTRAORDINAIRE !!!!", 2),
    ("💰💰💰 INVESTISSEZ MAINTENANT 💰💰💰", 2),
    ("Buy Instagram followers", 2),
    ("Limited time offer", 2),
]

label_names = {0: "✅ normal", 1: "⚠️ toxique", 2: "📢 spam"}

for text, expected in test_cases:
    pred = pipeline.predict([text])[0]
    proba = max(pipeline.predict_proba([text])[0])
    status = "✓" if pred == expected else "✗"
    print(f"   {status} {label_names[pred]} (confiance: {proba:.2f}) - '{text[:50]}'")