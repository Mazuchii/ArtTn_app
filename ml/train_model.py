import pandas as pd
import numpy as np
import pickle
import os
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.preprocessing import StandardScaler
from datetime import datetime
import random
import warnings
warnings.filterwarnings('ignore')

DATASET_PATH = 'ml/datasets/users_fraud_dataset.csv'
MODEL_PATH = 'ml/models/fraud_model.pkl'

DISPOSABLE_DOMAINS = ['10minutemail', 'yopmail', 'tempmail', 'mailinator',
                      'guerrilla', 'trashmail', 'throwaway', 'guerrillamail',
                      'temp-mail', 'fake-mail', 'spam', 'jetable']

def extract_features(df):
    """Extrait toutes les features du DataFrame"""

    df['email_disposable'] = df['email'].apply(
        lambda x: 1 if pd.notna(x) and any(d in str(x).lower() for d in DISPOSABLE_DOMAINS) else 0
    )

    df['email_suspicious_format'] = df['email'].apply(
        lambda x: 1 if pd.notna(x) and ('+' in str(x) or len(str(x).split('@')[0]) > 20) else 0
    )

    df['name_suspect'] = df['full_name'].apply(
        lambda x: 1 if pd.isna(x) or len(str(x)) <= 3 or str(x).lower() in ['test', 'user', 'admin', 'temp', 'fake'] else 0
    )

    df['username_suspect'] = df['username'].apply(
        lambda x: 1 if pd.isna(x) or len(str(x)) < 3 or str(x).lower().startswith(('user', 'test', 'admin', 'temp')) else 0
    )

    weak_passwords = ['123456', 'password', '123456789', '12345', 'qwerty', 'abc123', 'admin', 'welcome']
    df['password_weak'] = df['password'].apply(
        lambda x: 1 if pd.isna(x) or len(str(x)) < 6 or str(x).lower() in weak_passwords else 0
    )

    def get_account_age(days):
        if pd.isna(days):
            return 30
        try:
            created = pd.to_datetime(days)
            age = (datetime.now() - created).days
            return max(0, age)
        except:
            return 30

    df['account_age_days'] = df['created_at'].apply(get_account_age)
    df['account_recent'] = (df['account_age_days'] < 7).astype(int)
    df['account_today'] = (df['account_age_days'] < 1).astype(int)
    df['account_age_normalized'] = df['account_age_days'] / 365
    df['account_age_normalized'] = df['account_age_normalized'].clip(0, 1)

    df['no_profile_picture'] = (df['has_profile_picture'] == 0).astype(int)

    return df

def add_noise(df, noise_level=0.1):
    """Ajoute du bruit aléatoire pour éviter le surapprentissage"""
    df_noisy = df.copy()

    # Inverser aléatoirement 10% des labels
    for i in range(len(df_noisy)):
        if random.random() < noise_level:
            df_noisy.loc[i, 'is_fraud'] = 1 - df_noisy.loc[i, 'is_fraud']

    return df_noisy

def train_model():
    print("=" * 60)
    print("🤖 ENTRAÎNEMENT DU MODÈLE (AVEC ANTI-SURAPPRENTISSAGE)")
    print("=" * 60)

    df = pd.read_csv(DATASET_PATH)
    print(f"\n📊 Dataset original: {len(df)} utilisateurs")
    print(f"   - Légitimes: {len(df[df['is_fraud']==0])}")
    print(f"   - Fraudes: {len(df[df['is_fraud']==1])}")

    # Ajouter du bruit
    df = add_noise(df, noise_level=0.15)
    print(f"\n📊 Après ajout de bruit (15% de labels inversés):")
    print(f"   - Légitimes: {len(df[df['is_fraud']==0])}")
    print(f"   - Fraudes: {len(df[df['is_fraud']==1])}")

    df = extract_features(df)

    feature_cols = ['email_disposable', 'email_suspicious_format', 'name_suspect',
                    'username_suspect', 'password_weak', 'account_recent',
                    'account_today', 'no_profile_picture', 'account_age_normalized']

    X = df[feature_cols]
    y = df['is_fraud']

    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # Split
    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled, y, test_size=0.3, random_state=42, stratify=y
    )

    print(f"\n📊 Split:")
    print(f"   Train: {len(X_train)} (légitimes: {sum(y_train==0)}, fraudes: {sum(y_train==1)})")
    print(f"   Test: {len(X_test)} (légitimes: {sum(y_test==0)}, fraudes: {sum(y_test==1)})")

    # Modèle avec régularisation
    print("\n🧠 Entraînement...")
    model = RandomForestClassifier(
        n_estimators=50,        # Moins d'arbres
        max_depth=5,            # Profondeur limitée
        min_samples_split=10,   # Plus d'échantillons pour diviser
        min_samples_leaf=5,     # Plus d'échantillons par feuille
        class_weight='balanced',
        random_state=42
    )
    model.fit(X_train, y_train)

    # Validation croisée
    cv_scores = cross_val_score(model, X_scaled, y, cv=5)
    print(f"\n📊 Validation croisée (5 folds): {cv_scores.mean():.4f} (+/- {cv_scores.std():.4f})")

    # Évaluation
    y_train_pred = model.predict(X_train)
    y_test_pred = model.predict(X_test)

    train_acc = accuracy_score(y_train, y_train_pred)
    test_acc = accuracy_score(y_test, y_test_pred)

    print(f"\n📈 Accuracy sur l'entraînement: {train_acc:.4f}")
    print(f"📈 Accuracy sur le test: {test_acc:.4f}")
    print(f"📊 Écart entraînement/test: {train_acc - test_acc:.4f}")

    if train_acc - test_acc > 0.1:
        print("⚠️ ATTENTION: Surapprentissage détecté!")
    else:
        print("✅ Pas de surapprentissage")

    print("\n📋 Rapport de classification:")
    print(classification_report(y_test, y_test_pred, target_names=['LÉGITIME', 'FRAUDE'], zero_division=0))

    cm = confusion_matrix(y_test, y_test_pred)
    print("\n📋 Matrice de confusion:")
    print(f"               Prédit LÉGITIME   Prédit FRAUDE")
    print(f"Réel LÉGITIME      {cm[0,0]}              {cm[0,1]}")
    print(f"Réel FRAUDE        {cm[1,0]}              {cm[1,1]}")

    # Sauvegarder
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    with open(MODEL_PATH, 'wb') as f:
        pickle.dump({'model': model, 'scaler': scaler, 'features': feature_cols}, f)

    print(f"\n✅ Modèle sauvegardé: {MODEL_PATH}")

    # Test de prédiction
    print("\n" + "=" * 60)
    print("🧪 TEST DE PRÉDICTION")
    print("=" * 60)

    test_cases = [
        {"email": "test@10minutemail.com", "username": "testuser", "full_name": "Test User",
         "password": "123456", "created_at": "2024-03-10", "has_profile_picture": 0},
        {"email": "john@gmail.com", "username": "john", "full_name": "John Doe",
         "password": "StrongPass123", "created_at": "2023-01-01", "has_profile_picture": 1},
        {"email": "ambiguous@site.com", "username": "newuser", "full_name": "New User",
         "password": "pass123", "created_at": "2024-03-15", "has_profile_picture": 0},
    ]

    for i, test in enumerate(test_cases):
        test_df = pd.DataFrame([test])
        test_df = extract_features(test_df)
        X_test_single = scaler.transform(test_df[feature_cols])
        pred = model.predict(X_test_single)[0]
        proba = model.predict_proba(X_test_single)[0]
        print(f"\nTest {i+1}: {test['email']}")
        print(f"   Prédit: {'FRAUDE' if pred == 1 else 'LÉGITIME'}")
        print(f"   Probabilité FRAUDE: {proba[1]:.2%}")

if __name__ == "__main__":
    train_model()