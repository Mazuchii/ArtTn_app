from flask import Flask, jsonify, request
from flask_cors import CORS
import warnings
import pandas as pd
import numpy as np
import joblib
import os
from sqlalchemy import create_engine
import pymysql
import traceback

# Supprimer le warning de version scikit-learn (inoffensif pour des versions proches)
warnings.filterwarnings("ignore", category=UserWarning, module="sklearn")

app = Flask(__name__)
CORS(app)  # Permettre les requêtes depuis JavaFX

# ==================== CONFIGURATION ====================
MODEL_PATH = 'models/sales_prediction_model.pkl'
model = None

# Base de données
def get_db_connection():
    return create_engine('mysql+pymysql://root:@127.0.0.1/esprit_museum?charset=utf8mb4')

# ==================== CHARGEMENT DU MODÈLE ====================
def load_model():
    global model
    if os.path.exists(MODEL_PATH):
        try:
            model = joblib.load(MODEL_PATH)
            print("✅ Modèle de prédiction des ventes chargé avec succès")
            return True
        except Exception as e:
            print(f"❌ Erreur lors du chargement du modèle: {e}")
            return False
    else:
        print(f"❌ Modèle non trouvé: {MODEL_PATH}")
        return False

# ==================== RÉCUPÉRATION DES DONNÉES PRODUIT ====================
def get_product_features(product_id, month):
    """Récupère les caractéristiques du produit pour la prédiction"""
    try:
        conn = get_db_connection()

        # Récupérer les données du produit et son historique
        query = f"""
        SELECT
            p.id as produit_id,
            p.name as nom,
            p.price as prix,
            COUNT(DISTINCT o.id) as nombre_commandes,
            COALESCE(SUM(oi.quantity), 0) as ventes_historiques
        FROM products p
        LEFT JOIN order_items oi ON p.id = oi.product_id
        LEFT JOIN orders o ON oi.order_id = o.id AND o.status IN ('DELIVERED', 'CONFIRMED')
        WHERE p.id = {product_id}
        GROUP BY p.id
        """

        df = pd.read_sql(query, conn)
        conn.dispose()

        if df.empty:
            return None

        # Préparer les features
        month_sin = np.sin(2 * np.pi * month / 12)
        month_cos = np.cos(2 * np.pi * month / 12)

        prix = float(df['prix'].iloc[0]) if not pd.isna(df['prix'].iloc[0]) else 10
        prix_log = np.log1p(prix)

        nb_commandes = float(df['nombre_commandes'].iloc[0]) if not pd.isna(df['nombre_commandes'].iloc[0]) else 0

        return {
            'mois_sin': month_sin,
            'mois_cos': month_cos,
            'prix_log': prix_log,
            'nombre_commandes': nb_commandes
        }

    except Exception as e:
        print(f"❌ Erreur récupération features: {e}")
        traceback.print_exc()
        return None

# ==================== NOMS DES MOIS ====================
def get_month_name(month):
    months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
              'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']
    return months[month - 1] if 1 <= month <= 12 else 'Inconnu'

# ==================== ROUTES API ====================

@app.route('/health', methods=['GET'])
def health_check():
    """Vérifie l'état du service"""
    return jsonify({
        'status': 'healthy',
        'model_loaded': model is not None,
        'model_path': MODEL_PATH
    })

@app.route('/info', methods=['GET'])
def get_info():
    """Informations sur l'API"""
    return jsonify({
        'service': 'API Prédiction des ventes',
        'version': '1.0',
        'model_loaded': model is not None,
        'endpoints': [
            '/predict/<product_id>/<month>',
            '/predict-year/<product_id>',
            '/best-month/<product_id>',
            '/worst-month/<product_id>',
            '/all-products',
            '/health',
            '/info'
        ]
    })

@app.route('/predict/<int:product_id>/<int:month>', methods=['GET'])
def predict_sales(product_id, month):
    """Prédit les ventes pour un produit et un mois donné"""

    if model is None:
        if not load_model():
            return jsonify({'error': 'Modèle non disponible'}), 503

    if month < 1 or month > 12:
        return jsonify({'error': 'Mois invalide (1-12)'}), 400

    features = get_product_features(product_id, month)
    if features is None:
        return jsonify({'error': f'Produit #{product_id} non trouvé'}), 404

    try:
        # Créer le DataFrame pour la prédiction
        X = pd.DataFrame([features])

        # Prédiction
        prediction = float(model.predict(X)[0])

        # Calcul du pourcentage de confiance (basé sur la variance des arbres)
        predictions = [tree.predict(X)[0] for tree in model.estimators_]
        std = np.std(predictions)
        confidence = max(50, min(95, 100 - (std / prediction * 100))) if prediction > 0 else 70

        # Déterminer la tendance
        if month < 12:
            next_features = get_product_features(product_id, month + 1)
            if next_features:
                X_next = pd.DataFrame([next_features])
                next_pred = float(model.predict(X_next)[0])
                if next_pred > prediction * 1.05:
                    trend = "📈 Hausse"
                elif next_pred < prediction * 0.95:
                    trend = "📉 Baisse"
                else:
                    trend = "➡️ Stable"
            else:
                trend = "➡️ Stable"
        else:
            trend = "➡️ Stable"

        return jsonify({
            'success': True,
            'product_id': product_id,
            'month': month,
            'month_name': get_month_name(month),
            'predicted_sales': round(prediction, 2),
            'confidence': f"{confidence:.0f}%",
            'trend': trend,
            'model': 'RandomForestRegressor'
        })

    except Exception as e:
        print(f"❌ Erreur prédiction: {e}")
        traceback.print_exc()
        return jsonify({'error': f'Erreur lors de la prédiction: {str(e)}'}), 500

@app.route('/predict-year/<int:product_id>', methods=['GET'])
def predict_year(product_id):
    """Prédit les ventes pour tous les mois de l'année"""

    predictions = []
    for month in range(1, 13):
        features = get_product_features(product_id, month)
        if features:
            X = pd.DataFrame([features])
            pred = float(model.predict(X)[0])
            predictions.append({
                'month': month,
                'month_name': get_month_name(month),
                'predicted_sales': round(pred, 2)
            })
        else:
            predictions.append({
                'month': month,
                'month_name': get_month_name(month),
                'predicted_sales': 0,
                'error': 'Données non disponibles'
            })

    # Calculer le total annuel
    total_annual = sum(p['predicted_sales'] for p in predictions)
    average_monthly = total_annual / 12 if predictions else 0

    return jsonify({
        'product_id': product_id,
        'predictions': predictions,
        'total_annual': round(total_annual, 2),
        'average_monthly': round(average_monthly, 2)
    })

@app.route('/best-month/<int:product_id>', methods=['GET'])
def best_month(product_id):
    """Trouve le meilleur mois de vente pour un produit"""

    best = None
    best_value = -1

    for month in range(1, 13):
        features = get_product_features(product_id, month)
        if features:
            X = pd.DataFrame([features])
            pred = float(model.predict(X)[0])
            if pred > best_value:
                best_value = pred
                best = month

    if best is None:
        return jsonify({'error': 'Aucune prédiction disponible'}), 404

    return jsonify({
        'product_id': product_id,
        'best_month': best,
        'best_month_name': get_month_name(best),
        'predicted_sales': round(best_value, 2)
    })

@app.route('/worst-month/<int:product_id>', methods=['GET'])
def worst_month(product_id):
    """Trouve le pire mois de vente pour un produit"""

    worst = None
    worst_value = float('inf')

    for month in range(1, 13):
        features = get_product_features(product_id, month)
        if features:
            X = pd.DataFrame([features])
            pred = float(model.predict(X)[0])
            if pred < worst_value:
                worst_value = pred
                worst = month

    if worst is None:
        return jsonify({'error': 'Aucune prédiction disponible'}), 404

    return jsonify({
        'product_id': product_id,
        'worst_month': worst,
        'worst_month_name': get_month_name(worst),
        'predicted_sales': round(worst_value, 2)
    })

@app.route('/all-products', methods=['GET'])
def predict_all_products():
    """Prédit les ventes pour tous les produits (mois actuel)"""

    try:
        conn = get_db_connection()
        query = "SELECT id, name, price FROM products ORDER BY id"
        products = pd.read_sql(query, conn)
        conn.dispose()

        current_month = pd.Timestamp.now().month

        results = []
        for _, row in products.iterrows():
            features = get_product_features(row['id'], current_month)
            if features:
                X = pd.DataFrame([features])
                pred = float(model.predict(X)[0])
                results.append({
                    'product_id': row['id'],
                    'product_name': row['name'],
                    'current_month': current_month,
                    'predicted_sales': round(pred, 2),
                    'price': float(row['price'])
                })

        # Trier par ventes prédites décroissantes
        results.sort(key=lambda x: x['predicted_sales'], reverse=True)

        return jsonify({
            'month': current_month,
            'month_name': get_month_name(current_month),
            'products': results,
            'total_products': len(results)
        })

    except Exception as e:
        print(f"❌ Erreur: {e}")
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500

@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Route non trouvée'}), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({'error': 'Erreur interne du serveur'}), 500

# ==================== DÉMARRAGE ====================
if __name__ == '__main__':
    print("=" * 50)
    print("🚀 API Prédiction des Ventes")
    print("=" * 50)

    # Charger le modèle
    load_model()

    # Démarrer le serveur
    port = 5002
    print(f"\n✅ Serveur démarré sur http://localhost:{port}")
    print(f"📊 Health check: http://localhost:{port}/health")
    print(f"📖 Documentation: http://localhost:{port}/info")
    print("\nPoints d'accès disponibles:")
    print("  - GET /predict/<product_id>/<month>")
    print("  - GET /predict-year/<product_id>")
    print("  - GET /best-month/<product_id>")
    print("  - GET /worst-month/<product_id>")
    print("  - GET /all-products")
    print("=" * 50)

    app.run(host='0.0.0.0', port=port, debug=True)