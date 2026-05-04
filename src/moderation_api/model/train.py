import random
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder, MinMaxScaler
from sklearn.ensemble import RandomForestRegressor
from sklearn.pipeline import Pipeline
import joblib

def calculer_montant(prix, capacite, categorie):
    base_billetterie = prix * max(15, capacite * 0.12) if prix > 0 else 0
    base_audience = max(650, capacite * 7.5)
    montant_de_base = (base_billetterie + base_audience) * multiplicateur_categorie(categorie)

    # Ajouter une variation aléatoire entre 0.7 (70%) et 1.5 (150%) pour avoir un prix différent à chaque fois
    variation = 0.7 + (1.5 - 0.7) * random.random()
    montant_aleatoire = montant_de_base * variation

    return round(max(200.0, montant_aleatoire) / 10.0) * 10.0

def multiplicateur_categorie(categorie):
    normalized = categorie.lower()
    if 'mus' in normalized:
        return 1.25
    if 'thea' in normalized or 'th' in normalized:
        return 1.15
    if 'sport' in normalized:
        return 1.30
    if 'art' in normalized or 'expo' in normalized:
        return 1.10
    return 1.00

# Générer des données d'entraînement synthétiques
categories = ['general', 'music', 'theatre', 'sport', 'art']
data = []
for _ in range(1000):
    categorie = random.choice(categories)
    capacite = random.randint(10, 10000)
    prix = random.uniform(0, 2000)
    montant = calculer_montant(prix, capacite, categorie)
    data.append({'categorie': categorie, 'capacite': capacite, 'prix': prix, 'montant': montant})

df = pd.DataFrame(data)
df.to_csv('training_data.csv', index=False)

# Préparation des données pour l'entraînement
X = df[['categorie', 'capacite', 'prix']]
y = df['montant']

# Définir le pipeline ML
preprocessor = ColumnTransformer(
    transformers=[
        ('cat', OneHotEncoder(), ['categorie']),
        ('num', MinMaxScaler(), ['capacite', 'prix'])
    ])

model = Pipeline([
    ('preprocessor', preprocessor),
    ('regressor', RandomForestRegressor(n_estimators=100, random_state=42))
])

# Entraîner le modèle
model.fit(X, y)

# Sauvegarder le modèle
joblib.dump(model, 'model.pkl')

print("Modèle entraîné et sauvegardé dans model.pkl")
print("Données d'entraînement sauvegardées dans training_data.csv")
