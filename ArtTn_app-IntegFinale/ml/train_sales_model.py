import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error
import joblib
import os
from sqlalchemy import create_engine

# ⭐⭐⭐ BASE DE DONNÉES CORRIGÉE ⭐⭐⭐
def get_db_connection():
    return create_engine('mysql+pymysql://root:@127.0.0.1/esprit_museum?charset=utf8mb4')

def collect_sales_data():
    conn = get_db_connection()
    # ⭐⭐⭐ REQUÊTE CORRIGÉE pour esprit_museum ⭐⭐⭐
    query = """
    SELECT
        p.id as produit_id,
        p.name as nom,
        p.price as prix,
        MONTH(o.order_date) as mois,
        YEAR(o.order_date) as annee,
        CAST(SUM(oi.quantity) AS DECIMAL(10,2)) as ventes_reelles,
        COUNT(DISTINCT o.id) as nombre_commandes
    FROM products p
    LEFT JOIN order_items oi ON p.id = oi.product_id
    LEFT JOIN orders o ON oi.order_id = o.id
    WHERE o.status IN ('DELIVERED', 'CONFIRMED', 'SHIPPED')
    GROUP BY p.id, MONTH(o.order_date), YEAR(o.order_date)
    ORDER BY p.id, o.order_date
    """

    print("📊 Exécution de la requête SQL...")
    df = pd.read_sql(query, conn)
    conn.dispose()

    print(f"📈 {len(df)} lignes de données récupérées")
    if not df.empty:
        print("\n📋 Aperçu des données:")
        print(df.head())

    return df

def prepare_features(df):
    # Convertir mois en numérique
    df['mois'] = pd.to_numeric(df['mois'], errors='coerce')
    df['prix'] = pd.to_numeric(df['prix'], errors='coerce')
    df['nombre_commandes'] = pd.to_numeric(df['nombre_commandes'], errors='coerce')
    df['ventes_reelles'] = pd.to_numeric(df['ventes_reelles'], errors='coerce')

    # Features circulaires pour la saisonnalité
    df['mois_sin'] = np.sin(2 * np.pi * df['mois'] / 12)
    df['mois_cos'] = np.cos(2 * np.pi * df['mois'] / 12)

    # Transformation du prix
    df['prix_log'] = np.log1p(df['prix'])

    # Remplacer les valeurs manquantes
    df = df.fillna(0)

    features = ['mois_sin', 'mois_cos', 'prix_log', 'nombre_commandes']
    target = 'ventes_reelles'

    X = df[features].astype(float)
    y = pd.to_numeric(df[target], errors='coerce').fillna(0).astype(float)

    print(f"\n🔧 Features préparées: {X.shape[1]} variables")
    print(f"📊 {len(X)} échantillons pour l'entraînement")

    return X, y

def train_model():
    print("=" * 50)
    print("🚀 ENTRAÎNEMENT DU MODÈLE DE PRÉDICTION DES VENTES")
    print("=" * 50)

    df = collect_sales_data()

    if df.empty or len(df) < 5:
        print(f"\n⚠️  Données insuffisantes ({len(df)} ligne(s)). Génération de données synthétiques...")
        df = generate_synthetic_data()

    X, y = prepare_features(df)

    # Avec peu de données, pas de split — entraîner sur tout
    if len(X) < 10:
        print(f"\n⚠️  Jeu de données réduit ({len(X)} échantillons) — entraînement sans split test")
        model = RandomForestRegressor(n_estimators=50, random_state=42, n_jobs=-1)
        model.fit(X, y)
        print(f"✅ Modèle entraîné sur {len(X)} échantillons")
    else:
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        print(f"\n📚 Données d'entraînement: {len(X_train)} échantillons")
        print(f"🧪 Données de test: {len(X_test)} échantillons")
        print("\n🔄 Entraînement du Random Forest...")
        model = RandomForestRegressor(n_estimators=100, random_state=42, n_jobs=-1)
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        mse = mean_squared_error(y_test, y_pred)
        rmse = np.sqrt(mse)
        print(f"\n📊 Performance du modèle:")
        print(f"   MSE: {mse:.2f}")
        print(f"   RMSE: {rmse:.2f}")

    # Sauvegarder dans les deux emplacements possibles
    os.makedirs('models', exist_ok=True)
    os.makedirs('ml/models', exist_ok=True)
    joblib.dump(model, 'models/sales_prediction_model.pkl')
    joblib.dump(model, 'ml/models/sales_prediction_model.pkl')
    print(f"\n✅ Modèle sauvegardé : models/sales_prediction_model.pkl")
    print(f"✅ Modèle sauvegardé : ml/models/sales_prediction_model.pkl")

    feature_importance = pd.DataFrame({
        'feature': X.columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    print("\n📈 Importance des variables:")
    for _, row in feature_importance.iterrows():
        print(f"   {row['feature']}: {row['importance']:.3f}")


def generate_synthetic_data():
    """Génère des données synthétiques réalistes quand la DB est vide."""
    print("   Génération de 120 échantillons synthétiques (12 mois × 10 produits)...")
    rows = []
    for product_id in range(1, 11):
        prix = np.random.uniform(10, 200)
        for month in range(1, 13):
            # Saisonnalité : pics en décembre et juillet
            seasonal = 1 + 0.5 * np.sin(2 * np.pi * (month - 3) / 12)
            base_sales = max(1, int(np.random.poisson(5) * seasonal))
            rows.append({
                'produit_id': product_id,
                'nom': f'Produit {product_id}',
                'prix': prix,
                'mois': month,
                'annee': 2024,
                'ventes_reelles': float(base_sales),
                'nombre_commandes': max(1, base_sales // 2),
            })
    return pd.DataFrame(rows)

if __name__ == '__main__':
    train_model()