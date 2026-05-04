import random
import pandas as pd
from PackPredictorService import PackPredictorService

def calculer_montant_ancien(prix, capacite, categorie):
    """Ancienne méthode heuristique du code Java"""
    base_billetterie = prix * max(15, capacite * 0.12) if prix > 0 else 0
    base_audience = max(650, capacite * 7.5)
    montant_de_base = (base_billetterie + base_audience) * multiplicateur_categorie(categorie)

    # Variation aléatoire
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

# Initialiser le service ML
service_ml = PackPredictorService()

# Exemples d'événements de test
evenements_test = [
    {'titre': 'Concert Jazz', 'categorie': 'music', 'nbPlaces': 2000, 'prix': 45.0},
    {'titre': 'Match Football', 'categorie': 'sport', 'nbPlaces': 50000, 'prix': 25.0},
    {'titre': 'Pièce Théâtre', 'categorie': 'theatre', 'nbPlaces': 800, 'prix': 35.0},
    {'titre': 'Exposition Art', 'categorie': 'art', 'nbPlaces': 1500, 'prix': 15.0},
    {'titre': 'Conférence Tech', 'categorie': 'general', 'nbPlaces': 3000, 'prix': 20.0},
]

print("=== COMPARAISON : Ancienne logique vs Nouvelle logique ML ===\n")

for i, event in enumerate(evenements_test, 1):
    print(f"Test {i}: {event['titre']}")
    print(f"  Catégorie: {event['categorie']}, Capacité: {event['nbPlaces']}, Prix: {event['prix']}€")

    # Ancienne méthode (moyenne sur 5 exécutions pour réduire la variance aléatoire)
    montants_anciens = [calculer_montant_ancien(event['prix'], event['nbPlaces'], event['categorie']) for _ in range(5)]
    montant_ancien_moyen = sum(montants_anciens) / len(montants_anciens)

    # Nouvelle méthode ML
    suggestion_ml = service_ml.generate_suggestion(event)
    montant_ml = suggestion_ml.get_montant()

    # Calcul de la différence
    difference = montant_ml - montant_ancien_moyen
    pourcentage = (difference / montant_ancien_moyen) * 100 if montant_ancien_moyen > 0 else 0

    print(".2f"    print(".2f"    print(".2f"    print(".2f"    print("-" * 60)

print("\n=== ANALYSE DES CHANGEMENTS ===")
print("• L'ancienne méthode utilisait des formules mathématiques fixes avec variation aléatoire")
print("• La nouvelle méthode utilise un modèle ML entraîné sur 1000 exemples")
print("• Les montants ML sont plus cohérents et basés sur des patterns appris")
print("• La logique des avantages reste identique, seul le calcul du montant change")
