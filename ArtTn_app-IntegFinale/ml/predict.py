import pickle
import sys
import warnings
import pandas as pd
import numpy as np

# Supprimer le warning de version scikit-learn (inoffensif pour des versions proches)
warnings.filterwarnings("ignore", category=UserWarning, module="sklearn")

from train_model import extract_features  # On réutilise la même logique !

MODEL_PATH = 'ml/models/fraud_model.pkl'

def predict_user_fraud(user_data):
    try:
        with open(MODEL_PATH, 'rb') as f:
            assets = pickle.load(f)

        df_input = pd.DataFrame([user_data])
        df_features = extract_features(df_input)

        X = df_features[assets['features']]
        X_scaled = assets['scaler'].transform(X)

        proba = assets['model'].predict_proba(X_scaled)[0][1]

        # Sortie standard pour être lue par Java
        print(f"PROBABILITY:{proba:.4f}")

    except Exception as e:
        print(f"ERROR:{str(e)}")

if __name__ == "__main__":
    # Format attendu : email username full_name password created_at has_photo
    if len(sys.argv) >= 7:
        data = {
            'email': sys.argv[1],
            'username': sys.argv[2],
            'full_name': sys.argv[3],
            'password': sys.argv[4],
            'created_at': sys.argv[5],
            'has_profile_picture': int(sys.argv[6])
        }
        predict_user_fraud(data)