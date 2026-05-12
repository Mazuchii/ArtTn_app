"""
Script de ré-entraînement de tous les modèles ML.
À exécuter depuis le dossier ArtTn_app-IntegFinale :
    python ml/retrain_all.py
"""

import subprocess
import sys
import os

def check_sklearn_version():
    try:
        import sklearn
        print(f"✅ scikit-learn version installée : {sklearn.__version__}")
        return sklearn.__version__
    except ImportError:
        print("❌ scikit-learn non installé. Installez-le avec : pip install scikit-learn")
        sys.exit(1)

def retrain_fraud_model():
    print("\n" + "=" * 55)
    print("🔄 Ré-entraînement : Modèle de détection de fraude")
    print("=" * 55)

    # Vérifier que le dataset existe
    dataset_path = os.path.join(os.path.dirname(__file__), "datasets", "users_fraud_dataset.csv")
    if not os.path.exists(dataset_path):
        print(f"❌ Dataset introuvable : {dataset_path}")
        return False

    try:
        # Importer et exécuter directement (même processus Python)
        import importlib.util, sys as _sys

        train_path = os.path.join(os.path.dirname(__file__), "train_model.py")
        spec = importlib.util.spec_from_file_location("train_model", train_path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        mod.train_model()

        model_path = os.path.join(os.path.dirname(__file__), "models", "fraud_model.pkl")
        if os.path.exists(model_path):
            print(f"\n✅ fraud_model.pkl régénéré avec succès")
            return True
        else:
            print(f"\n❌ fraud_model.pkl non trouvé après entraînement")
            return False

    except Exception as e:
        print(f"❌ Erreur lors de l'entraînement du modèle fraude : {e}")
        import traceback
        traceback.print_exc()
        return False

def retrain_sales_model():
    print("\n" + "=" * 55)
    print("🔄 Ré-entraînement : Modèle de prédiction des ventes")
    print("=" * 55)

    try:
        import importlib.util

        train_path = os.path.join(os.path.dirname(__file__), "train_sales_model.py")
        spec = importlib.util.spec_from_file_location("train_sales_model", train_path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        mod.train_model()

        # Le script sales sauvegarde dans models/ (relatif au cwd)
        model_path = os.path.join("models", "sales_prediction_model.pkl")
        alt_path   = os.path.join(os.path.dirname(__file__), "models", "sales_prediction_model.pkl")

        if os.path.exists(model_path) or os.path.exists(alt_path):
            print(f"\n✅ sales_prediction_model.pkl régénéré avec succès")
            return True
        else:
            print(f"\n⚠️  sales_prediction_model.pkl non trouvé (données insuffisantes en DB ?)")
            return False

    except Exception as e:
        print(f"❌ Erreur lors de l'entraînement du modèle ventes : {e}")
        import traceback
        traceback.print_exc()
        return False

def verify_models():
    print("\n" + "=" * 55)
    print("🔍 Vérification des modèles régénérés")
    print("=" * 55)

    import pickle, sklearn

    models_to_check = [
        os.path.join(os.path.dirname(__file__), "models", "fraud_model.pkl"),
        os.path.join(os.path.dirname(__file__), "models", "sales_prediction_model.pkl"),
        os.path.join("models", "sales_prediction_model.pkl"),
    ]

    found = False
    for path in models_to_check:
        if os.path.exists(path):
            found = True
            try:
                with open(path, "rb") as f:
                    obj = pickle.load(f)
                print(f"✅ {os.path.basename(path)} — chargé sans avertissement")
            except Exception as e:
                print(f"❌ {os.path.basename(path)} — erreur : {e}")

    if not found:
        print("⚠️  Aucun modèle trouvé à vérifier")

if __name__ == "__main__":
    print("=" * 55)
    print("🤖 OUTIL DE RÉ-ENTRAÎNEMENT DES MODÈLES ML")
    print("=" * 55)

    version = check_sklearn_version()

    fraud_ok = retrain_fraud_model()
    sales_ok = retrain_sales_model()

    verify_models()

    print("\n" + "=" * 55)
    print("📋 RÉSUMÉ")
    print("=" * 55)
    print(f"  scikit-learn : {version}")
    print(f"  Modèle fraude  : {'✅ OK' if fraud_ok else '❌ Échec'}")
    print(f"  Modèle ventes  : {'✅ OK' if sales_ok else '⚠️  Ignoré (données insuffisantes)'}")
    print("\nLes modèles sont maintenant compatibles avec votre version de scikit-learn.")
    print("Relancez l'application Java pour utiliser les nouveaux modèles.")
