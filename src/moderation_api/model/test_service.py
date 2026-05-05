from PackPredictorService import PackPredictorService

# Exemple d'événement
evenement = {
    'titre': 'Concert de Jazz',
    'categorie': 'music',
    'nbPlaces': 5000,
    'prix': 50.0,
    'lieu': 'Salle Pleyel',
    'dateEvenement': '2026-05-15'
}

service = PackPredictorService()
suggestion = service.generate_suggestion(evenement)

print("Nom du pack:", suggestion.get_nom())
print("Montant suggéré:", suggestion.get_montant())
print("Avantages:")
print(suggestion.get_avantages())
